package com.FMS.controllers;

import com.FMS.dto.VehicleMatchDto;
import com.FMS.response.ApiResponse;
import com.FMS.services.DispatchOptimizationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/dispatch")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DispatchOptimizationController {

    DispatchOptimizationService dispatchOptimizationService;

    @GetMapping("/suggest-vehicles")
    ApiResponse<List<VehicleMatchDto>> suggestVehicles(
            @RequestParam(required = false, defaultValue = "1.0") Double cargoWeightTon,
            @RequestParam(required = false) String startLocation) {
        return ApiResponse.<List<VehicleMatchDto>>builder()
                .result(dispatchOptimizationService.suggestVehiclesForTrip(cargoWeightTon, startLocation))
                .build();
    }
}
