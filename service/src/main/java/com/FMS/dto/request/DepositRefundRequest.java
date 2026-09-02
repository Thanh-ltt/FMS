package com.FMS.dto.request;

import com.FMS.enums.PaymentMethod;
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
public class DepositRefundRequest {
    @NotNull(message = "INVALID_DEPOSIT_AMOUNT")
    @Positive(message = "INVALID_DEPOSIT_AMOUNT")
    Double amount;

    @NotNull(message = "INVALID_DEPOSIT_DATE")
    @PastOrPresent(message = "INVALID_DEPOSIT_DATE")
    LocalDate refundDate;

    @NotNull(message = "INVALID_PAYMENT_METHOD")
    PaymentMethod paymentMethod;

    @Size(max = 100, message = "INVALID_PAYMENT_METHOD")
    String bankName;
    @Size(max = 100, message = "INVALID_PAYMENT_METHOD")
    String accountHolder;
    @Size(max = 50, message = "INVALID_PAYMENT_METHOD")
    String accountNumber;
    @Size(max = 100, message = "INVALID_PAYMENT_METHOD")
    String referenceNumber;
    @Size(max = 500, message = "INVALID_DEPOSIT_POLICY")
    String note;
}
