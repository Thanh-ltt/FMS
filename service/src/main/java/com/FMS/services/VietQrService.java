package com.FMS.services;

import com.FMS.dto.VietQrDto;

public interface VietQrService {
    VietQrDto generateInvoiceQr(String invoiceId);
}
