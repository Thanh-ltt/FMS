package com.FMS.services;

import com.FMS.dto.InvoiceDto;
import com.FMS.dto.InvoicePaymentDto;
import com.FMS.dto.request.InvoiceCreationRequest;
import com.FMS.dto.request.InvoicePaymentRequest;
import com.FMS.enums.InvoiceStatus;

import java.util.List;

public interface InvoiceService {

    InvoiceDto createInvoice(InvoiceCreationRequest request);

    List<InvoiceDto> getAllInvoices();

    void deleteInvoice(String invoiceId);

    InvoiceDto markAsPaid(String invoiceId, InvoicePaymentRequest request);

    List<InvoicePaymentDto> getPayments(String invoiceId);

    InvoiceDto cancelInvoice(String invoiceId);

    List<InvoiceDto> findByStatus(
            InvoiceStatus status);

    Double calculateRevenue();

    Double calculateOutstandingDebt();

    List<InvoiceDto> findOverdueInvoices();

    Double calculateRevenueByCustomer(
            String customerId);

    Double calculateRevenueByMonth(
            int month,
            int year);
}
