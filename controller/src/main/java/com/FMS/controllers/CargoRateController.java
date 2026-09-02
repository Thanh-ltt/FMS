package com.FMS.controllers;

import com.FMS.entity.CargoRate;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.CargoRateServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cargo-rates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CargoRateController {
    CargoRateServiceImpl cargoRateServiceImpl;

    @GetMapping
    ApiResponse<List<CargoRate>> getAll() {
        return ApiResponse.<List<CargoRate>>builder()
                .result(cargoRateServiceImpl.getAll())
                .build();
    }

    @PutMapping("/{cargoType}")
    ApiResponse<CargoRate> updateRate(@PathVariable String cargoType, @RequestBody CargoRate request) {
        return ApiResponse.<CargoRate>builder()
                .result(cargoRateServiceImpl.updateRate(cargoType, request))
                .build();
    }
}
