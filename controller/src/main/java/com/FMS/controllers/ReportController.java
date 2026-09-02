package com.FMS.controllers;

import com.FMS.dto.FinancialReportDto;
import com.FMS.enums.TripStatus;
import com.FMS.response.ApiResponse;
import com.FMS.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import com.FMS.dto.ReportAnalyticsDto;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/financial")
    ApiResponse<FinancialReportDto> getFinancialReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) TripStatus tripStatus
    ) {
        return ApiResponse.<FinancialReportDto>builder()
                .result(reportService.getFinancialReport(fromDate, toDate, customerId, tripStatus))
                .build();
    }

    @GetMapping("/analytics")
    ApiResponse<ReportAnalyticsDto> getAnalytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ApiResponse.<ReportAnalyticsDto>builder()
                .result(reportService.getAnalyticsData(fromDate, toDate))
                .build();
    }
}
