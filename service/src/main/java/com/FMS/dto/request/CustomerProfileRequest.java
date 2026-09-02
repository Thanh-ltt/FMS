package com.FMS.dto.request;

import com.FMS.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileRequest {
    @NotBlank(message = "INVALID_PERSON_NAME")
    @Size(min = 2, max = 100, message = "INVALID_PERSON_NAME")
    String name;

    @NotBlank(message = "INVALID_PHONE")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "INVALID_PHONE")
    String phone;

    @NotBlank(message = "INVALID_ID_NUMBER")
    @Pattern(regexp = ValidationPatterns.ID_NUMBER, message = "INVALID_ID_NUMBER")
    String idNumber;

    @NotNull(message = "INVALID_DATE_OF_BIRTH")
    @Past(message = "INVALID_DATE_OF_BIRTH")
    LocalDate dob;

    @NotBlank(message = "INVALID_ADDRESS")
    @Size(min = 5, max = 255, message = "INVALID_ADDRESS")
    String address;
}
