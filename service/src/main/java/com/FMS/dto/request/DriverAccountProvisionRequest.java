package com.FMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverAccountProvisionRequest {
    @NotBlank(message = "INVALID_USERNAME")
    @Size(min = 6, max = 50, message = "INVALID_USERNAME")
    String username;

    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 8, max = 72, message = "INVALID_PASSWORD")
    String password;
}
