package com.FMS.dto.request;

import com.FMS.validation.ValidationPatterns;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverCreationRequest {
    @NotBlank(message = "INVALID_PERSON_NAME")
    @Size(min = 2, max = 100, message = "INVALID_PERSON_NAME")
    String name;

    @NotNull(message = "INVALID_DATE_OF_BIRTH")
    @Past(message = "INVALID_DATE_OF_BIRTH")
    LocalDate dob;

    @NotBlank(message = "INVALID_PHONE")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "INVALID_PHONE")
    String phone;

    @NotBlank(message = "INVALID_DRIVER_LICENSE")
    @Pattern(regexp = ValidationPatterns.DRIVER_LICENSE, message = "INVALID_DRIVER_LICENSE")
    String licenseNumber;

    @NotNull(message = "INVALID_DRIVER_LICENSE")
    @FutureOrPresent(message = "INVALID_DRIVER_LICENSE")
    LocalDate licenseExpiration;

    @NotBlank(message = "INVALID_ADDRESS")
    @Size(min = 5, max = 255, message = "INVALID_ADDRESS")
    String address;

    String avatarUrl;
}
