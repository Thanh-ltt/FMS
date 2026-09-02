package com.FMS.controllers;

import com.FMS.dto.MaintenanceDto;
import com.FMS.dto.request.MaintenanceCreationRequest;
import com.FMS.dto.request.MaintenanceUpdateRequest;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.MaintenanceServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/maintenances")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceController {

    MaintenanceServiceImpl maintenanceServiceImpl;

    @PostMapping
    ApiResponse<MaintenanceDto> create(@RequestBody @Valid MaintenanceCreationRequest request) {
        return ApiResponse.<MaintenanceDto>builder()
                .result(maintenanceServiceImpl.createMaintenance(request))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<MaintenanceDto> update(
            @PathVariable String id,
            @RequestBody @Valid MaintenanceUpdateRequest request
    ) {
        return ApiResponse.<MaintenanceDto>builder()
                .result(maintenanceServiceImpl.updateMaintenance(id, request))
                .build();
    }

    @GetMapping
    ApiResponse<List<MaintenanceDto>> getAll() {
        return ApiResponse.<List<MaintenanceDto>>builder()
                .result(maintenanceServiceImpl.getAllMaintenances())
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        maintenanceServiceImpl.deleteMaintenance(id);
        return ApiResponse.<String>builder()
                .result("Maintenance deleted successfully")
                .build();
    }

    @GetMapping("/vehicle/{vehicleId}")
    ApiResponse<List<MaintenanceDto>> getByVehicle(@PathVariable String vehicleId) {
        return ApiResponse.<List<MaintenanceDto>>builder()
                .result(maintenanceServiceImpl.getByVehicle(vehicleId))
                .build();
    }

    @GetMapping("/vehicle/{vehicleId}/cost")
    ApiResponse<Double> calculateMaintenanceCost(@PathVariable String vehicleId) {
        return ApiResponse.<Double>builder()
                .result(maintenanceServiceImpl.calculateMaintenanceCost(vehicleId))
                .build();
    }

    @PatchMapping("/{id}/start")
    ApiResponse<String> startMaintenance(@PathVariable String id) {
        maintenanceServiceImpl.startMaintenance(id);
        return ApiResponse.<String>builder()
                .result("Maintenance started successfully")
                .build();
    }

    @PatchMapping("/{id}/complete")
    ApiResponse<String> completeMaintenance(@PathVariable String id) {
        maintenanceServiceImpl.completeMaintenance(id);
        return ApiResponse.<String>builder()
                .result("Maintenance completed successfully")
                .build();
    }

    @PatchMapping("/{id}/cancel")
    ApiResponse<String> cancelMaintenance(@PathVariable String id) {
        maintenanceServiceImpl.cancelMaintenance(id);
        return ApiResponse.<String>builder()
                .result("Maintenance cancelled successfully")
                .build();
    }
}
