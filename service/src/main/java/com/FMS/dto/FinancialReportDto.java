package com.FMS.dto;

import com.FMS.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportDto {
    LocalDate fromDate;
    LocalDate toDate;
    String customerId;
    TripStatus tripStatus;
    boolean maintenanceIncluded;
    Totals totals;
    List<TripRow> trips;
    List<CustomerRow> customers;
    List<MonthlyRow> monthly;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        long tripCount;
        long completedTripCount;
        double distanceKm;
        double recognizedRevenue;
        double paidRevenue;
        double depositApplied;
        double depositAvailable;
        double outstanding;
        double tripExpense;
        double maintenanceExpense;
        double totalExpense;
        double grossProfit;
        double netProfit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripRow {
        String id;
        String code;
        TripStatus status;
        String customerId;
        String customerName;
        String vehiclePlate;
        String startTime;
        String endTime;
        String route;
        double distanceKm;
        double cargoWeightTon;
        double freightAmount;
        double revenue;
        double paidRevenue;
        double depositApplied;
        double outstanding;
        double cost;
        double profit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRow {
        String customerId;
        String customerName;
        long tripCount;
        long completedTripCount;
        double distanceKm;
        double revenue;
        double paidRevenue;
        double depositApplied;
        double outstanding;
        double tripCost;
        double profit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRow {
        String period;
        String label;
        double revenue;
        double paidRevenue;
        double depositApplied;
        double outstanding;
        double tripCost;
        double maintenanceCost;
        double netProfit;
    }
}
