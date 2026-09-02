package com.FMS.dto.request;

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
public class MaintenanceUpdateRequest {
    String maintenanceType;

    @Size(min = 1, message = "INVALID_MAINTENANCE_INPUT")
    List<String> maintenanceTypes;

    @Size(max = 500, message = "INVALID_MAINTENANCE_INPUT")
    String description;

    @PositiveOrZero(message = "INVALID_MAINTENANCE_INPUT")
    Double cost;

    LocalDate maintenanceDate;
    LocalDate nextMaintenanceDate;
}
