package com.FMS.services.impl;

import com.FMS.dto.request.DepositCreationRequest;
import com.FMS.dto.request.DepositRefundRequest;
import com.FMS.entity.*;
import com.FMS.enums.*;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositServiceImplTest {
    @Mock
    private DepositRepository depositRepository;
    @Mock
    private DepositRefundRepository refundRepository;
    @Mock
    private InvoiceDepositAllocationRepository allocationRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private TripRepository tripRepository;

    private DepositServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DepositServiceImpl(
                depositRepository,
                refundRepository,
                allocationRepository,
                customerRepository,
                contractRepository,
                tripRepository
        );
    }

    @Test
    void allocateToInvoice_usesTripThenContractBeforeGeneralBalance() {
        Customer customer = Customer.builder().id("customer-1").build();
        Contract contract = Contract.builder()
                .id("contract-1")
                .customer(customer)
                .depositUsage(DepositUsage.APPLY_TO_INVOICE)
                .build();
        Trip trip = Trip.builder()
                .id("trip-1")
                .customer(customer)
                .contract(contract)
                .build();
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .trip(trip)
                .customer(customer)
                .totalAmount(200D)
                .paidAmount(0D)
                .depositAppliedAmount(0D)
                .status(InvoiceStatus.PENDING)
                .build();

        Deposit general = deposit("general", customer, null, null, 100D);
        Deposit contractDeposit = deposit("contract", customer, contract, null, 100D);
        Deposit tripDeposit = deposit("trip", customer, contract, trip, 50D);
        when(depositRepository.findByCustomerIdForUpdate("customer-1"))
                .thenReturn(List.of(general, contractDeposit, tripDeposit));

        double allocated = service.allocateToInvoice(invoice, 80D);

        assertThat(allocated).isEqualTo(80D);
        assertThat(tripDeposit.getAllocatedAmount()).isEqualTo(50D);
        assertThat(contractDeposit.getAllocatedAmount()).isEqualTo(30D);
        assertThat(general.getAllocatedAmount()).isZero();
        assertThat(invoice.getDepositAppliedAmount()).isEqualTo(80D);
        assertThat(invoice.getPaidAmount()).isEqualTo(80D);
        assertThat(invoice.getAmountDue()).isEqualTo(120D);
        verify(allocationRepository).saveAll(any());
    }

    @Test
    void releaseInvoiceAllocations_restoresDepositBalance() {
        Customer customer = Customer.builder().id("customer-1").build();
        Deposit deposit = deposit("deposit-1", customer, null, null, 100D);
        deposit.setAllocatedAmount(60D);
        deposit.setStatus(DepositStatus.PARTIALLY_APPLIED);
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .totalAmount(100D)
                .depositAppliedAmount(60D)
                .paidAmount(60D)
                .status(InvoiceStatus.PENDING)
                .build();
        InvoiceDepositAllocation allocation = InvoiceDepositAllocation.builder()
                .id("allocation-1")
                .invoice(invoice)
                .deposit(deposit)
                .amount(60D)
                .build();
        when(allocationRepository.findByInvoiceId("invoice-1")).thenReturn(List.of(allocation));
        when(depositRepository.findByIdForUpdate("deposit-1")).thenReturn(Optional.of(deposit));

        service.releaseInvoiceAllocations(invoice);

        assertThat(deposit.getAllocatedAmount()).isZero();
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.AVAILABLE);
        assertThat(invoice.getDepositAppliedAmount()).isZero();
        assertThat(invoice.getPaidAmount()).isZero();
        verify(allocationRepository).deleteAll(List.of(allocation));
    }

    @Test
    void refund_rejectsAmountAboveAvailableBalance() {
        Customer customer = Customer.builder().id("customer-1").build();
        Deposit deposit = deposit("deposit-1", customer, null, null, 100D);
        deposit.setAllocatedAmount(70D);
        when(depositRepository.findByIdForUpdate("deposit-1")).thenReturn(Optional.of(deposit));

        DepositRefundRequest request = DepositRefundRequest.builder()
                .amount(31D)
                .refundDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        assertThatThrownBy(() -> service.refund("deposit-1", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPOSIT_REFUND_EXCEEDS_AVAILABLE);
    }

    @Test
    void create_rejectsBankTransferWithoutBankAndReference() {
        DepositCreationRequest request = DepositCreationRequest.builder()
                .customerId("customer-1")
                .amount(100D)
                .receivedDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BANK_TRANSFER_DETAILS_REQUIRED);
    }

    @Test
    void refund_rejectsBankTransferWithoutBankAndReference() {
        Customer customer = Customer.builder().id("customer-1").build();
        Deposit deposit = deposit("deposit-1", customer, null, null, 100D);
        when(depositRepository.findByIdForUpdate("deposit-1")).thenReturn(Optional.of(deposit));

        DepositRefundRequest request = DepositRefundRequest.builder()
                .amount(10D)
                .refundDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        assertThatThrownBy(() -> service.refund("deposit-1", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BANK_TRANSFER_DETAILS_REQUIRED);
    }

    @Test
    void allocateToInvoice_rejectsSecurityHoldPolicy() {
        Customer customer = Customer.builder().id("customer-1").build();
        Contract contract = Contract.builder()
                .id("contract-1")
                .customer(customer)
                .depositUsage(DepositUsage.SECURITY_HOLD)
                .build();
        Trip trip = Trip.builder().id("trip-1").customer(customer).contract(contract).build();
        Invoice invoice = Invoice.builder().id("invoice-1").trip(trip).totalAmount(100D).build();

        assertThatThrownBy(() -> service.allocateToInvoice(invoice, 10D))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DEPOSIT_POLICY);
    }

    @Test
    void getSummaryForTrip_doesNotCountDepositConsumedByAnotherTripAsCoverage() {
        Customer customer = Customer.builder().id("customer-1").build();
        Contract contract = Contract.builder()
                .id("contract-1")
                .customer(customer)
                .depositRequired(true)
                .depositScope(DepositScope.TRIP)
                .depositType(DepositType.FIXED)
                .depositValue(100D)
                .depositUsage(DepositUsage.APPLY_TO_INVOICE)
                .build();
        Trip currentTrip = Trip.builder()
                .id("trip-current")
                .customer(customer)
                .contract(contract)
                .build();
        Deposit contractDeposit = deposit("contract", customer, contract, null, 100D);
        contractDeposit.setAllocatedAmount(100D);
        contractDeposit.setStatus(DepositStatus.APPLIED);

        when(tripRepository.findById("trip-current")).thenReturn(Optional.of(currentTrip));
        when(depositRepository.findByCustomerIdOrderByReceivedDateAscCreatedAtAsc("customer-1"))
                .thenReturn(List.of(contractDeposit));
        when(allocationRepository.findByInvoice_Trip_Id("trip-current")).thenReturn(List.of());

        var summary = service.getSummaryForTrip("trip-current");

        assertThat(summary.getReceivedAmount()).isEqualTo(100D);
        assertThat(summary.getAvailableAmount()).isZero();
        assertThat(summary.getShortfallAmount()).isEqualTo(100D);
    }

    private Deposit deposit(
            String id,
            Customer customer,
            Contract contract,
            Trip trip,
            double amount
    ) {
        return Deposit.builder()
                .id(id)
                .receiptNumber("PC-" + id)
                .customer(customer)
                .contract(contract)
                .trip(trip)
                .amount(amount)
                .allocatedAmount(0D)
                .refundedAmount(0D)
                .receivedDate(LocalDate.of(2026, 7, 1))
                .paymentMethod(PaymentMethod.CASH)
                .status(DepositStatus.AVAILABLE)
                .build();
    }
}
