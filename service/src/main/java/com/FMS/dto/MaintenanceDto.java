package com.FMS.dto;

import com.FMS.enums.MaintenanceStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceDto {
    String id;

    String vehicleId;

    String vehiclePlate;

    String maintenanceType;

    List<String> maintenanceTypes;

    String description;

    Double cost;

    LocalDate maintenanceDate;

    LocalDate nextMaintenanceDate;

    LocalDateTime startedAt;

    LocalDateTime completedAt;

    MaintenanceStatus status;
}
