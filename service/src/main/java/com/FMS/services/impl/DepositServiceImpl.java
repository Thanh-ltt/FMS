package com.FMS.services.impl;

import com.FMS.dto.DepositDto;
import com.FMS.dto.DepositRefundDto;
import com.FMS.dto.DepositSummaryDto;
import com.FMS.dto.request.DepositCreationRequest;
import com.FMS.dto.request.DepositRefundRequest;
import com.FMS.entity.*;
import com.FMS.enums.*;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.repositories.*;
import com.FMS.services.DepositService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositServiceImpl implements DepositService {
    static final double EPSILON = 0.000001D;

    DepositRepository depositRepository;
    DepositRefundRepository depositRefundRepository;
    InvoiceDepositAllocationRepository allocationRepository;
    CustomerRepository customerRepository;
    ContractRepository contractRepository;
    TripRepository tripRepository;

    @Override
    @Transactional
    public DepositDto create(DepositCreationRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }
        if (request.getReceivedDate() == null || request.getReceivedDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_DATE);
        }
        if (request.getPaymentMethod() == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        validateTransferDetails(
                request.getPaymentMethod(),
                request.getBankName(),
                request.getReferenceNumber()
        );
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        Contract contract = findContract(request.getContractId());
        Trip trip = findTrip(request.getTripId());
        validateAndResolveTarget(customer, contract, trip);

        if (trip != null && contract == null) {
            contract = trip.getContract();
        }

        String receiptNumber = resolveReceiptNumber(request.getReceiptNumber());
        Deposit deposit = Deposit.builder()
                .receiptNumber(receiptNumber)
                .customer(customer)
                .contract(contract)
                .trip(trip)
                .amount(request.getAmount())
                .allocatedAmount(0D)
                .refundedAmount(0D)
                .receivedDate(request.getReceivedDate())
                .paymentMethod(request.getPaymentMethod())
                .bankName(trimToNull(request.getBankName()))
                .accountHolder(trimToNull(request.getAccountHolder()))
                .accountNumber(trimToNull(request.getAccountNumber()))
                .referenceNumber(trimToNull(request.getReferenceNumber()))
                .note(trimToNull(request.getNote()))
                .status(DepositStatus.AVAILABLE)
                .build();

        return toDto(depositRepository.save(deposit));
    }

    @Override
    public DepositDto getById(String id) {
        return toDto(findDeposit(id));
    }

    @Override
    public List<DepositDto> getAll() {
        return depositRepository.findAllByOrderByReceivedDateDescCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<DepositDto> getByCustomer(String customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        return depositRepository.findByCustomerIdOrderByReceivedDateAscCreatedAtAsc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<DepositDto> getByContract(String contractId) {
        if (!contractRepository.existsById(contractId)) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        return depositRepository.findByContractIdOrderByReceivedDateDescCreatedAtDesc(contractId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<DepositRefundDto> getRefunds(String depositId) {
        findDeposit(depositId);
        return depositRefundRepository.findByDepositIdOrderByRefundDateDescCreatedAtDesc(depositId)
                .stream()
                .map(this::toRefundDto)
                .toList();
    }

    @Override
    @Transactional
    public DepositDto refund(String depositId, DepositRefundRequest request) {
        Deposit deposit = depositRepository.findByIdForUpdate(depositId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPOSIT_NOT_FOUND));

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }
        if (request.getAmount() - deposit.getAvailableAmount() > EPSILON) {
            throw new AppException(ErrorCode.DEPOSIT_REFUND_EXCEEDS_AVAILABLE);
        }
        if (request.getRefundDate() == null
                || request.getRefundDate().isBefore(deposit.getReceivedDate())
                || request.getRefundDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_DATE);
        }
        if (request.getPaymentMethod() == null) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }
        validateTransferDetails(
                request.getPaymentMethod(),
                request.getBankName(),
                request.getReferenceNumber()
        );

        DepositRefund refund = DepositRefund.builder()
                .deposit(deposit)
                .amount(request.getAmount())
                .refundDate(request.getRefundDate())
                .paymentMethod(request.getPaymentMethod())
                .bankName(trimToNull(request.getBankName()))
                .accountHolder(trimToNull(request.getAccountHolder()))
                .accountNumber(trimToNull(request.getAccountNumber()))
                .referenceNumber(trimToNull(request.getReferenceNumber()))
                .note(trimToNull(request.getNote()))
                .build();

        deposit.setRefundedAmount(valueOrZero(deposit.getRefundedAmount()) + request.getAmount());
        refreshStatus(deposit);
        depositRefundRepository.save(refund);
        return toDto(depositRepository.save(deposit));
    }

    @Override
    @Transactional
    public void delete(String id) {
        Deposit deposit = findDeposit(id);
        if (valueOrZero(deposit.getAllocatedAmount()) > EPSILON
                || valueOrZero(deposit.getRefundedAmount()) > EPSILON
                || allocationRepository.existsByDepositId(id)
                || depositRefundRepository.existsByDepositId(id)) {
            throw new AppException(ErrorCode.DEPOSIT_CANNOT_DELETE);
        }
        depositRepository.delete(deposit);
    }

    @Override
    public DepositSummaryDto getSummaryForTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return buildSummary(trip);
    }

    @Override
    @Transactional
    public double allocateToInvoice(Invoice invoice, Double requestedAmount) {
        Trip trip = invoice.getTrip();
        if (trip == null || trip.getCustomer() == null) {
            throw new AppException(ErrorCode.DEPOSIT_TARGET_MISMATCH);
        }

        Contract contract = trip.getContract();
        if (contract != null && contract.getDepositUsage() == DepositUsage.SECURITY_HOLD) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_POLICY);
        }

        List<Deposit> eligible = depositRepository.findByCustomerIdForUpdate(trip.getCustomer().getId())
                .stream()
                .filter(deposit -> isEligible(deposit, trip))
                .sorted(Comparator
                        .comparingInt((Deposit deposit) -> targetPriority(deposit, trip))
                        .thenComparing(Deposit::getReceivedDate)
                        .thenComparing(Deposit::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        double available = eligible.stream().mapToDouble(Deposit::getAvailableAmount).sum();
        if (available <= EPSILON) {
            if (requestedAmount != null && requestedAmount > EPSILON) {
                throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
            }
            return 0D;
        }

        double totalAmount = valueOrZero(invoice.getTotalAmount());
        double amountToAllocate = requestedAmount == null
                ? Math.min(totalAmount, available)
                : requestedAmount;
        if (amountToAllocate <= 0
                || amountToAllocate - totalAmount > EPSILON
                || amountToAllocate - available > EPSILON) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }

        double remaining = amountToAllocate;
        List<InvoiceDepositAllocation> allocations = new ArrayList<>();
        for (Deposit deposit : eligible) {
            if (remaining <= EPSILON) {
                break;
            }
            double allocationAmount = Math.min(deposit.getAvailableAmount(), remaining);
            if (allocationAmount <= EPSILON) {
                continue;
            }

            deposit.setAllocatedAmount(valueOrZero(deposit.getAllocatedAmount()) + allocationAmount);
            refreshStatus(deposit);
            allocations.add(InvoiceDepositAllocation.builder()
                    .deposit(deposit)
                    .invoice(invoice)
                    .amount(allocationAmount)
                    .build());
            remaining -= allocationAmount;
        }

        if (remaining > EPSILON) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_AMOUNT);
        }

        depositRepository.saveAll(eligible);
        allocationRepository.saveAll(allocations);
        invoice.setDepositAppliedAmount(valueOrZero(invoice.getDepositAppliedAmount()) + amountToAllocate);
        invoice.setPaidAmount(valueOrZero(invoice.getPaidAmount()) + amountToAllocate);
        if (invoice.getAmountDue() <= EPSILON) {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        return amountToAllocate;
    }

    @Override
    @Transactional
    public void releaseInvoiceAllocations(Invoice invoice) {
        if (invoice.getId() == null) {
            return;
        }
        List<InvoiceDepositAllocation> allocations = allocationRepository.findByInvoiceId(invoice.getId());
        if (allocations.isEmpty()) {
            return;
        }

        List<InvoiceDepositAllocation> orderedAllocations = allocations.stream()
                .sorted(Comparator.comparing(allocation -> allocation.getDeposit().getId()))
                .toList();
        for (InvoiceDepositAllocation allocation : orderedAllocations) {
            Deposit deposit = depositRepository.findByIdForUpdate(allocation.getDeposit().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.DEPOSIT_NOT_FOUND));
            deposit.setAllocatedAmount(Math.max(
                    valueOrZero(deposit.getAllocatedAmount()) - valueOrZero(allocation.getAmount()),
                    0D
            ));
            refreshStatus(deposit);
            depositRepository.save(deposit);
        }
        allocationRepository.deleteAll(orderedAllocations);

        double applied = valueOrZero(invoice.getDepositAppliedAmount());
        invoice.setDepositAppliedAmount(0D);
        invoice.setPaidAmount(Math.max(valueOrZero(invoice.getPaidAmount()) - applied, 0D));
    }

    private DepositSummaryDto buildSummary(Trip trip) {
        Contract contract = trip.getContract();
        boolean required = contract != null && Boolean.TRUE.equals(contract.getDepositRequired());
        double requiredAmount = calculateRequiredAmount(contract, trip);

        List<Deposit> relevantDeposits = trip.getCustomer() == null
                ? List.of()
                : depositRepository.findByCustomerIdOrderByReceivedDateAscCreatedAtAsc(trip.getCustomer().getId())
                        .stream()
                        .filter(deposit -> isRelevantToTrip(deposit, trip))
                        .filter(deposit -> deposit.getStatus() != DepositStatus.CANCELLED)
                        .toList();

        double grossReceived = relevantDeposits.stream().mapToDouble(deposit -> valueOrZero(deposit.getAmount())).sum();
        double refunded = relevantDeposits.stream().mapToDouble(deposit -> valueOrZero(deposit.getRefundedAmount())).sum();
        double allocated = allocationRepository.findByInvoice_Trip_Id(trip.getId())
                .stream()
                .mapToDouble(allocation -> valueOrZero(allocation.getAmount()))
                .sum();
        double available = relevantDeposits.stream().mapToDouble(Deposit::getAvailableAmount).sum();
        double netReceived = Math.max(grossReceived - refunded, 0D);
        double coverage = contract != null && contract.getDepositScope() == DepositScope.CONTRACT
                ? netReceived
                : available + allocated;

        return DepositSummaryDto.builder()
                .required(required)
                .scope(contract == null ? null : contract.getDepositScope())
                .type(contract == null ? null : contract.getDepositType())
                .usage(contract == null ? null : contract.getDepositUsage())
                .policyValue(contract == null ? null : contract.getDepositValue())
                .requiredAmount(requiredAmount)
                .receivedAmount(netReceived)
                .allocatedAmount(allocated)
                .refundedAmount(refunded)
                .availableAmount(available)
                .shortfallAmount(Math.max(requiredAmount - coverage, 0D))
                .build();
    }

    private double calculateRequiredAmount(Contract contract, Trip trip) {
        if (contract == null || !Boolean.TRUE.equals(contract.getDepositRequired())
                || contract.getDepositValue() == null) {
            return 0D;
        }
        if (contract.getDepositType() == DepositType.FIXED) {
            return contract.getDepositValue();
        }
        Double baseAmount = contract.getDepositScope() == DepositScope.CONTRACT
                ? contract.getContractValue()
                : trip.getFreightAmount();
        return baseAmount == null ? 0D : baseAmount * contract.getDepositValue() / 100D;
    }

    private boolean isEligible(Deposit deposit, Trip trip) {
        return deposit.getStatus() != DepositStatus.CANCELLED
                && deposit.getStatus() != DepositStatus.REFUNDED
                && deposit.getAvailableAmount() > EPSILON
                && isRelevantToTrip(deposit, trip);
    }

    private boolean isRelevantToTrip(Deposit deposit, Trip trip) {
        if (deposit.getTrip() != null) {
            return deposit.getTrip().getId().equals(trip.getId());
        }
        if (deposit.getContract() != null) {
            return trip.getContract() != null
                    && deposit.getContract().getId().equals(trip.getContract().getId());
        }
        return deposit.getCustomer() != null
                && trip.getCustomer() != null
                && deposit.getCustomer().getId().equals(trip.getCustomer().getId());
    }

    private int targetPriority(Deposit deposit, Trip trip) {
        if (deposit.getTrip() != null && deposit.getTrip().getId().equals(trip.getId())) {
            return 0;
        }
        if (deposit.getContract() != null) {
            return 1;
        }
        return 2;
    }

    private void validateAndResolveTarget(Customer customer, Contract contract, Trip trip) {
        if (contract != null && (contract.getCustomer() == null
                || !contract.getCustomer().getId().equals(customer.getId()))) {
            throw new AppException(ErrorCode.DEPOSIT_TARGET_MISMATCH);
        }
        if (trip != null && (trip.getCustomer() == null
                || !trip.getCustomer().getId().equals(customer.getId()))) {
            throw new AppException(ErrorCode.DEPOSIT_TARGET_MISMATCH);
        }
        if (trip != null && contract != null && (trip.getContract() == null
                || !trip.getContract().getId().equals(contract.getId()))) {
            throw new AppException(ErrorCode.DEPOSIT_TARGET_MISMATCH);
        }
    }

    private Contract findContract(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return contractRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }

    private Trip findTrip(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return tripRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
    }

    private Deposit findDeposit(String id) {
        return depositRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPOSIT_NOT_FOUND));
    }

    private String resolveReceiptNumber(String requestedNumber) {
        if (requestedNumber != null && !requestedNumber.isBlank()) {
            String receiptNumber = requestedNumber.trim();
            if (depositRepository.existsByReceiptNumber(receiptNumber)) {
                throw new AppException(ErrorCode.DEPOSIT_RECEIPT_ALREADY_EXISTS);
            }
            return receiptNumber;
        }

        String prefix = "PC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String generated;
        do {
            generated = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (depositRepository.existsByReceiptNumber(generated));
        return generated;
    }

    private void refreshStatus(Deposit deposit) {
        double allocated = valueOrZero(deposit.getAllocatedAmount());
        double refunded = valueOrZero(deposit.getRefundedAmount());
        double amount = valueOrZero(deposit.getAmount());
        double available = deposit.getAvailableAmount();

        if (refunded >= amount - EPSILON && allocated <= EPSILON) {
            deposit.setStatus(DepositStatus.REFUNDED);
        } else if (refunded > EPSILON) {
            deposit.setStatus(DepositStatus.PARTIALLY_REFUNDED);
        } else if (available <= EPSILON && allocated > EPSILON) {
            deposit.setStatus(DepositStatus.APPLIED);
        } else if (allocated > EPSILON) {
            deposit.setStatus(DepositStatus.PARTIALLY_APPLIED);
        } else {
            deposit.setStatus(DepositStatus.AVAILABLE);
        }
    }

    private DepositDto toDto(Deposit deposit) {
        Customer customer = deposit.getCustomer();
        return DepositDto.builder()
                .id(deposit.getId())
                .receiptNumber(deposit.getReceiptNumber())
                .customerId(customer == null ? null : customer.getId())
                .customerName(customer == null ? null : customer.getName())
                .customerUsername(customer == null || customer.getUser() == null
                        ? null : customer.getUser().getUsername())
                .contractId(deposit.getContract() == null ? null : deposit.getContract().getId())
                .contractCode(deposit.getContract() == null ? null : deposit.getContract().getContractCode())
                .tripId(deposit.getTrip() == null ? null : deposit.getTrip().getId())
                .amount(deposit.getAmount())
                .allocatedAmount(valueOrZero(deposit.getAllocatedAmount()))
                .refundedAmount(valueOrZero(deposit.getRefundedAmount()))
                .availableAmount(deposit.getAvailableAmount())
                .receivedDate(deposit.getReceivedDate())
                .paymentMethod(deposit.getPaymentMethod())
                .bankName(deposit.getBankName())
                .accountHolder(deposit.getAccountHolder())
                .accountNumber(deposit.getAccountNumber())
                .referenceNumber(deposit.getReferenceNumber())
                .note(deposit.getNote())
                .status(deposit.getStatus())
                .build();
    }

    private DepositRefundDto toRefundDto(DepositRefund refund) {
        return DepositRefundDto.builder()
                .id(refund.getId())
                .depositId(refund.getDeposit().getId())
                .amount(refund.getAmount())
                .refundDate(refund.getRefundDate())
                .paymentMethod(refund.getPaymentMethod())
                .bankName(refund.getBankName())
                .accountHolder(refund.getAccountHolder())
                .accountNumber(refund.getAccountNumber())
                .referenceNumber(refund.getReferenceNumber())
                .note(refund.getNote())
                .build();
    }

    private double valueOrZero(Double value) {
        return value == null ? 0D : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateTransferDetails(PaymentMethod paymentMethod, String bankName, String referenceNumber) {
        if (paymentMethod == PaymentMethod.BANK_TRANSFER
                && (trimToNull(bankName) == null || trimToNull(referenceNumber) == null)) {
            throw new AppException(ErrorCode.BANK_TRANSFER_DETAILS_REQUIRED);
        }
    }
}
