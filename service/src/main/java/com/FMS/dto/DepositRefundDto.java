package com.FMS.dto;

import com.FMS.enums.PaymentMethod;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositRefundDto {
    String id;
    String depositId;
    Double amount;
    LocalDate refundDate;
    PaymentMethod paymentMethod;
    String bankName;
    String accountHolder;
    String accountNumber;
    String referenceNumber;
    String note;
}
