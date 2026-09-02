package com.FMS.services;

import com.FMS.dto.VehicleDto;
import com.FMS.entity.Vehicle;
import com.FMS.enums.VehicleStatus;

import java.util.List;

public interface VehicleService {

    VehicleDto create(Vehicle request);

    VehicleDto update(String id, Vehicle request);

    void delete(String id);

    VehicleDto getById(String id);

    List<VehicleDto> getAll();

    List<VehicleDto> getByStatus(VehicleStatus status);

    List<VehicleDto> findByCapacityBetween(Double min, Double max);

    List<VehicleDto> searchByLicensePlate(String keyword);

    void updateStatus(String vehicleId, VehicleStatus status);

    List<VehicleDto> findAvailableVehicles();

    List<VehicleDto> findVehiclesInMaintenance();

    Long countAvailableVehicles();
}