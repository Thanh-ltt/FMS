package com.FMS.services;

import com.FMS.dto.VehicleMatchDto;

import java.util.List;

public interface DispatchOptimizationService {
    List<VehicleMatchDto> suggestVehiclesForTrip(Double cargoWeightTon, String startLocation);
}
