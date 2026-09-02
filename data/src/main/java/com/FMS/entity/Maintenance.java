package com.FMS.entity;

import com.FMS.converter.StringListConverter;
import com.FMS.enums.MaintenanceStatus;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "maintenances")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Maintenance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    Vehicle vehicle;

    String maintenanceType;

    @Convert(converter = StringListConverter.class)
    @Column(name = "maintenance_types", length = 1000)
    List<String> maintenanceTypes;

    String description;

    Double cost;

    LocalDate maintenanceDate;

    LocalDate nextMaintenanceDate;

    LocalDateTime startedAt;

    LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    MaintenanceStatus status;
}
