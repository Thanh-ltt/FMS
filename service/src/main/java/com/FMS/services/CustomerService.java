package com.FMS.services;

import com.FMS.dto.ContractDto;
import com.FMS.dto.CustomerDto;
import com.FMS.dto.CustomerPortalDto;
import com.FMS.dto.InvoiceDto;
import com.FMS.dto.request.CustomerAccountCreationRequest;
import com.FMS.dto.request.CustomerAccountLinkRequest;
import com.FMS.dto.request.CustomerProfileRequest;

import java.util.List;

public interface CustomerService {
    CustomerDto create(CustomerProfileRequest request);

    CustomerDto createWithNewAccount(CustomerAccountCreationRequest request);

    CustomerDto createWithExistingAccount(CustomerAccountLinkRequest request);

    CustomerDto update(String id, CustomerProfileRequest request);

    CustomerDto getById(String id);

    List<CustomerDto> getAll();

    void delete(String id);

    List<ContractDto> getContracts(String customerId);

    List<InvoiceDto> getInvoices(String customerId);

    CustomerPortalDto getPortalByUsername(String username);
}
