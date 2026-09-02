package com.FMS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMatchDto {
    private String vehicleId;
    private String licensePlate;
    private String vehicleType;
    private Double capacity;
    private Double matchScore; // 0 - 100%
    private String recommendationReason;
}
