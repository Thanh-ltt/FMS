package com.FMS.entity;

import com.FMS.enums.ContractStatus;
import com.FMS.enums.ContractValueMode;
import com.FMS.enums.DepositScope;
import com.FMS.enums.DepositType;
import com.FMS.enums.DepositUsage;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "contracts")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Contract extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String contractCode;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;

    LocalDate signedDate;

    LocalDate startDate;

    LocalDate endDate;

    @Column(length = 1000)
    String cargoDescription;

    String cargoType;

    Double freightRatePerTonKm;

    Double estimatedDistanceKm;

    Double estimatedCargoWeightTon;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ContractValueMode valueMode = ContractValueMode.PER_TRIP;

    Double contractValue;

    @Builder.Default
    Boolean depositRequired = false;

    @Enumerated(EnumType.STRING)
    DepositScope depositScope;

    @Enumerated(EnumType.STRING)
    DepositType depositType;

    Double depositValue;

    @Enumerated(EnumType.STRING)
    DepositUsage depositUsage;

    Integer depositDueDays;

    @Column(length = 1000)
    String depositTerms;

    @Enumerated(EnumType.STRING)
    ContractStatus status;

    @Transient
    public Double getRequiredDepositAmount() {
        if (!Boolean.TRUE.equals(depositRequired) || depositValue == null) {
            return 0D;
        }
        if (depositScope == DepositScope.TRIP) {
            return null;
        }
        if (depositType == DepositType.PERCENTAGE) {
            return contractValue == null ? null : contractValue * depositValue / 100D;
        }
        return depositValue;
    }
}
