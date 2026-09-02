package com.FMS.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.FMS.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverDto {
    String id;
    String userId;
    String username;
    String name;
    LocalDate dob;
    String phone;

    String licenseNumber;
    LocalDate licenseExpiration;

    String address;
    String avatarUrl;
    String accountProvisionedByUserId;
    String accountProvisionedByName;
    Role accountProvisionedByRole;
    LocalDateTime accountProvisionedAt;
}
