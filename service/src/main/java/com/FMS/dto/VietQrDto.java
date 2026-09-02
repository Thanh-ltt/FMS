package com.FMS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietQrDto {
    private String invoiceId;
    private String invoiceCode;
    private Double amount;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrImageUrl;
}
