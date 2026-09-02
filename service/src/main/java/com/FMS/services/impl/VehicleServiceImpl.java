package com.FMS.services.impl;

import com.FMS.dto.VehicleDto;
import com.FMS.entity.Vehicle;
import com.FMS.enums.MaintenanceStatus;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.VehicleMapper;
import com.FMS.repositories.MaintenanceRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.VehicleRepository;
import com.FMS.services.VehicleService;
import com.FMS.validation.ValidationPatterns;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    VehicleRepository vehicleRepository;

    VehicleMapper vehicleMapper;

    TripRepository tripRepository;

    MaintenanceRepository maintenanceRepository;

    @Override
    public VehicleDto create(Vehicle request) {
        validateVehicleInput(request);
        String licensePlate = normalizeLicensePlate(request.getLicensePlate());

        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new AppException(ErrorCode.VEHICLE_ALREADY_EXISTS);
        }

        request.setLicensePlate(licensePlate);
        request.setVehicleType(request.getVehicleType().trim());

        if (request.getStatus() == null) {
            request.setStatus(VehicleStatus.AVAILABLE);
        }

        validateManualStatusChange(null, null, request.getStatus());

        return vehicleMapper.toDto(vehicleRepository.save(request));
    }

    @Override
    @Transactional
    public VehicleDto update(String id, Vehicle request) {

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        validateVehicleInput(request);
        String licensePlate = normalizeLicensePlate(request.getLicensePlate());
        if (vehicleRepository.existsByLicensePlateAndIdNot(licensePlate, id)) {
            throw new AppException(ErrorCode.VEHICLE_ALREADY_EXISTS);
        }

        VehicleStatus requestedStatus = request.getStatus() == null ? vehicle.getStatus() : request.getStatus();
        validateManualStatusChange(vehicle.getId(), vehicle.getStatus(), requestedStatus);

        vehicle.setLicensePlate(licensePlate);

        vehicle.setVehicleType(request.getVehicleType().trim());

        vehicle.setCapacity(request.getCapacity());

        vehicle.setStatus(requestedStatus);

        return vehicleMapper.toDto(vehicleRepository.save(vehicle));
    }

    @Override
    public VehicleDto getById(String id) {

        return vehicleMapper.toDto(vehicleRepository.findById(id).orElseThrow(() ->
                                new AppException(ErrorCode.VEHICLE_NOT_FOUND)));
    }

    @Override
    public List<VehicleDto> getAll() {

        return vehicleRepository.findAll()
                .stream()
                .map(vehicleMapper::toDto)
                .toList();
    }

    @Override
    public List<VehicleDto> getByStatus(VehicleStatus status) {

        return vehicleRepository.findByStatus(status)
                .stream()
                .map(vehicleMapper::toDto)
                .toList();
    }

    @Override
    public List<VehicleDto> findByCapacityBetween(Double min, Double max) {

        return vehicleRepository
                .findByCapacityBetween(min, max)
                .stream()
                .map(vehicleMapper::toDto)
                .toList();
    }

    @Override
    public List<VehicleDto> searchByLicensePlate(String keyword) {

        return vehicleRepository
                .findByLicensePlateContainingIgnoreCase(keyword)
                .stream()
                .map(vehicleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(String vehicleId, VehicleStatus status) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        validateManualStatusChange(vehicle.getId(), vehicle.getStatus(), status);
        vehicle.setStatus(status);

        vehicleRepository.save(vehicle);
    }

    @Override
    public void delete(String id) {

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        if (!tripRepository.findByVehicleId(id).isEmpty()
                || !maintenanceRepository.findByVehicleId(id).isEmpty()) {
            throw new AppException(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);
        }

        vehicleRepository.delete(vehicle);
    }
    @Override
    public List<VehicleDto> findAvailableVehicles() {

        return vehicleRepository
                .findByStatus(VehicleStatus.AVAILABLE)
                .stream()
                .map(vehicleMapper::toDto)
                .toList();
    }

    @Override
    public List<VehicleDto> findVehiclesInMaintenance() {

        return vehicleRepository
                .findByStatus(VehicleStatus.MAINTENANCE)
                .stream()
                .map(vehicleMapper::toDto)
                .toList();
    }

    @Override
    public Long countAvailableVehicles() {

        return vehicleRepository.countByStatus(VehicleStatus.AVAILABLE);
    }

    private void validateManualStatusChange(
            String vehicleId,
            VehicleStatus currentStatus,
            VehicleStatus requestedStatus) {
        if (requestedStatus == null || requestedStatus == currentStatus) {
            return;
        }

        if (requestedStatus == VehicleStatus.IN_TRIP
                || requestedStatus == VehicleStatus.MAINTENANCE) {
            throw new AppException(ErrorCode.VEHICLE_STATUS_MANAGED);
        }

        if (vehicleId == null) {
            return;
        }

        boolean hasActiveTrip = tripRepository.existsByVehicleIdAndStatusIn(
                vehicleId,
                List.of(TripStatus.IN_PROGRESS)
        );
        boolean hasActiveMaintenance = maintenanceRepository.findByVehicleId(vehicleId)
                .stream()
                .anyMatch(maintenance -> maintenance.getStatus() == MaintenanceStatus.IN_PROGRESS);

        if (hasActiveTrip || hasActiveMaintenance) {
            throw new AppException(ErrorCode.VEHICLE_STATUS_MANAGED);
        }
    }

    private String normalizeLicensePlate(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private void validateVehicleInput(Vehicle request) {
        if (request == null
                || request.getLicensePlate() == null
                || !request.getLicensePlate().trim().matches(ValidationPatterns.VEHICLE_LICENSE_PLATE)
                || request.getVehicleType() == null
                || request.getVehicleType().isBlank()
                || request.getCapacity() == null
                || request.getCapacity() <= 0) {
            throw new AppException(ErrorCode.INVALID_VEHICLE_INPUT);
        }
    }
}
