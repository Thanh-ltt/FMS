package com.FMS.controllers;

import com.FMS.dto.DepositDto;
import com.FMS.dto.DepositRefundDto;
import com.FMS.dto.DepositSummaryDto;
import com.FMS.dto.request.DepositCreationRequest;
import com.FMS.dto.request.DepositRefundRequest;
import com.FMS.response.ApiResponse;
import com.FMS.services.DepositService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deposits")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositController {
    DepositService depositService;

    @PostMapping
    ApiResponse<DepositDto> create(@RequestBody @Valid DepositCreationRequest request) {
        return ApiResponse.<DepositDto>builder()
                .result(depositService.create(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<DepositDto>> getAll() {
        return ApiResponse.<List<DepositDto>>builder()
                .result(depositService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<DepositDto> getById(@PathVariable String id) {
        return ApiResponse.<DepositDto>builder()
                .result(depositService.getById(id))
                .build();
    }

    @GetMapping("/customer/{customerId}")
    ApiResponse<List<DepositDto>> getByCustomer(@PathVariable String customerId) {
        return ApiResponse.<List<DepositDto>>builder()
                .result(depositService.getByCustomer(customerId))
                .build();
    }

    @GetMapping("/contract/{contractId}")
    ApiResponse<List<DepositDto>> getByContract(@PathVariable String contractId) {
        return ApiResponse.<List<DepositDto>>builder()
                .result(depositService.getByContract(contractId))
                .build();
    }

    @GetMapping("/{id}/refunds")
    ApiResponse<List<DepositRefundDto>> getRefunds(@PathVariable String id) {
        return ApiResponse.<List<DepositRefundDto>>builder()
                .result(depositService.getRefunds(id))
                .build();
    }

    @PostMapping("/{id}/refunds")
    ApiResponse<DepositDto> refund(
            @PathVariable String id,
            @RequestBody @Valid DepositRefundRequest request
    ) {
        return ApiResponse.<DepositDto>builder()
                .result(depositService.refund(id, request))
                .build();
    }

    @GetMapping("/trip/{tripId}/summary")
    ApiResponse<DepositSummaryDto> getTripSummary(@PathVariable String tripId) {
        return ApiResponse.<DepositSummaryDto>builder()
                .result(depositService.getSummaryForTrip(tripId))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        depositService.delete(id);
        return ApiResponse.<String>builder()
                .result("Deposit deleted successfully")
                .build();
    }
}
