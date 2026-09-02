package com.FMS.controllers;

import com.FMS.dto.ContractDto;
import com.FMS.dto.CustomerDto;
import com.FMS.dto.InvoiceDto;
import com.FMS.dto.request.CustomerAccountCreationRequest;
import com.FMS.dto.request.CustomerAccountLinkRequest;
import com.FMS.dto.request.CustomerProfileRequest;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.CustomerServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerController {

    CustomerServiceImpl customerServiceImpl;

    @PostMapping
    ApiResponse<CustomerDto> create(@RequestBody @Valid CustomerProfileRequest request) {
        return ApiResponse.<CustomerDto>builder()
                .result(customerServiceImpl.create(request))
                .build();
    }

    @PostMapping("/with-new-account")
    ApiResponse<CustomerDto> createWithNewAccount(@RequestBody @Valid CustomerAccountCreationRequest request) {
        return ApiResponse.<CustomerDto>builder()
                .result(customerServiceImpl.createWithNewAccount(request))
                .build();
    }

    @PostMapping("/with-existing-account")
    ApiResponse<CustomerDto> createWithExistingAccount(@RequestBody @Valid CustomerAccountLinkRequest request) {
        return ApiResponse.<CustomerDto>builder()
                .result(customerServiceImpl.createWithExistingAccount(request))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<CustomerDto> update(@PathVariable String id, @RequestBody @Valid CustomerProfileRequest request) {
        return ApiResponse.<CustomerDto>builder()
                .result(customerServiceImpl.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        customerServiceImpl.delete(id);
        return ApiResponse.<String>builder()
                .result("Customer deleted successfully")
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<CustomerDto> getById(@PathVariable String id) {
        return ApiResponse.<CustomerDto>builder()
                .result(customerServiceImpl.getById(id))
                .build();
    }

    @GetMapping
    ApiResponse<List<CustomerDto>> getAll() {
        return ApiResponse.<List<CustomerDto>>builder()
                .result(customerServiceImpl.getAll())
                .build();
    }

    @GetMapping("/{id}/contracts")
    ApiResponse<List<ContractDto>> getContracts(@PathVariable String id) {
        return ApiResponse.<List<ContractDto>>builder()
                .result(customerServiceImpl.getContracts(id))
                .build();
    }

    @GetMapping("/{id}/invoices")
    ApiResponse<List<InvoiceDto>> getInvoices(@PathVariable String id) {
        return ApiResponse.<List<InvoiceDto>>builder()
                .result(customerServiceImpl.getInvoices(id))
                .build();
    }
}
