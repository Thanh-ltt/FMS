package com.FMS.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceCreationRequest {
    @NotBlank(message = "VEHICLE_NOT_FOUND")
    String vehicleId;

    String maintenanceType;

    @Size(min = 1, message = "INVALID_MAINTENANCE_INPUT")
    List<String> maintenanceTypes;

    @Size(max = 500, message = "INVALID_MAINTENANCE_INPUT")
    String description;

    @PositiveOrZero(message = "INVALID_MAINTENANCE_INPUT")
    Double cost;

    @NotNull(message = "INVALID_MAINTENANCE_DATE")
    @FutureOrPresent(message = "INVALID_MAINTENANCE_DATE")
    LocalDate maintenanceDate;

    LocalDate nextMaintenanceDate;
}
