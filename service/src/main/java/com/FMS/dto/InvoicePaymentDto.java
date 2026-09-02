package com.FMS.dto;

import com.FMS.enums.PaymentMethod;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoicePaymentDto {
    String id;
    String invoiceId;
    Double amount;
    LocalDate paymentDate;
    PaymentMethod paymentMethod;
    String bankName;
    String accountHolder;
    String accountNumber;
    String transactionReference;
    String note;
    LocalDateTime createdAt;
}
