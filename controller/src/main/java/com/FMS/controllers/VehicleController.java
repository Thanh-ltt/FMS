package com.FMS.controllers;

import com.FMS.dto.VehicleDto;
import com.FMS.entity.Vehicle;
import com.FMS.enums.VehicleStatus;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.VehicleServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VehicleController {

    VehicleServiceImpl vehicleServiceImpl;

    @PostMapping
    ApiResponse<VehicleDto> create(@RequestBody @Valid Vehicle request) {
        return ApiResponse.<VehicleDto>builder()
                .result(vehicleServiceImpl.create(request))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<VehicleDto> update(@PathVariable String id, @RequestBody @Valid Vehicle request) {
        return ApiResponse.<VehicleDto>builder()
                .result(vehicleServiceImpl.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        vehicleServiceImpl.delete(id);
        return ApiResponse.<String>builder()
                .result("Vehicle deleted successfully")
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<VehicleDto> getById(@PathVariable String id) {
        return ApiResponse.<VehicleDto>builder()
                .result(vehicleServiceImpl.getById(id))
                .build();
    }

    @GetMapping
    ApiResponse<List<VehicleDto>> getAll() {
        return ApiResponse.<List<VehicleDto>>builder()
                .result(vehicleServiceImpl.getAll())
                .build();
    }

    @GetMapping("/status/{status}")
    ApiResponse<List<VehicleDto>> getByStatus(@PathVariable VehicleStatus status) {
        return ApiResponse.<List<VehicleDto>>builder()
                .result(vehicleServiceImpl.getByStatus(status))
                .build();
    }

    @GetMapping("/available")
    ApiResponse<List<VehicleDto>> getAvailableVehicles() {
        return ApiResponse.<List<VehicleDto>>builder()
                .result(vehicleServiceImpl.findAvailableVehicles())
                .build();
    }

    @GetMapping("/maintenance")
    ApiResponse<List<VehicleDto>> getVehiclesInMaintenance() {
        return ApiResponse.<List<VehicleDto>>builder()
                .result(vehicleServiceImpl.findVehiclesInMaintenance())
                .build();
    }

    @GetMapping("/available/count")
    ApiResponse<Long> countAvailableVehicles() {
        return ApiResponse.<Long>builder()
                .result(vehicleServiceImpl.countAvailableVehicles())
                .build();
    }

    @GetMapping("/search")
    ApiResponse<List<VehicleDto>> searchByLicensePlate(@RequestParam String keyword) {
        return ApiResponse.<List<VehicleDto>>builder()
                .result(vehicleServiceImpl.searchByLicensePlate(keyword))
                .build();
    }

    @GetMapping("/capacity")
    ApiResponse<List<VehicleDto>> findByCapacityBetween(
            @RequestParam Double min,
            @RequestParam Double max) {
        return ApiResponse.<List<VehicleDto>>builder()
                .result(vehicleServiceImpl.findByCapacityBetween(min, max))
                .build();
    }

    @PatchMapping("/{id}/status")
    ApiResponse<String> updateStatus(
            @PathVariable String id,
            @RequestParam VehicleStatus status) {
        vehicleServiceImpl.updateStatus(id, status);
        return ApiResponse.<String>builder()
                .result("Vehicle status updated successfully")
                .build();
    }
}
