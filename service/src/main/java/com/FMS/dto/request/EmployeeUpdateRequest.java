package com.FMS.dto.request;

import com.FMS.enums.Gender;
import com.FMS.enums.Role;
import com.FMS.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
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
public class EmployeeUpdateRequest {
    @NotBlank(message = "INVALID_EMPLOYEE_INPUT")
    @Size(min = 6, max = 50, message = "INVALID_USERNAME")
    String username;

    @Size(min = 8, max = 72, message = "INVALID_PASSWORD")
    String password;

    @NotBlank(message = "INVALID_EMPLOYEE_INPUT")
    @Pattern(regexp = ValidationPatterns.EMPLOYEE_CODE, message = "INVALID_EMPLOYEE_INPUT")
    String employeeCode;

    @NotBlank(message = "INVALID_PERSON_NAME")
    @Size(min = 2, max = 100, message = "INVALID_PERSON_NAME")
    String fullName;

    @NotBlank(message = "INVALID_PHONE")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "INVALID_PHONE")
    String phone;

    @NotBlank(message = "INVALID_EMAIL")
    @Email(message = "INVALID_EMAIL")
    @Size(max = 254, message = "INVALID_EMAIL")
    String email;

    @NotBlank(message = "INVALID_ADDRESS")
    @Size(min = 5, max = 255, message = "INVALID_ADDRESS")
    String address;

    @NotBlank(message = "INVALID_ID_NUMBER")
    @Pattern(regexp = ValidationPatterns.ID_NUMBER, message = "INVALID_ID_NUMBER")
    String idNumber;

    @NotNull(message = "INVALID_DATE_OF_BIRTH")
    @Past(message = "INVALID_DATE_OF_BIRTH")
    LocalDate dob;

    @NotNull(message = "INVALID_EMPLOYEE_INPUT")
    Gender gender;

    @Size(max = 100, message = "INVALID_EMPLOYEE_INPUT")
    String position;

    @NotNull(message = "INVALID_EMPLOYEE_DATE")
    @PastOrPresent(message = "INVALID_EMPLOYEE_DATE")
    LocalDate hireDate;
    String avatarUrl;

    @NotNull(message = "INVALID_EMPLOYEE_ROLE")
    Role role;
}
