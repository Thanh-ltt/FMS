package com.FMS.services.impl;

import com.FMS.dto.FinancialReportDto;
import com.FMS.entity.Customer;
import com.FMS.entity.Expense;
import com.FMS.entity.Invoice;
import com.FMS.entity.Maintenance;
import com.FMS.entity.Trip;
import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.InvoiceStatus;
import com.FMS.enums.DepositStatus;
import com.FMS.enums.MaintenanceStatus;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.repositories.ExpenseRepository;
import com.FMS.repositories.DepositRepository;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.MaintenanceRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.FMS.dto.ReportAnalyticsDto;
import com.FMS.enums.VehicleStatus;
import com.FMS.repositories.VehicleRepository;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    private final TripRepository tripRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DepositRepository depositRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportAnalyticsDto getAnalyticsData(LocalDate fromDate, LocalDate toDate) {
        LocalDate start = fromDate != null ? fromDate : LocalDate.now().minusMonths(5).withDayOfMonth(1);
        LocalDate end = toDate != null ? toDate : LocalDate.now();

        FinancialReportDto summary = getFinancialReport(start, end, null, null);

        // Group monthly data points
        List<ReportAnalyticsDto.MonthlyFinancialPoint> points = summary.getMonthly() != null
                ? summary.getMonthly().stream().map(m -> ReportAnalyticsDto.MonthlyFinancialPoint.builder()
                        .month(m.getLabel())
                        .revenue(m.getRevenue())
                        .expense(m.getTripCost() + m.getMaintenanceCost())
                        .profit(m.getNetProfit())
                        .build()).toList()
                : List.of();

        // Vehicle status breakdown
        List<ReportAnalyticsDto.VehicleStatusCount> vehicleCounts = List.of(
                ReportAnalyticsDto.VehicleStatusCount.builder().status("AVAILABLE").label("Sẵn sàng").count(vehicleRepository.countByStatus(VehicleStatus.AVAILABLE)).build(),
                ReportAnalyticsDto.VehicleStatusCount.builder().status("IN_TRIP").label("Đang vận chuyển").count(vehicleRepository.countByStatus(VehicleStatus.IN_TRIP)).build(),
                ReportAnalyticsDto.VehicleStatusCount.builder().status("MAINTENANCE").label("Bảo dưỡng").count(vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE)).build(),
                ReportAnalyticsDto.VehicleStatusCount.builder().status("INACTIVE").label("Ngưng hoạt động").count(vehicleRepository.countByStatus(VehicleStatus.INACTIVE)).build()
        );

        FinancialReportDto.Totals totals = summary.getTotals() != null ? summary.getTotals() : new FinancialReportDto.Totals();

        return ReportAnalyticsDto.builder()
                .totalRevenue(totals.getRecognizedRevenue())
                .totalTripExpense(totals.getTripExpense())
                .totalMaintenanceExpense(totals.getMaintenanceExpense())
                .totalNetProfit(totals.getNetProfit())
                .monthlyPoints(points)
                .vehicleStatusCounts(vehicleCounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialReportDto getFinancialReport(
            LocalDate fromDate,
            LocalDate toDate,
            String customerId,
            TripStatus tripStatus
    ) {
        validatePeriod(fromDate, toDate);
        String normalizedCustomerId = normalizeFilter(customerId);

        List<Trip> filteredTrips = tripRepository.findAll().stream()
                .filter(trip -> matchesCustomer(trip, normalizedCustomerId))
                .filter(trip -> tripStatus == null || trip.getStatus() == tripStatus)
                .filter(trip -> matchesPeriod(parseTripDate(trip.getStartTime()), fromDate, toDate))
                .sorted(Comparator.comparing(
                        (Trip trip) -> parseTripDate(trip.getStartTime()),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        Set<String> tripIds = filteredTrips.stream()
                .map(Trip::getId)
                .collect(Collectors.toSet());

        Map<String, List<Invoice>> invoicesByTrip = invoiceRepository.findAll().stream()
                .filter(this::isActiveInvoice)
                .filter(invoice -> invoice.getTrip() != null && tripIds.contains(invoice.getTrip().getId()))
                .collect(Collectors.groupingBy(invoice -> invoice.getTrip().getId()));

        Map<String, List<Expense>> expensesByTrip = expenseRepository.findAll().stream()
                .filter(this::isApprovedExpense)
                .filter(expense -> expense.getTrip() != null && tripIds.contains(expense.getTrip().getId()))
                .collect(Collectors.groupingBy(expense -> expense.getTrip().getId()));

        boolean maintenanceIncluded = normalizedCustomerId == null && tripStatus == null;
        List<Maintenance> maintenances = maintenanceIncluded
                ? maintenanceRepository.findAll().stream()
                        .filter(maintenance -> maintenance.getStatus() == MaintenanceStatus.COMPLETED)
                        .filter(maintenance -> matchesPeriod(maintenanceAccountingDate(maintenance), fromDate, toDate))
                        .toList()
                : List.of();

        List<FinancialReportDto.TripRow> tripRows = filteredTrips.stream()
                .map(trip -> toTripRow(
                        trip,
                        invoicesByTrip.getOrDefault(trip.getId(), List.of()),
                        expensesByTrip.getOrDefault(trip.getId(), List.of())
                ))
                .toList();

        List<FinancialReportDto.CustomerRow> customerRows = buildCustomerRows(tripRows);
        List<FinancialReportDto.MonthlyRow> monthlyRows = buildMonthlyRows(
                filteredTrips,
                tripRows,
                maintenances
        );

        double revenue = tripRows.stream().mapToDouble(FinancialReportDto.TripRow::getRevenue).sum();
        double paidRevenue = tripRows.stream().mapToDouble(FinancialReportDto.TripRow::getPaidRevenue).sum();
        double depositApplied = tripRows.stream().mapToDouble(FinancialReportDto.TripRow::getDepositApplied).sum();
        double depositAvailable = depositRepository.findAll().stream()
                .filter(deposit -> deposit.getStatus() != DepositStatus.CANCELLED)
                .filter(deposit -> normalizedCustomerId == null
                        || (deposit.getCustomer() != null
                            && normalizedCustomerId.equals(deposit.getCustomer().getId())))
                .mapToDouble(deposit -> number(deposit.getAvailableAmount()))
                .sum();
        double outstanding = tripRows.stream().mapToDouble(FinancialReportDto.TripRow::getOutstanding).sum();
        double tripExpense = tripRows.stream().mapToDouble(FinancialReportDto.TripRow::getCost).sum();
        double maintenanceExpense = maintenances.stream().mapToDouble(this::maintenanceCost).sum();
        double totalExpense = tripExpense + maintenanceExpense;

        FinancialReportDto.Totals totals = FinancialReportDto.Totals.builder()
                .tripCount(tripRows.size())
                .completedTripCount(filteredTrips.stream().filter(trip -> trip.getStatus() == TripStatus.COMPLETED).count())
                .distanceKm(tripRows.stream().mapToDouble(FinancialReportDto.TripRow::getDistanceKm).sum())
                .recognizedRevenue(revenue)
                .paidRevenue(paidRevenue)
                .depositApplied(depositApplied)
                .depositAvailable(depositAvailable)
                .outstanding(outstanding)
                .tripExpense(tripExpense)
                .maintenanceExpense(maintenanceExpense)
                .totalExpense(totalExpense)
                .grossProfit(revenue - tripExpense)
                .netProfit(revenue - totalExpense)
                .build();

        return FinancialReportDto.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .customerId(normalizedCustomerId)
                .tripStatus(tripStatus)
                .maintenanceIncluded(maintenanceIncluded)
                .totals(totals)
                .trips(tripRows)
                .customers(customerRows)
                .monthly(monthlyRows)
                .build();
    }

    private FinancialReportDto.TripRow toTripRow(
            Trip trip,
            List<Invoice> invoices,
            List<Expense> expenses
    ) {
        double revenue = invoices.stream().mapToDouble(this::invoiceAmount).sum();
        double paidRevenue = invoices.stream().mapToDouble(this::paidAmount).sum();
        double depositApplied = invoices.stream().mapToDouble(this::depositAppliedAmount).sum();
        double outstanding = invoices.stream()
                .filter(this::isOutstandingInvoice)
                .mapToDouble(invoice -> number(invoice.getAmountDue()))
                .sum();
        double cost = expenses.stream().mapToDouble(this::expenseAmount).sum();

        return FinancialReportDto.TripRow.builder()
                .id(trip.getId())
                .code(shortId(trip.getId()))
                .status(trip.getStatus())
                .customerId(trip.getCustomer() != null ? trip.getCustomer().getId() : null)
                .customerName(customerName(trip.getCustomer()))
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : "-")
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .route(routeName(trip))
                .distanceKm(number(trip.getDistanceKm()))
                .cargoWeightTon(number(trip.getCargoWeightTon()))
                .freightAmount(number(trip.getFreightAmount()))
                .revenue(revenue)
                .paidRevenue(paidRevenue)
                .depositApplied(depositApplied)
                .outstanding(outstanding)
                .cost(cost)
                .profit(revenue - cost)
                .build();
    }

    private List<FinancialReportDto.CustomerRow> buildCustomerRows(
            List<FinancialReportDto.TripRow> tripRows
    ) {
        Map<String, CustomerAccumulator> grouped = new LinkedHashMap<>();

        for (FinancialReportDto.TripRow row : tripRows) {
            String key = row.getCustomerId() != null ? row.getCustomerId() : "UNLINKED";
            grouped.computeIfAbsent(key, ignored -> new CustomerAccumulator(
                    row.getCustomerId(),
                    row.getCustomerName()
            )).add(row);
        }

        return grouped.values().stream()
                .map(CustomerAccumulator::toDto)
                .sorted(Comparator.comparingDouble(FinancialReportDto.CustomerRow::getRevenue).reversed())
                .toList();
    }

    private List<FinancialReportDto.MonthlyRow> buildMonthlyRows(
            List<Trip> trips,
            List<FinancialReportDto.TripRow> tripRows,
            List<Maintenance> maintenances
    ) {
        Map<String, FinancialReportDto.TripRow> rowsById = tripRows.stream()
                .collect(Collectors.toMap(FinancialReportDto.TripRow::getId, Function.identity()));
        Map<YearMonth, MonthlyAccumulator> grouped = new HashMap<>();

        for (Trip trip : trips) {
            LocalDate tripDate = parseTripDate(trip.getStartTime());
            FinancialReportDto.TripRow row = rowsById.get(trip.getId());
            if (tripDate == null || row == null) {
                continue;
            }
            grouped.computeIfAbsent(YearMonth.from(tripDate), MonthlyAccumulator::new).addTrip(row);
        }

        for (Maintenance maintenance : maintenances) {
            LocalDate accountingDate = maintenanceAccountingDate(maintenance);
            if (accountingDate == null) {
                continue;
            }
            YearMonth month = YearMonth.from(accountingDate);
            grouped.computeIfAbsent(month, MonthlyAccumulator::new)
                    .addMaintenance(maintenanceCost(maintenance));
        }

        return grouped.values().stream()
                .sorted(Comparator.comparing(MonthlyAccumulator::month))
                .map(MonthlyAccumulator::toDto)
                .toList();
    }

    private void validatePeriod(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_REPORT_PERIOD);
        }
    }

    private boolean matchesCustomer(Trip trip, String customerId) {
        return customerId == null
                || (trip.getCustomer() != null && customerId.equals(trip.getCustomer().getId()));
    }

    private boolean matchesPeriod(LocalDate date, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        if (date == null) {
            return false;
        }
        return (fromDate == null || !date.isBefore(fromDate))
                && (toDate == null || !date.isAfter(toDate));
    }

    private LocalDate parseTripDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private boolean isActiveInvoice(Invoice invoice) {
        return invoice.getStatus() != InvoiceStatus.CANCELLED;
    }

    private boolean isOutstandingInvoice(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.PENDING
                || invoice.getStatus() == InvoiceStatus.OVERDUE;
    }

    private double invoiceAmount(Invoice invoice) {
        return number(invoice.getTotalAmount());
    }

    private double paidAmount(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return invoiceAmount(invoice);
        }
        return number(invoice.getPaidAmount());
    }

    private double depositAppliedAmount(Invoice invoice) {
        return number(invoice.getDepositAppliedAmount());
    }

    private double expenseAmount(Expense expense) {
        return number(expense.getAmount());
    }

    private boolean isApprovedExpense(Expense expense) {
        return expense.getStatus() == null || expense.getStatus() == ExpenseStatus.APPROVED;
    }

    private double maintenanceCost(Maintenance maintenance) {
        return number(maintenance.getCost());
    }

    private LocalDate maintenanceAccountingDate(Maintenance maintenance) {
        return maintenance.getCompletedAt() == null
                ? maintenance.getMaintenanceDate()
                : maintenance.getCompletedAt().toLocalDate();
    }

    private double number(Double value) {
        return value == null ? 0 : value;
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "-";
        }
        return id.substring(0, Math.min(8, id.length())).toUpperCase();
    }

    private String customerName(Customer customer) {
        if (customer == null) {
            return "Chưa liên kết";
        }
        if (customer.getName() != null && !customer.getName().isBlank()) {
            return customer.getName();
        }
        if (customer.getUser() != null && customer.getUser().getUsername() != null) {
            return customer.getUser().getUsername();
        }
        return shortId(customer.getId());
    }

    private String routeName(Trip trip) {
        String start = trip.getStartLocation() == null ? "" : trip.getStartLocation().trim();
        String end = trip.getEndLocation() == null ? "" : trip.getEndLocation().trim();
        if (start.isEmpty() && end.isEmpty()) {
            return "-";
        }
        if (start.isEmpty()) {
            return end;
        }
        if (end.isEmpty()) {
            return start;
        }
        return start + " -> " + end;
    }

    private static final class CustomerAccumulator {
        private final String customerId;
        private final String customerName;
        private long tripCount;
        private long completedTripCount;
        private double distanceKm;
        private double revenue;
        private double paidRevenue;
        private double depositApplied;
        private double outstanding;
        private double tripCost;

        private CustomerAccumulator(String customerId, String customerName) {
            this.customerId = customerId;
            this.customerName = customerName;
        }

        private void add(FinancialReportDto.TripRow row) {
            tripCount++;
            if (row.getStatus() == TripStatus.COMPLETED) {
                completedTripCount++;
            }
            distanceKm += row.getDistanceKm();
            revenue += row.getRevenue();
            paidRevenue += row.getPaidRevenue();
            depositApplied += row.getDepositApplied();
            outstanding += row.getOutstanding();
            tripCost += row.getCost();
        }

        private FinancialReportDto.CustomerRow toDto() {
            return FinancialReportDto.CustomerRow.builder()
                    .customerId(customerId)
                    .customerName(customerName)
                    .tripCount(tripCount)
                    .completedTripCount(completedTripCount)
                    .distanceKm(distanceKm)
                    .revenue(revenue)
                    .paidRevenue(paidRevenue)
                    .depositApplied(depositApplied)
                    .outstanding(outstanding)
                    .tripCost(tripCost)
                    .profit(revenue - tripCost)
                    .build();
        }
    }

    private static final class MonthlyAccumulator {
        private final YearMonth month;
        private double revenue;
        private double paidRevenue;
        private double depositApplied;
        private double outstanding;
        private double tripCost;
        private double maintenanceCost;

        private MonthlyAccumulator(YearMonth month) {
            this.month = month;
        }

        private YearMonth month() {
            return month;
        }

        private void addTrip(FinancialReportDto.TripRow row) {
            revenue += row.getRevenue();
            paidRevenue += row.getPaidRevenue();
            depositApplied += row.getDepositApplied();
            outstanding += row.getOutstanding();
            tripCost += row.getCost();
        }

        private void addMaintenance(double value) {
            maintenanceCost += value;
        }

        private FinancialReportDto.MonthlyRow toDto() {
            return FinancialReportDto.MonthlyRow.builder()
                    .period(month.toString())
                    .label(month.format(MONTH_LABEL_FORMATTER))
                    .revenue(revenue)
                    .paidRevenue(paidRevenue)
                    .depositApplied(depositApplied)
                    .outstanding(outstanding)
                    .tripCost(tripCost)
                    .maintenanceCost(maintenanceCost)
                    .netProfit(revenue - tripCost - maintenanceCost)
                    .build();
        }
    }
}
