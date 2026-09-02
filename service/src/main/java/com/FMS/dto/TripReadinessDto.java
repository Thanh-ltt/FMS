package com.FMS.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripReadinessDto {
    String tripId;
    Boolean ready;
    LocalDateTime checkedAt;
    Integer passedCount;
    Integer blockedCount;
    Integer waitingCount;
    Integer notApplicableCount;
    String primaryBlockerCode;
    String primaryBlockerMessage;
    String primaryResolution;
    DepositSummaryDto depositSummary;
    List<TripReadinessCheckDto> checks;
}
