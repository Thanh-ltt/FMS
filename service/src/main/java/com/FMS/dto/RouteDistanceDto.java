package com.FMS.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RouteDistanceDto {
    Double distanceKm;
    Double durationMinutes;
    Integer routeCount;
    String source;
    String title;
    String detail;
    String startLabel;
    String endLabel;
}
