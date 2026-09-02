package com.FMS.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseSummaryDto {
    Double approvedAmount;
    Double pendingAmount;
    Integer approvedCount;
    Integer pendingCount;
    Integer rejectedCount;
    Integer totalCount;
}
