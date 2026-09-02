package com.FMS.entity;

import com.FMS.model.BaseEntity;
import com.FMS.enums.Role;
import com.FMS.validation.ValidationPatterns;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Driver extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String userId;

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

    @Column(columnDefinition = "TEXT")
    String avatarUrl;

    String accountProvisionedByUserId;

    String accountProvisionedByName;

    @Enumerated(EnumType.STRING)
    Role accountProvisionedByRole;

    LocalDateTime accountProvisionedAt;
}
