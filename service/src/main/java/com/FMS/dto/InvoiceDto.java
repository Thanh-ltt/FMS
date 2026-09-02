package com.FMS.dto;

import com.FMS.enums.InvoiceStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDto {
    String id;

    String invoiceNumber;
    String customerId;
    String customerName;
    String customerUsername;

    String tripId;

    Double totalAmount;

    Double depositAppliedAmount;

    Double paidAmount;

    Double amountDue;

    LocalDate issueDate;

    LocalDate dueDate;

    InvoiceStatus status;
}
