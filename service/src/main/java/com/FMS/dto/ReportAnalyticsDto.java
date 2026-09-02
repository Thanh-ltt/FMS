package com.FMS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAnalyticsDto {
    private Double totalRevenue;
    private Double totalTripExpense;
    private Double totalMaintenanceExpense;
    private Double totalNetProfit;
    private List<MonthlyFinancialPoint> monthlyPoints;
    private List<VehicleStatusCount> vehicleStatusCounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyFinancialPoint {
        private String month; // e.g. "T1", "T2", ...
        private Double revenue;
        private Double expense;
        private Double profit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleStatusCount {
        private String status;
        private String label;
        private Long count;
    }
}
