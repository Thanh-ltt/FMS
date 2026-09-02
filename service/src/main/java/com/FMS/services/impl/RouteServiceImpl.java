package com.FMS.services.impl;

import com.FMS.dto.RouteDistanceDto;
import com.FMS.dto.request.RouteDistanceRequest;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.services.RouteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RouteServiceImpl implements RouteService {
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    static final String USER_AGENT = "FMS/1.0 route-distance";
    static final int MAX_ROUTE_CANDIDATES = 4;
    static final Pattern HOUSE_NUMBER_PATTERN = Pattern.compile("^\\s*(\\d+)[A-Za-z]?((?:[/-]\\d+[A-Za-z]?)*)(?=\\s|,|$)");
    static final Pattern BRANCH_NUMBER_PATTERN = Pattern.compile("\\d+");
    static final Pattern ADDRESS_SEPARATOR_PATTERN = Pattern.compile("[,;]+");
    static final Pattern ADMIN_KEYWORD_PATTERN = Pattern.compile("\\b(phuong|p\\.?|xa|x\\.?|thi tran|quan|q\\.?|huyen|thanh pho|tp\\.?|tinh)\\b");

    ObjectMapper objectMapper;
    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    private record Coordinate(double lat, double lon, String label, String query, String houseNumber) {
    }

    private record GeocodeCandidate(Coordinate coordinate, int score, double qualityPenaltyKm) {
    }

    private record RouteResult(double distanceKm, Double durationMinutes, int routeCount) {
    }

    private record DijkstraRoute(
            RouteResult route,
            Coordinate start,
            Coordinate end,
            double costKm,
            List<String> path
    ) {
    }

    private record DijkstraEdge(
            String from,
            String to,
            double costKm,
            RouteResult route,
            Coordinate start,
            Coordinate end,
            String label
    ) {
    }

    private record QueueNode(String id, double costKm) {
    }

    private record HouseNumber(int main, List<Integer> branches) {
    }

    private record AddressParts(
            String original,
            String houseNumberText,
            String streetDisplay,
            String streetNormalized,
            List<String> areaComponents
    ) {
    }

    @Override
    public RouteDistanceDto calculateDistance(RouteDistanceRequest request) {
        String startAddress = trimToEmpty(request.getStartLocation());
        String endAddress = trimToEmpty(request.getEndLocation());

        if (startAddress.isBlank() || endAddress.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ROUTE_ADDRESS);
        }

        Double sameStreetFallback = estimateSameStreetDistanceKm(startAddress, endAddress);
        boolean bothAddressesHaveHouseNumbers = extractHouseNumberParts(startAddress) != null
                && extractHouseNumberParts(endAddress) != null;
        if (sameStreetFallback != null && (sameStreetFallback == 0 || bothAddressesHaveHouseNumbers)) {
            return sameStreetDto(startAddress, endAddress, sameStreetFallback);
        }

        List<GeocodeCandidate> startCandidates = geocodeCandidatesSafely(startAddress);
        List<GeocodeCandidate> endCandidates = geocodeCandidatesSafely(endAddress);
        Coordinate start = bestCoordinate(startCandidates);
        Coordinate end = bestCoordinate(endCandidates);

        if (start == null || end == null) {
            return fallbackOrThrow(startAddress, endAddress, start, end);
        }

        try {
            DijkstraRoute route = dijkstraRoute(startCandidates, endCandidates);
            Double sameStreetDistanceKm = shouldUseSameStreetEstimate(
                    route.route(),
                    route.start(),
                    route.end(),
                    startAddress,
                    endAddress
            );

            if (sameStreetDistanceKm != null) {
                return sameStreetDto(startAddress, endAddress, sameStreetDistanceKm);
            }

            return dijkstraRouteDto(startAddress, endAddress, route, startCandidates.size(), endCandidates.size());
        } catch (IOException exception) {
            log.debug("Unable to calculate road route", exception);
            return fallbackOrThrow(startAddress, endAddress, start, end);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return fallbackOrThrow(startAddress, endAddress, start, end);
        }
    }

    private List<GeocodeCandidate> geocodeCandidatesSafely(String address) {
        try {
            return geocodeCandidates(address);
        } catch (IOException exception) {
            log.debug("Unable to geocode address: {}", address, exception);
            return List.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private Coordinate bestCoordinate(List<GeocodeCandidate> candidates) {
        return candidates.isEmpty() ? null : candidates.get(0).coordinate();
    }

    private RouteDistanceDto fallbackOrThrow(
            String startAddress,
            String endAddress,
            Coordinate start,
            Coordinate end
    ) {
        Double sameStreetDistanceKm = shouldUseSameStreetEstimate(null, start, end, startAddress, endAddress);

        if (sameStreetDistanceKm != null) {
            return sameStreetDto(startAddress, endAddress, sameStreetDistanceKm);
        }

        if (start != null && end != null) {
            return straightLineDto(startAddress, endAddress, start, end);
        }

        throw new AppException(ErrorCode.ROUTE_DISTANCE_UNAVAILABLE);
    }

    private List<GeocodeCandidate> geocodeCandidates(String address) throws IOException, InterruptedException {
        List<GeocodeCandidate> candidates = new ArrayList<>();
        Set<String> seenCoordinates = new LinkedHashSet<>();
        List<String> queries = buildGeocodeQueries(address);

        for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++) {
            String query = queries.get(queryIndex);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create("https://nominatim.openstreetmap.org/search"
                    + "?format=json&limit=3&countrycodes=vn&addressdetails=1&accept-language=vi&q="
                    + encodedQuery);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                continue;
            }

            JsonNode data = objectMapper.readTree(response.body());
            if (!data.isArray()) {
                continue;
            }

            for (int resultIndex = 0; resultIndex < data.size(); resultIndex++) {
                JsonNode item = data.get(resultIndex);
                if (!item.hasNonNull("lat") || !item.hasNonNull("lon")) {
                    continue;
                }

                double lat = item.path("lat").asDouble();
                double lon = item.path("lon").asDouble();
                String coordinateKey = String.format(Locale.ROOT, "%.6f,%.6f", lat, lon);
                if (!seenCoordinates.add(coordinateKey)) {
                    continue;
                }

                String displayName = item.path("display_name").asText(query);
                String street = normalizedStreetName(address);
                Coordinate candidate = new Coordinate(
                        lat,
                        lon,
                        displayName,
                        query,
                        item.path("address").path("house_number").asText("")
                );
                int score = scoreGeocodeCandidate(address, query, item, queryIndex, resultIndex);
                double housePenaltyKm = houseNumberPenaltyKm(address, item, displayName, street);
                candidates.add(new GeocodeCandidate(candidate, score, housePenaltyKm));
            }
        }

        if (!candidates.isEmpty()) {
            int bestScore = candidates.stream()
                    .mapToInt(GeocodeCandidate::score)
                    .max()
                    .orElse(0);

            return candidates.stream()
                    .map(candidate -> new GeocodeCandidate(
                            candidate.coordinate(),
                            candidate.score(),
                            candidate.qualityPenaltyKm() + Math.max(0, bestScore - candidate.score()) / 20D
                    ))
                    .sorted(Comparator
                            .comparingInt(GeocodeCandidate::score)
                            .reversed()
                            .thenComparingDouble(GeocodeCandidate::qualityPenaltyKm))
                    .limit(MAX_ROUTE_CANDIDATES)
                    .toList();
        }

        throw new IOException("GEOCODE_EMPTY");
    }

    private int scoreGeocodeCandidate(
            String address,
            String query,
            JsonNode item,
            int queryIndex,
            int resultIndex
    ) {
        String displayName = item.path("display_name").asText("");
        String label = normalize(displayName);
        AddressParts addressParts = parseAddressParts(address);
        String street = addressParts.streetNormalized();
        int score = Math.max(0, 30 - queryIndex * 3) + Math.max(0, 10 - resultIndex);

        if (!street.isBlank()) {
            score += label.contains(street) ? 100 : -100;
        }

        score += scoreHouseNumber(address, item, displayName, street);
        score += scoreAddressComponents(addressParts, label);

        return score;
    }

    private int scoreHouseNumber(String address, JsonNode item, String displayName, String street) {
        HouseNumber expected = extractHouseNumberParts(address);
        if (expected == null) {
            return 0;
        }

        String candidateHouseNumber = candidateHouseNumberText(item, displayName, street);
        HouseNumber candidate = extractHouseNumberParts(candidateHouseNumber);
        if (candidate == null) {
            return -15;
        }

        if (candidate.main() == expected.main()) {
            return candidate.branches().equals(expected.branches()) ? 90 : 60;
        }

        int gap = Math.abs(candidate.main() - expected.main());
        if (gap <= 10) {
            return 20 - gap * 3;
        }

        if (gap <= 50) {
            return -60 - gap;
        }

        return -140 - Math.min(gap, 160);
    }

    private double houseNumberPenaltyKm(String address, JsonNode item, String displayName, String street) {
        HouseNumber expected = extractHouseNumberParts(address);
        if (expected == null) {
            return 0;
        }

        String candidateHouseNumber = candidateHouseNumberText(item, displayName, street);
        HouseNumber candidate = extractHouseNumberParts(candidateHouseNumber);
        if (candidate == null) {
            return 1.5;
        }

        if (candidate.main() == expected.main()) {
            return candidate.branches().equals(expected.branches()) ? 0 : 0.8;
        }

        int gap = Math.abs(candidate.main() - expected.main());
        if (gap <= 10) {
            return 1.5;
        }

        if (gap <= 50) {
            return 4;
        }

        return 8;
    }

    private String candidateHouseNumberText(JsonNode item, String displayName, String street) {
        String addressHouseNumber = item.path("address").path("house_number").asText("");
        if (!normalizeHouseNumber(addressHouseNumber).isBlank()) {
            return addressHouseNumber;
        }

        String firstPartHouseNumber = houseNumberText(displayName);
        if (!normalizeHouseNumber(firstPartHouseNumber).isBlank()) {
            return firstPartHouseNumber;
        }

        if (street.isBlank()) {
            return "";
        }

        String label = normalize(displayName);
        Matcher matcher = Pattern.compile("([0-9]+(?:[/-][0-9]+)*)\\s*,?\\s*" + Pattern.quote(street))
                .matcher(label);
        return matcher.find() ? matcher.group(1) : "";
    }

    private int scoreAddressComponents(AddressParts addressParts, String label) {
        int score = 0;
        List<String> components = addressParts.areaComponents();

        for (int index = 0; index < components.size(); index++) {
            String component = components.get(index);
            String normalizedComponent = normalizeAddressComponent(component);
            if (normalizedComponent.isBlank()) {
                continue;
            }

            boolean matched = label.contains(normalize(component)) || label.contains(normalizedComponent);
            int weight = Math.max(20, 70 - index * 12);
            score += matched ? weight : -Math.max(10, weight / 2);
        }

        return score;
    }

    private DijkstraRoute dijkstraRoute(
            List<GeocodeCandidate> startCandidates,
            List<GeocodeCandidate> endCandidates
    ) throws IOException, InterruptedException {
        Map<String, List<DijkstraEdge>> graph = new HashMap<>();
        String source = "SOURCE";
        String target = "TARGET";

        for (int index = 0; index < startCandidates.size(); index++) {
            GeocodeCandidate candidate = startCandidates.get(index);
            addEdge(graph, new DijkstraEdge(
                    source,
                    "S" + index,
                    candidate.qualityPenaltyKm(),
                    null,
                    null,
                    null,
                    firstPresent(candidate.coordinate().label(), candidate.coordinate().query())
            ));
        }

        for (int index = 0; index < endCandidates.size(); index++) {
            GeocodeCandidate candidate = endCandidates.get(index);
            addEdge(graph, new DijkstraEdge(
                    "E" + index,
                    target,
                    candidate.qualityPenaltyKm(),
                    null,
                    null,
                    null,
                    firstPresent(candidate.coordinate().label(), candidate.coordinate().query())
            ));
        }

        boolean hasRouteEdge = false;
        for (int startIndex = 0; startIndex < startCandidates.size(); startIndex++) {
            GeocodeCandidate start = startCandidates.get(startIndex);

            for (int endIndex = 0; endIndex < endCandidates.size(); endIndex++) {
                GeocodeCandidate end = endCandidates.get(endIndex);

                try {
                    RouteResult route = routeDistance(start.coordinate(), end.coordinate());
                    addEdge(graph, new DijkstraEdge(
                            "S" + startIndex,
                            "E" + endIndex,
                            route.distanceKm(),
                            route,
                            start.coordinate(),
                            end.coordinate(),
                            "Tuyến đường bộ"
                    ));
                    hasRouteEdge = true;
                } catch (IOException exception) {
                    log.debug("Unable to route between geocode candidates", exception);
                }
            }
        }

        if (!hasRouteEdge) {
            throw new IOException("DIJKSTRA_ROUTE_EMPTY");
        }

        return runDijkstra(graph, source, target);
    }

    private void addEdge(Map<String, List<DijkstraEdge>> graph, DijkstraEdge edge) {
        graph.computeIfAbsent(edge.from(), key -> new ArrayList<>()).add(edge);
    }

    private DijkstraRoute runDijkstra(
            Map<String, List<DijkstraEdge>> graph,
            String source,
            String target
    ) throws IOException {
        Map<String, Double> distances = new HashMap<>();
        Map<String, DijkstraEdge> previous = new HashMap<>();
        PriorityQueue<QueueNode> queue = new PriorityQueue<>(Comparator.comparingDouble(QueueNode::costKm));

        distances.put(source, 0D);
        queue.add(new QueueNode(source, 0D));

        while (!queue.isEmpty()) {
            QueueNode current = queue.poll();
            if (current.costKm() > distances.getOrDefault(current.id(), Double.POSITIVE_INFINITY)) {
                continue;
            }

            if (target.equals(current.id())) {
                break;
            }

            for (DijkstraEdge edge : graph.getOrDefault(current.id(), List.of())) {
                double nextCost = current.costKm() + edge.costKm();
                if (nextCost < distances.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    distances.put(edge.to(), nextCost);
                    previous.put(edge.to(), edge);
                    queue.add(new QueueNode(edge.to(), nextCost));
                }
            }
        }

        if (!distances.containsKey(target)) {
            throw new IOException("DIJKSTRA_ROUTE_EMPTY");
        }

        DijkstraEdge roadEdge = null;
        List<String> path = new ArrayList<>();
        String cursor = target;
        while (previous.containsKey(cursor)) {
            DijkstraEdge edge = previous.get(cursor);
            if (!edge.label().isBlank()) {
                path.add(edge.label());
            }

            if (edge.route() != null) {
                roadEdge = edge;
            }

            cursor = edge.from();
        }

        if (roadEdge == null) {
            throw new IOException("DIJKSTRA_ROUTE_EMPTY");
        }

        Collections.reverse(path);
        return new DijkstraRoute(
                roadEdge.route(),
                roadEdge.start(),
                roadEdge.end(),
                distances.get(target),
                path
        );
    }

    private RouteResult routeDistance(Coordinate start, Coordinate end) throws IOException, InterruptedException {
        URI uri = URI.create(String.format(Locale.ROOT,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false&alternatives=true&steps=false",
                start.lon(),
                start.lat(),
                end.lon(),
                end.lat()));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("ROUTE_FAILED");
        }

        JsonNode routes = objectMapper.readTree(response.body()).path("routes");
        if (!routes.isArray() || routes.isEmpty()) {
            throw new IOException("ROUTE_EMPTY");
        }

        JsonNode shortestRoute = null;
        double shortestDistance = Double.POSITIVE_INFINITY;

        for (JsonNode route : routes) {
            double distance = route.path("distance").asDouble(Double.NaN);
            if (Double.isFinite(distance) && distance > 0 && distance < shortestDistance) {
                shortestDistance = distance;
                shortestRoute = route;
            }
        }

        if (shortestRoute == null) {
            throw new IOException("ROUTE_EMPTY");
        }

        Double durationMinutes = shortestRoute.hasNonNull("duration")
                ? shortestRoute.path("duration").asDouble() / 60
                : null;

        return new RouteResult(shortestDistance / 1000, durationMinutes, routes.size());
    }

    private RouteDistanceDto dijkstraRouteDto(
            String startAddress,
            String endAddress,
            DijkstraRoute dijkstraRoute,
            int startCandidateCount,
            int endCandidateCount
    ) {
        RouteResult route = dijkstraRoute.route();
        Coordinate start = dijkstraRoute.start();
        Coordinate end = dijkstraRoute.end();
        String routeSummary = route.durationMinutes() != null
                ? String.format(Locale.ROOT,
                "Dijkstra đã chọn tuyến có quãng đường %.1f km trong %d x %d ứng viên tọa độ, thời gian ước tính khoảng %d phút.",
                route.distanceKm(),
                startCandidateCount,
                endCandidateCount,
                Math.round(route.durationMinutes()))
                : String.format(Locale.ROOT,
                "Dijkstra đã chọn tuyến có quãng đường %.1f km trong %d x %d ứng viên tọa độ.",
                route.distanceKm(),
                startCandidateCount,
                endCandidateCount);
        List<String> details = new ArrayList<>(List.of(
                "Điểm đi: " + firstPresent(start.label(), start.query(), startAddress),
                "Điểm đến: " + firstPresent(end.label(), end.query(), endAddress),
                routeSummary
        ));

        if (!dijkstraRoute.path().isEmpty()) {
            details.add("Đường đi: " + String.join(" -> ", dijkstraRoute.path()));
        }

        if (dijkstraRoute.costKm() > route.distanceKm()) {
            details.add(String.format(Locale.ROOT,
                    "Chi phí %.1f km chỉ dùng để Dijkstra tránh tọa độ sai số nhà/khu vực, không cộng vào quãng đường hiển thị.",
                    dijkstraRoute.costKm()));
        }

        if (!geocodeHasHouseNumber(startAddress, start) || !geocodeHasHouseNumber(endAddress, end)) {
            details.add("Dữ liệu bản đồ chưa xác nhận đúng số nhà, quãng đường là ước tính theo điểm gần nhất trên tuyến.");
        }

        return RouteDistanceDto.builder()
                .distanceKm(route.distanceKm())
                .durationMinutes(route.durationMinutes())
                .routeCount(route.routeCount())
                .source("DIJKSTRA_OSM_ROUTE")
                .title("Tuyến ngắn nhất bằng Dijkstra")
                .detail(String.join(" | ", details))
                .startLabel(firstPresent(start.label(), start.query(), startAddress))
                .endLabel(firstPresent(end.label(), end.query(), endAddress))
                .build();
    }

    private RouteDistanceDto roadRouteDto(
            String startAddress,
            String endAddress,
            Coordinate start,
            Coordinate end,
            RouteResult route
    ) {
        String routeSummary = route.durationMinutes() != null
                ? String.format(Locale.ROOT,
                "Đã chọn tuyến ngắn nhất trong %d phương án, thời gian ước tính khoảng %d phút.",
                route.routeCount(),
                Math.round(route.durationMinutes()))
                : String.format(Locale.ROOT, "Đã chọn tuyến ngắn nhất trong %d phương án.", route.routeCount());
        List<String> details = new ArrayList<>(List.of(
                "Điểm đi: " + firstPresent(start.label(), start.query(), startAddress),
                "Điểm đến: " + firstPresent(end.label(), end.query(), endAddress),
                routeSummary
        ));

        if (!geocodeHasHouseNumber(startAddress, start) || !geocodeHasHouseNumber(endAddress, end)) {
            details.add("Dữ liệu bản đồ chưa xác nhận đúng số nhà, quãng đường là ước tính theo điểm gần nhất trên tuyến.");
        }

        return RouteDistanceDto.builder()
                .distanceKm(route.distanceKm())
                .durationMinutes(route.durationMinutes())
                .routeCount(route.routeCount())
                .source("ROAD_ROUTE")
                .title("Tuyến ngắn nhất theo đường bộ")
                .detail(String.join(" | ", details))
                .startLabel(firstPresent(start.label(), start.query(), startAddress))
                .endLabel(firstPresent(end.label(), end.query(), endAddress))
                .build();
    }

    private RouteDistanceDto sameStreetDto(String startAddress, String endAddress, double distanceKm) {
        String title = distanceKm == 0
                ? "Hai địa chỉ trùng nhau"
                : "Ước tính theo số nhà cùng tuyến đường";
        String detail = distanceKm == 0
                ? "Điểm đi và điểm đến giống nhau nên quãng đường được tính là 0 km."
                : String.join(" | ",
                "Tuyến: " + streetDisplayName(startAddress),
                "Dữ liệu bản đồ không trả về đúng tọa độ từng số nhà hoặc trả hai điểm quá gần nhau.",
                "Hệ thống ước tính theo chênh lệch số nhà/hẻm để tránh kết quả sai.");

        return RouteDistanceDto.builder()
                .distanceKm(distanceKm)
                .source("SAME_STREET_ESTIMATE")
                .title(title)
                .detail(detail)
                .startLabel(startAddress)
                .endLabel(endAddress)
                .build();
    }

    private RouteDistanceDto straightLineDto(String startAddress, String endAddress, Coordinate start, Coordinate end) {
        return RouteDistanceDto.builder()
                .distanceKm(estimateStraightLineKm(start, end))
                .source("STRAIGHT_LINE_ESTIMATE")
                .title("Ước tính theo khoảng cách địa lý")
                .detail("Không lấy được tuyến đường bộ chi tiết, hệ thống dùng khoảng cách đường thẳng có hệ số quy đổi.")
                .startLabel(firstPresent(start.label(), start.query(), startAddress))
                .endLabel(firstPresent(end.label(), end.query(), endAddress))
                .build();
    }

    private List<String> buildGeocodeQueries(String address) {
        String trimmed = trimToEmpty(address);
        AddressParts addressParts = parseAddressParts(trimmed);
        String houseStreet = joinAddressParts(List.of(addressParts.houseNumberText(), addressParts.streetDisplay()));
        String areaSuffix = joinAddressParts(addressParts.areaComponents());
        Set<String> queries = new LinkedHashSet<>();

        addQuery(queries, trimmed);
        if (!normalize(trimmed).contains("viet nam")) {
            addQuery(queries, trimmed + ", Việt Nam");
        }

        if (!houseStreet.isBlank() && !areaSuffix.isBlank()) {
            addQuery(queries, houseStreet + ", " + areaSuffix);
            addQuery(queries, houseStreet + ", " + areaSuffix + ", Việt Nam");
        }

        if (!addressParts.streetDisplay().isBlank() && !areaSuffix.isBlank()) {
            addQuery(queries, addressParts.streetDisplay() + ", " + areaSuffix);
            addQuery(queries, addressParts.streetDisplay() + ", " + areaSuffix + ", Việt Nam");
        }

        if (!houseStreet.isBlank()) {
            addQuery(queries, houseStreet);
            addQuery(queries, houseStreet + ", Việt Nam");
        }

        if (!addressParts.streetDisplay().isBlank()) {
            addQuery(queries, addressParts.streetDisplay());
            addQuery(queries, addressParts.streetDisplay() + ", Việt Nam");
        }

        return new ArrayList<>(queries);
    }

    private AddressParts parseAddressParts(String address) {
        String original = trimToEmpty(address);
        List<String> segments = ADDRESS_SEPARATOR_PATTERN.splitAsStream(original)
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toList();
        String firstSegment = segments.isEmpty() ? original : segments.get(0);
        String houseNumber = houseNumberText(firstSegment);
        String streetDisplay = stripAdministrativeTail(removeHouseNumber(firstSegment));
        List<String> areaComponents = new ArrayList<>();

        if (segments.size() > 1) {
            areaComponents.addAll(segments.subList(1, segments.size()));
        } else {
            areaComponents.addAll(extractInlineAdministrativeComponents(original));
        }

        String normalizedStreet = normalizeStreet(streetDisplay);
        return new AddressParts(
                original,
                houseNumber,
                streetDisplay,
                normalizedStreet,
                areaComponents.stream()
                        .map(String::trim)
                        .filter(component -> !component.isBlank())
                        .distinct()
                        .toList()
        );
    }

    private List<String> extractInlineAdministrativeComponents(String address) {
        String normalized = normalize(address);
        List<String> components = new ArrayList<>();
        addInlineAdministrativeComponent(components, normalized, "(phuong|p\\.?)\\s+(.+?)(?=\\s+(quan|q\\.?|huyen|thanh pho|tp\\.?|tinh)\\s+|$)");
        addInlineAdministrativeComponent(components, normalized, "(xa|x\\.?)\\s+(.+?)(?=\\s+(huyen|quan|thanh pho|tp\\.?|tinh)\\s+|$)");
        addInlineAdministrativeComponent(components, normalized, "(thi tran)\\s+(.+?)(?=\\s+(huyen|quan|thanh pho|tp\\.?|tinh)\\s+|$)");
        addInlineAdministrativeComponent(components, normalized, "(quan|q\\.?)\\s+(.+?)(?=\\s+(thanh pho|tp\\.?|tinh)\\s+|$)");
        addInlineAdministrativeComponent(components, normalized, "(huyen)\\s+(.+?)(?=\\s+(thanh pho|tp\\.?|tinh)\\s+|$)");
        addInlineAdministrativeComponent(components, normalized, "(thanh pho|tp\\.?)\\s+(.+?)(?=\\s+(viet nam)$|$)");
        addInlineAdministrativeComponent(components, normalized, "(tinh)\\s+(.+?)(?=\\s+(viet nam)$|$)");
        return components;
    }

    private void addInlineAdministrativeComponent(List<String> components, String normalizedAddress, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(normalizedAddress);
        while (matcher.find()) {
            String component = trimToEmpty(matcher.group());
            if (!component.isBlank()) {
                components.add(component);
            }
        }
    }

    private String stripAdministrativeTail(String value) {
        String normalized = normalize(value);
        Matcher matcher = ADMIN_KEYWORD_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return trimToEmpty(value);
        }

        String normalizedPrefix = normalized.substring(0, matcher.start()).trim();
        String original = trimToEmpty(value);
        return normalizedPrefix.isBlank()
                ? original
                : original.substring(0, Math.min(original.length(), normalizedPrefix.length())).trim();
    }

    private String normalizeAddressComponent(String value) {
        return normalize(value)
                .replaceAll("\\b(phuong|p\\.?|xa|x\\.?|thi tran|quan|q\\.?|huyen|thanh pho|tp\\.?|tinh)\\b\\s*", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeStreet(String value) {
        return normalize(value)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String joinAddressParts(List<String> parts) {
        return String.join(", ", parts.stream()
                .map(this::trimToEmpty)
                .filter(part -> !part.isBlank())
                .toList());
    }

    private void addQuery(Set<String> queries, String query) {
        String normalizedQuery = trimToEmpty(query);
        if (!normalizedQuery.isBlank()) {
            queries.add(normalizedQuery);
        }
    }

    private Double shouldUseSameStreetEstimate(
            RouteResult route,
            Coordinate start,
            Coordinate end,
            String startAddress,
            String endAddress
    ) {
        Double sameStreetDistanceKm = estimateSameStreetDistanceKm(startAddress, endAddress);

        if (sameStreetDistanceKm == null) {
            return null;
        }

        if (sameStreetDistanceKm == 0) {
            return 0D;
        }

        boolean coordinatesTooClose = start != null && end != null && estimateStraightLineKm(start, end) < 0.05;
        boolean routeTooSmall = route == null || !Double.isFinite(route.distanceKm()) || route.distanceKm() < 0.1;
        boolean houseNumberIgnored = !geocodeHasHouseNumber(startAddress, start)
                || !geocodeHasHouseNumber(endAddress, end);

        return routeTooSmall || coordinatesTooClose || houseNumberIgnored ? sameStreetDistanceKm : null;
    }

    private Double estimateSameStreetDistanceKm(String startAddress, String endAddress) {
        if (normalizedAddress(startAddress).equals(normalizedAddress(endAddress))) {
            return 0D;
        }

        String startStreet = normalizedStreetName(startAddress);
        String endStreet = normalizedStreetName(endAddress);
        if (startStreet.isBlank() || !startStreet.equals(endStreet)) {
            return null;
        }

        HouseNumber startNumber = extractHouseNumberParts(startAddress);
        HouseNumber endNumber = extractHouseNumberParts(endAddress);

        if (startNumber == null || endNumber == null) {
            return 0.3;
        }

        double mainGapKm = Math.abs(startNumber.main() - endNumber.main()) * 0.01;
        double branchGapKm = calculateBranchGap(startNumber.branches(), endNumber.branches());
        double oppositeSideAdjustmentKm = startNumber.main() % 2 != endNumber.main() % 2 ? 0.05 : 0;
        double estimatedKm = mainGapKm + branchGapKm + oppositeSideAdjustmentKm;

        return Math.min(Math.max(estimatedKm, 0.1), 5);
    }

    private double calculateBranchGap(List<Integer> startBranches, List<Integer> endBranches) {
        int maxLength = Math.max(startBranches.size(), endBranches.size());
        double totalGap = 0;

        for (int index = 0; index < maxLength; index++) {
            int startValue = index < startBranches.size() ? startBranches.get(index) : 0;
            int endValue = index < endBranches.size() ? endBranches.get(index) : 0;
            double weight = index == 0 ? 0.006 : 0.003;
            totalGap += Math.abs(startValue - endValue) * weight;
        }

        return totalGap;
    }

    private boolean geocodeHasHouseNumber(String address, Coordinate geocodeResult) {
        String expectedHouseNumber = normalizeHouseNumber(houseNumberText(address));
        if (expectedHouseNumber.isBlank()) {
            return true;
        }

        if (geocodeResult == null) {
            return false;
        }

        String resultHouseNumber = normalizeHouseNumber(geocodeResult.houseNumber());
        if (!resultHouseNumber.isBlank() && resultHouseNumber.equals(expectedHouseNumber)) {
            return true;
        }

        String labelHouseNumber = normalizeHouseNumber(houseNumberText(geocodeResult.label()));
        return labelHouseNumber.equals(expectedHouseNumber);
    }

    private double estimateStraightLineKm(Coordinate start, Coordinate end) {
        double earthRadiusKm = 6371;
        double dLat = toRadians(end.lat() - start.lat());
        double dLon = toRadians(end.lon() - start.lon());
        double lat1 = toRadians(start.lat());
        double lat2 = toRadians(end.lat());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double straightKm = earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return straightKm * 1.25;
    }

    private double toRadians(double value) {
        return value * Math.PI / 180;
    }

    private String normalizedAddress(String value) {
        return normalize(value).replaceAll("\\s+", " ").trim();
    }

    private String normalizedStreetName(String value) {
        return parseAddressParts(value).streetNormalized();
    }

    private String streetDisplayName(String value) {
        String street = parseAddressParts(value).streetDisplay();
        return street.isBlank() ? firstAddressPart(value) : street;
    }

    private String firstAddressPart(String value) {
        return trimToEmpty(value).split("[,;]", 2)[0].trim();
    }

    private String removeHouseNumber(String value) {
        return trimToEmpty(value)
                .replaceFirst("^\\s*\\d+[A-Za-z]?(?:[/-]\\d+[A-Za-z]?)*\\s*", "")
                .trim();
    }

    private HouseNumber extractHouseNumberParts(String value) {
        Matcher matcher = HOUSE_NUMBER_PATTERN.matcher(firstAddressPart(value));
        if (!matcher.find()) {
            return null;
        }

        List<Integer> branches = new ArrayList<>();
        Matcher branchMatcher = BRANCH_NUMBER_PATTERN.matcher(matcher.group(2));
        while (branchMatcher.find()) {
            branches.add(Integer.parseInt(branchMatcher.group()));
        }

        return new HouseNumber(Integer.parseInt(matcher.group(1)), branches);
    }

    private String houseNumberText(String value) {
        HouseNumber parts = extractHouseNumberParts(value);
        if (parts == null) {
            return "";
        }

        List<String> numbers = new ArrayList<>();
        numbers.add(String.valueOf(parts.main()));
        parts.branches().forEach(number -> numbers.add(String.valueOf(number)));
        return String.join("/", numbers);
    }

    private String normalizeHouseNumber(String value) {
        return trimToEmpty(value).replaceAll("[^0-9]", "");
    }

    private String normalize(String value) {
        return Normalizer.normalize(trimToEmpty(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (!trimToEmpty(value).isBlank()) {
                return value;
            }
        }
        return "";
    }
}
