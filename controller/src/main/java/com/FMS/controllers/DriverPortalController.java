package com.FMS.controllers;

import com.FMS.dto.TripProofDto;
import com.FMS.dto.request.TripProofCreateRequest;
import com.FMS.response.ApiResponse;
import com.FMS.services.DriverPortalService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/driver-portal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DriverPortalController {

    DriverPortalService driverPortalService;

    @PostMapping("/trips/{tripId}/epod")
    ApiResponse<TripProofDto> createProof(
            @PathVariable String tripId,
            @RequestBody @Valid TripProofCreateRequest request) {
        return ApiResponse.<TripProofDto>builder()
                .result(driverPortalService.createProof(tripId, request))
                .build();
    }

    @GetMapping("/trips/{tripId}/epod")
    ApiResponse<TripProofDto> getProofByTripId(@PathVariable String tripId) {
        return ApiResponse.<TripProofDto>builder()
                .result(driverPortalService.getProofByTripId(tripId))
                .build();
    }

    @PostMapping("/trips/{tripId}/start")
    ApiResponse<String> startTrip(@PathVariable String tripId) {
        driverPortalService.startTrip(tripId);
        return ApiResponse.<String>builder()
                .result("Bắt đầu chuyến đi thành công")
                .build();
    }

    @PostMapping("/trips/{tripId}/complete-with-epod")
    ApiResponse<TripProofDto> completeTripWithProof(
            @PathVariable String tripId,
            @RequestBody @Valid TripProofCreateRequest request) {
        return ApiResponse.<TripProofDto>builder()
                .result(driverPortalService.completeTripWithProof(tripId, request))
                .build();
    }
}
