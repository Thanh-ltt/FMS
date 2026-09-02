package com.FMS.controllers;

import com.FMS.dto.ContractDto;
import com.FMS.dto.request.ContractCreationRequest;
import com.FMS.enums.ContractStatus;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.ContractServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractController {

    ContractServiceImpl contractServiceImpl;

    @PostMapping
    ApiResponse<ContractDto> create(@RequestBody @Valid ContractCreationRequest request) {
        return ApiResponse.<ContractDto>builder()
                .result(contractServiceImpl.create(request))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<ContractDto> update(
            @PathVariable String id,
            @RequestBody @Valid ContractCreationRequest request
    ) {
        return ApiResponse.<ContractDto>builder()
                .result(contractServiceImpl.update(id, request))
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<ContractDto> getById(@PathVariable String id) {
        return ApiResponse.<ContractDto>builder()
                .result(contractServiceImpl.getById(id))
                .build();
    }

    @GetMapping
    ApiResponse<List<ContractDto>> getAll() {
        return ApiResponse.<List<ContractDto>>builder()
                .result(contractServiceImpl.getAll())
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        contractServiceImpl.delete(id);
        return ApiResponse.<String>builder()
                .result("Contract deleted successfully")
                .build();
    }

    @GetMapping("/status/{status}")
    ApiResponse<List<ContractDto>> getByStatus(@PathVariable ContractStatus status) {
        return ApiResponse.<List<ContractDto>>builder()
                .result(contractServiceImpl.findByStatus(status))
                .build();
    }

    @PatchMapping("/{id}/activate")
    ApiResponse<String> activateContract(@PathVariable String id) {
        contractServiceImpl.activateContract(id);
        return ApiResponse.<String>builder()
                .result("Contract activated successfully")
                .build();
    }

    @PatchMapping("/{id}/complete")
    ApiResponse<String> completeContract(@PathVariable String id) {
        contractServiceImpl.completeContract(id);
        return ApiResponse.<String>builder()
                .result("Contract completed successfully")
                .build();
    }

    @PatchMapping("/{id}/cancel")
    ApiResponse<String> cancelContract(@PathVariable String id) {
        contractServiceImpl.cancelContract(id);
        return ApiResponse.<String>builder()
                .result("Contract cancelled successfully")
                .build();
    }
}
