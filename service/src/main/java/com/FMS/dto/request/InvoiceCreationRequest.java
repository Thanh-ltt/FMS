package com.FMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceCreationRequest {
    @Size(max = 50, message = "INVALID_INVOICE_INPUT")
    String invoiceNumber;

    @NotBlank(message = "TRIP_NOT_FOUND")
    String tripId;

    @Positive(message = "INVALID_INVOICE_AMOUNT")
    Double totalAmount;

    @NotNull(message = "INVALID_INVOICE_DATE")
    @PastOrPresent(message = "INVALID_INVOICE_DATE")
    LocalDate issueDate;

    @NotNull(message = "INVALID_INVOICE_DATE")
    LocalDate dueDate;

    Boolean applyDeposit;

    @Positive(message = "INVALID_DEPOSIT_AMOUNT")
    Double depositAmount;
}
