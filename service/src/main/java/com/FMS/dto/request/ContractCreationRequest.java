package com.FMS.dto.request;

import com.FMS.enums.ContractValueMode;
import com.FMS.enums.DepositScope;
import com.FMS.enums.DepositType;
import com.FMS.enums.DepositUsage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractCreationRequest {
    @Size(max = 50, message = "INVALID_CONTRACT_INPUT")
    String contractCode;

    @NotBlank(message = "CUSTOMER_NOT_FOUND")
    String customerId;

    @NotNull(message = "INVALID_CONTRACT_DATE")
    @PastOrPresent(message = "INVALID_CONTRACT_DATE")
    LocalDate signedDate;

    @NotNull(message = "INVALID_CONTRACT_DATE")
    LocalDate startDate;

    @NotNull(message = "INVALID_CONTRACT_DATE")
    LocalDate endDate;

    @NotBlank(message = "INVALID_CONTRACT_INPUT")
    @Size(max = 500, message = "INVALID_CONTRACT_INPUT")
    String cargoDescription;

    @NotBlank(message = "INVALID_CONTRACT_INPUT")
    @Size(max = 50, message = "INVALID_CONTRACT_INPUT")
    String cargoType;

    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double freightRatePerTonKm;

    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double estimatedDistanceKm;

    @Positive(message = "INVALID_FREIGHT_INPUT")
    Double estimatedCargoWeightTon;

    @Positive(message = "INVALID_CONTRACT_INPUT")
    Double contractValue;

    ContractValueMode valueMode;

    Boolean depositRequired;

    DepositScope depositScope;

    DepositType depositType;

    @Positive(message = "INVALID_DEPOSIT_POLICY")
    Double depositValue;

    DepositUsage depositUsage;

    @PositiveOrZero(message = "INVALID_DEPOSIT_POLICY")
    Integer depositDueDays;

    @Size(max = 1000, message = "INVALID_DEPOSIT_POLICY")
    String depositTerms;
}
