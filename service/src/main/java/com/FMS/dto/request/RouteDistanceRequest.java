package com.FMS.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class RouteDistanceRequest {
    @NotBlank(message = "INVALID_ROUTE_ADDRESS")
    String startLocation;

    @NotBlank(message = "INVALID_ROUTE_ADDRESS")
    String endLocation;
}
