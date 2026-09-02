package com.FMS.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerPortalDto {
    CustomerDto profile;

    List<ContractDto> contracts;

    List<TripDto> trips;

    List<InvoiceDto> invoices;

    List<DepositDto> deposits;
}
