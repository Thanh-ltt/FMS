package com.FMS.controllers;

import com.FMS.dto.RouteDistanceDto;
import com.FMS.dto.request.RouteDistanceRequest;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.RouteServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RouteController {
    RouteServiceImpl routeServiceImpl;

    @PostMapping("/distance")
    ApiResponse<RouteDistanceDto> calculateDistance(@RequestBody @Valid RouteDistanceRequest request) {
        return ApiResponse.<RouteDistanceDto>builder()
                .result(routeServiceImpl.calculateDistance(request))
                .build();
    }
}
