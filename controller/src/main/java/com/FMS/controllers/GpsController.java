package com.FMS.controllers;

import com.FMS.dto.VehicleLocationDto;
import com.FMS.dto.request.VehicleLocationRecordRequest;
import com.FMS.response.ApiResponse;
import com.FMS.services.GpsService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/gps")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GpsController {

    GpsService gpsService;

    @PostMapping("/vehicles/{vehicleId}/location")
    ApiResponse<VehicleLocationDto> recordLocation(
            @PathVariable String vehicleId,
            @RequestBody @Valid VehicleLocationRecordRequest request) {
        return ApiResponse.<VehicleLocationDto>builder()
                .result(gpsService.recordLocation(vehicleId, request))
                .build();
    }

    @GetMapping("/vehicles/{vehicleId}/latest")
    ApiResponse<VehicleLocationDto> getLatestLocation(@PathVariable String vehicleId) {
        return ApiResponse.<VehicleLocationDto>builder()
                .result(gpsService.getLatestLocation(vehicleId))
                .build();
    }

    @GetMapping("/vehicles/{vehicleId}/history")
    ApiResponse<List<VehicleLocationDto>> getVehicleHistory(@PathVariable String vehicleId) {
        return ApiResponse.<List<VehicleLocationDto>>builder()
                .result(gpsService.getVehicleHistory(vehicleId))
                .build();
    }

    @GetMapping("/trips/{tripId}/history")
    ApiResponse<List<VehicleLocationDto>> getTripHistory(@PathVariable String tripId) {
        return ApiResponse.<List<VehicleLocationDto>>builder()
                .result(gpsService.getTripHistory(tripId))
                .build();
    }

    @PostMapping("/vehicles/{vehicleId}/simulate")
    ApiResponse<VehicleLocationDto> simulateMovement(
            @PathVariable String vehicleId,
            @RequestParam(required = false) String tripId) {
        return ApiResponse.<VehicleLocationDto>builder()
                .result(gpsService.simulateMovement(vehicleId, tripId))
                .build();
    }
}
