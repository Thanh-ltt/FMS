package com.FMS.services;

import com.FMS.dto.DepositDto;
import com.FMS.dto.DepositRefundDto;
import com.FMS.dto.DepositSummaryDto;
import com.FMS.dto.request.DepositCreationRequest;
import com.FMS.dto.request.DepositRefundRequest;
import com.FMS.entity.Invoice;

import java.util.List;

public interface DepositService {
    DepositDto create(DepositCreationRequest request);

    DepositDto getById(String id);

    List<DepositDto> getAll();

    List<DepositDto> getByCustomer(String customerId);

    List<DepositDto> getByContract(String contractId);

    List<DepositRefundDto> getRefunds(String depositId);

    DepositDto refund(String depositId, DepositRefundRequest request);

    void delete(String id);

    DepositSummaryDto getSummaryForTrip(String tripId);

    double allocateToInvoice(Invoice invoice, Double requestedAmount);

    void releaseInvoiceAllocations(Invoice invoice);
}
