package com.FMS.dto;

import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.ExpenseType;
import com.FMS.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseDto {
    String id;

    ExpenseType expenseType;

    List<ExpenseType> expenseTypes;

    Double amount;

    String description;

    String receiptImageUrl;

    LocalDate expenseDate;

    String tripId;

    ExpenseStatus status;

    String recordedByUserId;

    String recordedByName;

    Role recordedByRole;

    String reviewedByUserId;

    String reviewedByName;

    LocalDateTime reviewedAt;

    String reviewNote;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
