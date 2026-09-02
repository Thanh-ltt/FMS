package com.FMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripCreationRequest {
    @NotBlank(message = "VEHICLE_NOT_FOUND")
    String vehicleId;

    @NotBlank(message = "DRIVER_NOT_FOUND")
    String driverId;

    @NotBlank(message = "CUSTOMER_NOT_FOUND")
    String customerId;

    @Size(max = 36, message = "CONTRACT_NOT_FOUND")
    String contractId;

    @NotBlank(message = "INVALID_ROUTE_ADDRESS")
    @Size(max = 255, message = "INVALID_ROUTE_ADDRESS")
    String startLocation;

    @NotBlank(message = "INVALID_ROUTE_ADDRESS")
    @Size(max = 255, message = "INVALID_ROUTE_ADDRESS")
    String endLocation;

    @NotBlank(message = "INVALID_TRIP_TIME")
    String startTime;

    @NotBlank(message = "INVALID_TRIP_TIME")
    String endTime;

    @NotNull(message = "INVALID_FREIGHT_INPUT")
    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double distanceKm;

    @NotNull(message = "INVALID_FREIGHT_INPUT")
    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double cargoWeightTon;

    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double freightRatePerTonKm;
}
