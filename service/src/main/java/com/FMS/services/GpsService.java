package com.FMS.services;

import com.FMS.dto.VehicleLocationDto;
import com.FMS.dto.request.VehicleLocationRecordRequest;

import java.util.List;

public interface GpsService {
    VehicleLocationDto recordLocation(String vehicleId, VehicleLocationRecordRequest request);
    VehicleLocationDto getLatestLocation(String vehicleId);
    List<VehicleLocationDto> getTripHistory(String tripId);
    List<VehicleLocationDto> getVehicleHistory(String vehicleId);
    VehicleLocationDto simulateMovement(String vehicleId, String tripId);
}
