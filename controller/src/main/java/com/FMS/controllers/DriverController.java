package com.FMS.controllers;

import com.FMS.dto.DriverDto;
import com.FMS.entity.Driver;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.DriverServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.FMS.dto.request.DriverCreationRequest;
import com.FMS.dto.request.DriverAccountProvisionRequest;
import com.FMS.dto.request.DriverPasswordResetRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.FMS.entity.User;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;

@Slf4j
@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DriverController {

    DriverServiceImpl driverServiceImpl;

    @PostMapping
    ApiResponse<DriverDto> create(@RequestBody @Valid DriverCreationRequest request) {
        return ApiResponse.<DriverDto>builder()
                .result(driverServiceImpl.createDriver(request))
                .build();
    }

    @GetMapping("/my-profile")
    ApiResponse<DriverDto> getMyProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<DriverDto>builder()
                .result(driverServiceImpl.getMyProfile(username))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<DriverDto> update(@PathVariable String id, @RequestBody @Valid Driver request) {
        return ApiResponse.<DriverDto>builder()
                .result(driverServiceImpl.updateDriver(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        driverServiceImpl.delete(id);
        return ApiResponse.<String>builder()
                .result("Driver deleted successfully")
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<DriverDto> getById(@PathVariable String id) {
        return ApiResponse.<DriverDto>builder()
                .result(driverServiceImpl.getById(id))
                .build();
    }

    @GetMapping
    ApiResponse<List<DriverDto>> getAll() {
        return ApiResponse.<List<DriverDto>>builder()
                .result(driverServiceImpl.getAll())
                .build();
    }

    @GetMapping("/available")
    ApiResponse<List<DriverDto>> getDriversWithoutActiveTrip() {
        return ApiResponse.<List<DriverDto>>builder()
                .result(driverServiceImpl.findDriversWithoutActiveTrip())
                .build();
    }

    @GetMapping("/expired-licenses")
    ApiResponse<List<DriverDto>> getDriversWithExpiredLicenses() {
        return ApiResponse.<List<DriverDto>>builder()
                .result(driverServiceImpl.findExpiredLicenses())
                .build();
    }

    @PostMapping("/{id}/account")
    ApiResponse<DriverDto> provisionAccount(
            @PathVariable String id,
            @RequestBody @Valid DriverAccountProvisionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.<DriverDto>builder()
                .result(driverServiceImpl.provisionAccount(id, request, currentUser(authentication)))
                .build();
    }

    @PatchMapping("/{id}/account/password")
    ApiResponse<DriverDto> resetAccountPassword(
            @PathVariable String id,
            @RequestBody @Valid DriverPasswordResetRequest request,
            Authentication authentication
    ) {
        return ApiResponse.<DriverDto>builder()
                .result(driverServiceImpl.resetAccountPassword(id, request, currentUser(authentication)))
                .build();
    }

    @DeleteMapping("/{id}/account")
    ApiResponse<DriverDto> revokeAccount(@PathVariable String id, Authentication authentication) {
        return ApiResponse.<DriverDto>builder()
                .result(driverServiceImpl.revokeAccount(id, currentUser(authentication)))
                .build();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }
}
