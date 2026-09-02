package com.FMS.dto;

import com.FMS.enums.VehicleStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VehicleDto {
    String id;
    String licensePlate;
    String vehicleType;
    Double capacity;
    VehicleStatus status;
}
