package com.FMS.controllers;

import com.FMS.dto.VietQrDto;
import com.FMS.response.ApiResponse;
import com.FMS.services.VietQrService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payment/vietqr")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VietQrController {

    VietQrService vietQrService;

    @GetMapping("/invoice/{invoiceId}")
    ApiResponse<VietQrDto> getInvoiceQr(@PathVariable String invoiceId) {
        return ApiResponse.<VietQrDto>builder()
                .result(vietQrService.generateInvoiceQr(invoiceId))
                .build();
    }
}
