package com.FMS.entity;

import com.FMS.enums.VehicleStatus;
import com.FMS.model.BaseEntity;
import com.FMS.validation.ValidationPatterns;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "vehicles")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "INVALID_VEHICLE_INPUT")
    @Pattern(regexp = ValidationPatterns.VEHICLE_LICENSE_PLATE, message = "INVALID_VEHICLE_INPUT")
    String licensePlate;

    @NotBlank(message = "INVALID_VEHICLE_INPUT")
    @Size(max = 50, message = "INVALID_VEHICLE_INPUT")
    String vehicleType;

    @NotNull(message = "INVALID_VEHICLE_INPUT")
    @Positive(message = "INVALID_VEHICLE_INPUT")
    Double capacity;

    @Enumerated(EnumType.STRING)
    VehicleStatus status;
}
