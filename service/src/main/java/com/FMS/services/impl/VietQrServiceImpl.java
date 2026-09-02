package com.FMS.services.impl;

import com.FMS.dto.VietQrDto;
import com.FMS.entity.Invoice;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.services.VietQrService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VietQrServiceImpl implements VietQrService {

    InvoiceRepository invoiceRepository;

    private static final String BANK_ID = "MB"; // MBBank
    private static final String BANK_NAME = "Ngân hàng TMCP Quân Đội (MBBank)";
    private static final String ACCOUNT_NO = "0388888888";
    private static final String ACCOUNT_NAME = "CTY VAN TAI LOGISTICS FMS";

    @Override
    public VietQrDto generateInvoiceQr(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        double amountToPay = invoice.getAmountDue();
        if (amountToPay <= 0) {
            amountToPay = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : 0.0;
        }

        String invNo = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getId().substring(0, 8);
        String transferContent = "FMS THANH TOAN " + invNo;

        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedAccountName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8);

        long amountInt = Math.round(amountToPay);

        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                BANK_ID, ACCOUNT_NO, amountInt, encodedContent, encodedAccountName
        );

        return VietQrDto.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invNo)
                .amount(amountToPay)
                .bankName(BANK_NAME)
                .accountNumber(ACCOUNT_NO)
                .accountName(ACCOUNT_NAME)
                .transferContent(transferContent)
                .qrImageUrl(qrUrl)
                .build();
    }
}
