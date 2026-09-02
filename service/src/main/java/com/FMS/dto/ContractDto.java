package com.FMS.dto;

import com.FMS.enums.ContractStatus;
import com.FMS.enums.ContractValueMode;
import com.FMS.enums.DepositScope;
import com.FMS.enums.DepositType;
import com.FMS.enums.DepositUsage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractDto {

    String id;

    String contractCode;

    String customerId;

    String customerName;

    String customerUsername;

    LocalDate signedDate;

    LocalDate startDate;

    LocalDate endDate;

    String cargoDescription;

    String cargoType;

    Double freightRatePerTonKm;

    Double estimatedDistanceKm;

    Double estimatedCargoWeightTon;

    ContractValueMode valueMode;

    Double contractValue;

    Boolean depositRequired;

    DepositScope depositScope;

    DepositType depositType;

    Double depositValue;

    DepositUsage depositUsage;

    Integer depositDueDays;

    String depositTerms;

    Double requiredDepositAmount;

    ContractStatus status;

}
