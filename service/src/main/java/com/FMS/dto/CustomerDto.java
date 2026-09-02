package com.FMS.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerDto {
    String id;
    String name;
    String phone;
    String idNumber;
    LocalDate dob;
    String address;
    String userId;
    String username;
}
