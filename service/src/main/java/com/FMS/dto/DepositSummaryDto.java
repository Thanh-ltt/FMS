package com.FMS.dto;

import com.FMS.enums.DepositScope;
import com.FMS.enums.DepositType;
import com.FMS.enums.DepositUsage;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositSummaryDto {
    Boolean required;
    DepositScope scope;
    DepositType type;
    DepositUsage usage;
    Double policyValue;
    Double requiredAmount;
    Double receivedAmount;
    Double allocatedAmount;
    Double refundedAmount;
    Double availableAmount;
    Double shortfallAmount;
}
