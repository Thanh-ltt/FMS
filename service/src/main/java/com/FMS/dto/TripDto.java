package com.FMS.dto;

import com.FMS.enums.TripStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripDto {
    String id;

    String vehicleId;
    String vehiclePlate;
    String driverId;
    String driverName;
    String customerId;
    String customerName;
    String customerUsername;
    String contractId;
    String contractCode;

    String startLocation;
    String endLocation;

    String startTime;
    String endTime;
    Double distanceKm;
    Double cargoWeightTon;
    Double freightRatePerTonKm;
    Double freightAmount;
    DepositSummaryDto depositSummary;
    ExpenseSummaryDto expenseSummary;
    TripStatus status;
}
