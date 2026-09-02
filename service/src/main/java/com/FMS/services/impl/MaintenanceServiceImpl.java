package com.FMS.services.impl;

import com.FMS.dto.MaintenanceDto;
import com.FMS.dto.request.MaintenanceCreationRequest;
import com.FMS.dto.request.MaintenanceUpdateRequest;
import com.FMS.entity.Maintenance;
import com.FMS.entity.Trip;
import com.FMS.entity.Vehicle;
import com.FMS.enums.MaintenanceStatus;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.MaintenanceMapper;
import com.FMS.repositories.MaintenanceRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.VehicleRepository;
import com.FMS.services.MaintenanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class MaintenanceServiceImpl implements MaintenanceService {
    static final List<TripStatus> RESERVED_TRIP_STATUSES = List.of(
            TripStatus.CREATED,
            TripStatus.ASSIGNED,
            TripStatus.IN_PROGRESS
    );

    MaintenanceRepository maintenanceRepository;

    VehicleRepository vehicleRepository;

    TripRepository tripRepository;

    MaintenanceMapper maintenanceMapper;

    @Override
    @Transactional
    public MaintenanceDto createMaintenance(MaintenanceCreationRequest request) {

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow(() ->
                new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        validateMaintenanceCost(request.getCost());
        validateMaintenanceDates(request.getMaintenanceDate(), request.getNextMaintenanceDate());
        ensureMaintenanceCanBeScheduled(vehicle, null, request.getMaintenanceDate());
        List<String> maintenanceTypes = resolveMaintenanceTypes(
                request.getMaintenanceTypes(),
                request.getMaintenanceType()
        );

        Maintenance maintenance = Maintenance.builder()
                .vehicle(vehicle)
                .maintenanceType(maintenanceTypes.getFirst())
                .maintenanceTypes(maintenanceTypes)
                .description(normalizeDescription(request.getDescription()))
                .cost(request.getCost() == null ? 0 : request.getCost())
                .maintenanceDate(request.getMaintenanceDate())
                .nextMaintenanceDate(request.getNextMaintenanceDate())
                .status(MaintenanceStatus.PENDING)
                .build();

        return maintenanceMapper.toDto(maintenanceRepository.save(maintenance));
    }

    @Override
    @Transactional
    public MaintenanceDto updateMaintenance(String id, MaintenanceUpdateRequest request) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id).orElseThrow(() ->
                                new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        if (maintenance.getStatus() != MaintenanceStatus.PENDING
                && maintenance.getStatus() != MaintenanceStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_MAINTENANCE_STATUS);
        }
        validateMaintenanceCost(request.getCost());
        if (maintenance.getStatus() == MaintenanceStatus.PENDING) {
            validateMaintenanceDates(request.getMaintenanceDate(), request.getNextMaintenanceDate());
            ensureMaintenanceCanBeScheduled(
                    maintenance.getVehicle(),
                    maintenance.getId(),
                    request.getMaintenanceDate()
            );
        } else if (request.getNextMaintenanceDate() != null
                && (maintenance.getMaintenanceDate() == null
                || !request.getNextMaintenanceDate().isAfter(maintenance.getMaintenanceDate()))) {
            throw new AppException(ErrorCode.INVALID_MAINTENANCE_DATE);
        }

        List<String> maintenanceTypes = resolveMaintenanceTypes(
                request.getMaintenanceTypes(),
                request.getMaintenanceType()
        );
        maintenance.setMaintenanceType(maintenanceTypes.getFirst());
        maintenance.setMaintenanceTypes(maintenanceTypes);
        maintenance.setDescription(normalizeDescription(request.getDescription()));
        maintenance.setCost(request.getCost() == null ? 0D : request.getCost());
        if (maintenance.getStatus() == MaintenanceStatus.PENDING) {
            maintenance.setMaintenanceDate(request.getMaintenanceDate());
        }
        maintenance.setNextMaintenanceDate(request.getNextMaintenanceDate());

        return maintenanceMapper.toDto(maintenanceRepository.save(maintenance));
    }

    @Override
    @Transactional
    public void startMaintenance(String id) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id).orElseThrow(() ->
                                new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        if (maintenance.getStatus() != MaintenanceStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_MAINTENANCE_STATUS);
        }

        if (maintenance.getMaintenanceDate() != null
                && LocalDate.now().isBefore(maintenance.getMaintenanceDate())) {
            throw new AppException(ErrorCode.MAINTENANCE_NOT_DUE);
        }

        Vehicle maintenanceVehicle = maintenance.getVehicle();

        if (maintenanceVehicle == null) {
            throw new AppException(ErrorCode.VEHICLE_NOT_FOUND);
        }

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(maintenanceVehicle.getId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        ensureVehicleCanEnterMaintenance(vehicle, maintenance.getId(), maintenance.getMaintenanceDate());

        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenance.setStartedAt(LocalDateTime.now());
        maintenance.setCompletedAt(null);
        vehicle.setStatus(VehicleStatus.MAINTENANCE);

        vehicleRepository.save(vehicle);

        maintenanceRepository.save(maintenance);
    }

    @Override
    @Transactional
    public void completeMaintenance(String id) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id).orElseThrow(() ->
                                new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        if (maintenance.getStatus() != MaintenanceStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_MAINTENANCE_STATUS);
        }

        maintenance.setStatus(MaintenanceStatus.COMPLETED);
        maintenance.setCompletedAt(LocalDateTime.now());

        Vehicle maintenanceVehicle = maintenance.getVehicle();

        if (maintenanceVehicle == null) {
            throw new AppException(ErrorCode.VEHICLE_NOT_FOUND);
        }

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(maintenanceVehicle.getId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        if (!hasOtherActiveMaintenance(vehicle.getId(), maintenance.getId())) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }

        vehicleRepository.save(vehicle);

        maintenanceRepository.save(maintenance);
    }

    @Override
    @Transactional
    public void cancelMaintenance(String id) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id).orElseThrow(() ->
                                new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED
                || maintenance.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_MAINTENANCE_STATUS);
        }

        Vehicle maintenanceVehicle = maintenance.getVehicle();
        Vehicle vehicle = maintenanceVehicle == null
                ? null
                : vehicleRepository.findByIdForUpdate(maintenanceVehicle.getId()).orElse(null);

        if (maintenance.getStatus() == MaintenanceStatus.IN_PROGRESS
                && vehicle != null
                && !hasOtherActiveMaintenance(vehicle.getId(), maintenance.getId())) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        maintenance.setStatus(MaintenanceStatus.CANCELLED);

        maintenanceRepository.save(maintenance);
    }

    @Override
    @Transactional
    public void deleteMaintenance(String id) {
        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id).orElseThrow(() ->
                new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));
        if (maintenance.getStatus() != MaintenanceStatus.PENDING
                && maintenance.getStatus() != MaintenanceStatus.CANCELLED) {
            throw new AppException(ErrorCode.MAINTENANCE_CANNOT_DELETE);
        }

        maintenanceRepository.delete(maintenance);
    }

    @Override
    public List<MaintenanceDto> getAllMaintenances() {

        return maintenanceRepository.findAll()
                .stream()
                .map(maintenanceMapper::toDto)
                .toList();
    }

    @Override
    public List<MaintenanceDto> getByVehicle(String vehicleId) {

        return maintenanceRepository
                .findByVehicleId(vehicleId)
                .stream()
                .map(maintenanceMapper::toDto)
                .toList();
    }
    @Override
    public Double calculateMaintenanceCost(String vehicleId) {

        return maintenanceRepository.calculateMaintenanceCost(vehicleId, MaintenanceStatus.COMPLETED);
    }

    private boolean conflictsWithMaintenance(Trip trip, LocalDate maintenanceDate) {
        if (!RESERVED_TRIP_STATUSES.contains(trip.getStatus())) {
            return false;
        }

        if (maintenanceDate == null) {
            return true;
        }

        LocalDateTime tripStart = parseTripTime(trip.getStartTime());
        LocalDateTime tripEnd = parseTripTime(trip.getEndTime());
        if (tripStart == null && tripEnd == null) {
            return true;
        }

        LocalDate startDate = tripStart == null ? tripEnd.toLocalDate() : tripStart.toLocalDate();
        LocalDate endDate = tripEnd == null ? startDate : tripEnd.toLocalDate();
        return !maintenanceDate.isBefore(startDate) && !maintenanceDate.isAfter(endDate);
    }

    private void ensureMaintenanceCanBeScheduled(
            Vehicle vehicle,
            String currentMaintenanceId,
            LocalDate maintenanceDate
    ) {
        if (vehicle == null || vehicle.getStatus() == VehicleStatus.INACTIVE) {
            throw new AppException(ErrorCode.VEHICLE_NOT_AVAILABLE_FOR_MAINTENANCE);
        }
        boolean hasOtherOpenMaintenance = maintenanceRepository.findByVehicleId(vehicle.getId())
                .stream()
                .anyMatch(item -> (item.getStatus() == MaintenanceStatus.PENDING
                        || item.getStatus() == MaintenanceStatus.IN_PROGRESS)
                        && (currentMaintenanceId == null || !currentMaintenanceId.equals(item.getId())));
        boolean hasTripConflict = tripRepository.findByVehicleId(vehicle.getId())
                .stream()
                .anyMatch(trip -> conflictsWithMaintenance(trip, maintenanceDate));
        if (hasOtherOpenMaintenance || hasTripConflict) {
            throw new AppException(ErrorCode.VEHICLE_NOT_AVAILABLE_FOR_MAINTENANCE);
        }
    }

    private void ensureVehicleCanEnterMaintenance(
            Vehicle vehicle,
            String currentMaintenanceId,
            LocalDate maintenanceDate) {
        boolean hasOtherActiveMaintenance = hasOtherActiveMaintenance(vehicle.getId(), currentMaintenanceId);
        boolean hasActiveTrip = tripRepository.findByVehicleId(vehicle.getId())
                .stream()
                .anyMatch(trip -> conflictsWithMaintenance(trip, maintenanceDate));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE || hasOtherActiveMaintenance || hasActiveTrip) {
            throw new AppException(ErrorCode.VEHICLE_NOT_AVAILABLE_FOR_MAINTENANCE);
        }
    }

    private boolean hasOtherActiveMaintenance(String vehicleId, String currentMaintenanceId) {
        return maintenanceRepository.findByVehicleId(vehicleId)
                .stream()
                .anyMatch(item -> item.getStatus() == MaintenanceStatus.IN_PROGRESS
                        && (currentMaintenanceId == null || !currentMaintenanceId.equals(item.getId())));
    }

    private LocalDateTime parseTripTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private void validateMaintenanceDates(LocalDate maintenanceDate, LocalDate nextMaintenanceDate) {
        if (maintenanceDate == null
                || maintenanceDate.isBefore(LocalDate.now())
                || (nextMaintenanceDate != null && !nextMaintenanceDate.isAfter(maintenanceDate))) {
            throw new AppException(ErrorCode.INVALID_MAINTENANCE_DATE);
        }
    }

    private List<String> resolveMaintenanceTypes(List<String> values, String legacyValue) {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(resolved::add);
        }
        if (resolved.isEmpty() && legacyValue != null && !legacyValue.isBlank()) {
            resolved.add(legacyValue.trim());
        }
        if (resolved.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return List.copyOf(resolved);
    }

    private void validateMaintenanceCost(Double cost) {
        if (cost != null && cost < 0) {
            throw new AppException(ErrorCode.INVALID_MAINTENANCE_INPUT);
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
