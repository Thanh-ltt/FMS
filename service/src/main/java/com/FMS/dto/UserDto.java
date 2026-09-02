package com.FMS.dto;

import com.FMS.enums.Role;
import com.FMS.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDto {
    String id;

    String username;

    Role role;

    String employeeCode;
    String fullName;
    String phone;
    String email;
    String address;
    String idNumber;
    LocalDate dob;
    Gender gender;
    String position;
    LocalDate hireDate;
    String avatarUrl;
    Boolean active;
    Boolean mustChangePassword;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
