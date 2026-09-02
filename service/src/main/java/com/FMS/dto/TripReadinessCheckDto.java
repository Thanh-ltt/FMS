package com.FMS.dto;

import com.FMS.enums.TripReadinessCheckStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripReadinessCheckDto {
    String key;
    String group;
    String label;
    TripReadinessCheckStatus status;
    String message;
    String resolution;
}
