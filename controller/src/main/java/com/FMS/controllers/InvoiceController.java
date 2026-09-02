package com.FMS.controllers;

import com.FMS.dto.InvoiceDto;
import com.FMS.dto.InvoicePaymentDto;
import com.FMS.dto.request.InvoiceCreationRequest;
import com.FMS.dto.request.InvoicePaymentRequest;
import com.FMS.enums.InvoiceStatus;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.InvoiceServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceController {

    InvoiceServiceImpl invoiceServiceImpl;

    @PostMapping
    ApiResponse<InvoiceDto> create(@RequestBody @Valid InvoiceCreationRequest request) {
        return ApiResponse.<InvoiceDto>builder()
                .result(invoiceServiceImpl.createInvoice(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<InvoiceDto>> getAll() {
        return ApiResponse.<List<InvoiceDto>>builder()
                .result(invoiceServiceImpl.getAllInvoices())
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        invoiceServiceImpl.deleteInvoice(id);
        return ApiResponse.<String>builder()
                .result("Invoice deleted successfully")
                .build();
    }

    @GetMapping("/status/{status}")
    ApiResponse<List<InvoiceDto>> getByStatus(@PathVariable InvoiceStatus status) {
        return ApiResponse.<List<InvoiceDto>>builder()
                .result(invoiceServiceImpl.findByStatus(status))
                .build();
    }

    @GetMapping("/overdue")
    ApiResponse<List<InvoiceDto>> getOverdueInvoices() {
        return ApiResponse.<List<InvoiceDto>>builder()
                .result(invoiceServiceImpl.findOverdueInvoices())
                .build();
    }

    @GetMapping("/revenue")
    ApiResponse<Double> calculateRevenue() {
        return ApiResponse.<Double>builder()
                .result(invoiceServiceImpl.calculateRevenue())
                .build();
    }

    @GetMapping("/outstanding-debt")
    ApiResponse<Double> calculateOutstandingDebt() {
        return ApiResponse.<Double>builder()
                .result(invoiceServiceImpl.calculateOutstandingDebt())
                .build();
    }

    @GetMapping("/revenue/customer/{customerId}")
    ApiResponse<Double> calculateRevenueByCustomer(@PathVariable String customerId) {
        return ApiResponse.<Double>builder()
                .result(invoiceServiceImpl.calculateRevenueByCustomer(customerId))
                .build();
    }

    @GetMapping("/revenue/monthly")
    ApiResponse<Double> calculateRevenueByMonth(
            @RequestParam int month,
            @RequestParam int year) {
        return ApiResponse.<Double>builder()
                .result(invoiceServiceImpl.calculateRevenueByMonth(month, year))
                .build();
    }

    @PatchMapping("/{id}/pay")
    ApiResponse<InvoiceDto> markAsPaid(
            @PathVariable String id,
            @RequestBody @Valid InvoicePaymentRequest request) {
        return ApiResponse.<InvoiceDto>builder()
                .result(invoiceServiceImpl.markAsPaid(id, request))
                .build();
    }

    @GetMapping("/{id}/payments")
    ApiResponse<List<InvoicePaymentDto>> getPayments(@PathVariable String id) {
        return ApiResponse.<List<InvoicePaymentDto>>builder()
                .result(invoiceServiceImpl.getPayments(id))
                .build();
    }

    @PatchMapping("/{id}/cancel")
    ApiResponse<InvoiceDto> cancelInvoice(@PathVariable String id) {
        return ApiResponse.<InvoiceDto>builder()
                .result(invoiceServiceImpl.cancelInvoice(id))
                .build();
    }
}
