package com.FMS.dto;

import com.FMS.enums.DepositStatus;
import com.FMS.enums.PaymentMethod;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositDto {
    String id;
    String receiptNumber;
    String customerId;
    String customerName;
    String customerUsername;
    String contractId;
    String contractCode;
    String tripId;
    Double amount;
    Double allocatedAmount;
    Double refundedAmount;
    Double availableAmount;
    LocalDate receivedDate;
    PaymentMethod paymentMethod;
    String bankName;
    String accountHolder;
    String accountNumber;
    String referenceNumber;
    String note;
    DepositStatus status;
}
