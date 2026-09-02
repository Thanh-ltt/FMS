package com.FMS.services;

import com.FMS.dto.FinancialReportDto;
import com.FMS.enums.TripStatus;

import java.time.LocalDate;

import com.FMS.dto.ReportAnalyticsDto;

public interface ReportService {
    FinancialReportDto getFinancialReport(
            LocalDate fromDate,
            LocalDate toDate,
            String customerId,
            TripStatus tripStatus
    );

    ReportAnalyticsDto getAnalyticsData(LocalDate fromDate, LocalDate toDate);
}
