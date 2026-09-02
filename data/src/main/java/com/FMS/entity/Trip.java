package com.FMS.entity;

import com.FMS.enums.TripStatus;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "trips")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    Driver driver;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    Contract contract;

    @NotBlank(message = "INVALID_ROUTE_ADDRESS")
    @Size(max = 255, message = "INVALID_ROUTE_ADDRESS")
    String startLocation;

    @NotBlank(message = "INVALID_ROUTE_ADDRESS")
    @Size(max = 255, message = "INVALID_ROUTE_ADDRESS")
    String endLocation;

    @NotBlank(message = "INVALID_TRIP_TIME")
    String startTime;

    @NotBlank(message = "INVALID_TRIP_TIME")
    String endTime;

    @NotNull(message = "INVALID_FREIGHT_INPUT")
    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double distanceKm;

    @NotNull(message = "INVALID_FREIGHT_INPUT")
    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double cargoWeightTon;

    @NotNull(message = "INVALID_FREIGHT_INPUT")
    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double freightRatePerTonKm;

    @NotNull(message = "INVALID_FREIGHT_INPUT")
    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double freightAmount;

    @Enumerated(EnumType.STRING)
    TripStatus status;
}
