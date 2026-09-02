package com.FMS.dto.request;

import com.FMS.enums.ExpenseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseCreationRequest {
    @NotBlank(message = "TRIP_NOT_FOUND")
    String tripId;

    ExpenseType expenseType;

    @Size(min = 1, message = "INVALID_EXPENSE_INPUT")
    List<ExpenseType> expenseTypes;

    @NotNull(message = "INVALID_EXPENSE_INPUT")
    @Positive(message = "INVALID_EXPENSE_INPUT")
    Double amount;

    @Size(max = 500, message = "INVALID_EXPENSE_INPUT")
    String description;

    @Size(max = 2_200_000, message = "INVALID_EXPENSE_RECEIPT")
    String receiptImageUrl;

    @NotNull(message = "INVALID_EXPENSE_INPUT")
    @PastOrPresent(message = "INVALID_EXPENSE_INPUT")
    LocalDate expenseDate;
}
