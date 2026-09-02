package com.FMS.services.impl;

import com.FMS.dto.InvoiceDto;
import com.FMS.dto.InvoicePaymentDto;
import com.FMS.dto.request.InvoiceCreationRequest;
import com.FMS.dto.request.InvoicePaymentRequest;
import com.FMS.entity.Invoice;
import com.FMS.entity.InvoicePayment;
import com.FMS.entity.Trip;
import com.FMS.enums.InvoiceStatus;
import com.FMS.enums.PaymentMethod;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.InvoiceMapper;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.InvoicePaymentRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.services.InvoiceService;
import com.FMS.services.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    private final TripRepository tripRepository;

    private final InvoiceMapper invoiceMapper;

    private final DepositService depositService;

    private final InvoicePaymentRepository invoicePaymentRepository;

    @Override
    @Transactional
    public InvoiceDto createInvoice(InvoiceCreationRequest request) {
        validateInvoiceDates(request);
        Trip trip = tripRepository.findById(request.getTripId()).orElseThrow(() ->
                new AppException(ErrorCode.TRIP_NOT_FOUND));

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new AppException(ErrorCode.TRIP_NOT_COMPLETED);
        }

        boolean hasActiveInvoice = invoiceRepository.findByTrip_Id(trip.getId())
                .stream()
                .anyMatch(invoice -> invoice.getStatus() != InvoiceStatus.CANCELLED);

        if (hasActiveInvoice) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_EXISTS);
        }

        if (trip.getCustomer() == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(resolveInvoiceNumber(request, trip))
                .customer(trip.getCustomer())
                .trip(trip)
                .totalAmount(resolveTotalAmount(request, trip))
                .depositAppliedAmount(0D)
                .paidAmount(0D)
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .status(InvoiceStatus.PENDING)
                .build();

        invoice = invoiceRepository.save(invoice);
        if (Boolean.TRUE.equals(request.getApplyDeposit())) {
            depositService.allocateToInvoice(invoice, request.getDepositAmount());
            invoice = invoiceRepository.save(invoice);
        }
        return invoiceMapper.toDto(invoice);
    }

    @Override
    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .map(invoiceMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteInvoice(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() ->
                new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.PAID_INVOICE_CANNOT_DELETE);
        }

        if (invoicePaymentRepository.existsByInvoice_Id(invoiceId)) {
            throw new AppException(ErrorCode.INVALID_INVOICE_STATUS);
        }

        depositService.releaseInvoiceAllocations(invoice);
        invoiceRepository.delete(invoice);
    }

    @Override
    @Transactional
    public InvoiceDto markAsPaid(String invoiceId, InvoicePaymentRequest request) {

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElseThrow(() ->
                        new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVOICE_CANCELLED);
        }

        validatePayment(invoice, request);

        double amountDue = valueOrZero(invoice.getAmountDue());
        if (amountDue <= 0) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }

        InvoicePayment payment = InvoicePayment.builder()
                .invoice(invoice)
                .amount(amountDue)
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .bankName(trimToNull(request.getBankName()))
                .accountHolder(trimToNull(request.getAccountHolder()))
                .accountNumber(trimToNull(request.getAccountNumber()))
                .transactionReference(trimToNull(request.getTransactionReference()))
                .note(trimToNull(request.getNote()))
                .build();

        invoicePaymentRepository.save(payment);

        invoice.setPaidAmount(invoice.getTotalAmount());
        invoice.setStatus(InvoiceStatus.PAID);

        return invoiceMapper.toDto(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoicePaymentDto> getPayments(String invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new AppException(ErrorCode.INVOICE_NOT_FOUND);
        }

        return invoicePaymentRepository.findByInvoice_IdOrderByPaymentDateDescCreatedAtDesc(invoiceId)
                .stream()
                .map(this::toPaymentDto)
                .toList();
    }

    @Override
    @Transactional
    public InvoiceDto cancelInvoice(String invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() ->
                        new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_INVOICE_STATUS);
        }

        if (invoicePaymentRepository.existsByInvoice_Id(invoiceId)) {
            throw new AppException(ErrorCode.INVALID_INVOICE_STATUS);
        }

        depositService.releaseInvoiceAllocations(invoice);
        invoice.setStatus(InvoiceStatus.CANCELLED);

        return invoiceMapper.toDto(invoiceRepository.save(invoice));
    }

    @Override
    public List<InvoiceDto> findByStatus(InvoiceStatus status) {

        return invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getEffectiveStatus() == status)
                .map(invoiceMapper::toDto)
                .toList();
    }

    @Override
    public Double calculateRevenue() {

        return invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.CANCELLED)
                .mapToDouble(this::receivedAmount)
                .sum();
    }

    @Override
    public Double calculateOutstandingDebt() {

        return invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.PENDING
                        || invoice.getStatus() == InvoiceStatus.OVERDUE)
                .mapToDouble(Invoice::getAmountDue)
                .sum();
    }

    @Override
    public List<InvoiceDto> findOverdueInvoices() {

        return invoiceRepository
                .findByDueDateBefore(LocalDate.now())
                .stream()
                .filter(invoice -> invoice.getEffectiveStatus() == InvoiceStatus.OVERDUE)
                .map(invoiceMapper::toDto)
                .toList();
    }

    @Override
    public Double calculateRevenueByCustomer(String customerId) {

        return invoiceRepository
                .findByCustomerId(customerId)
                .stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.CANCELLED)
                .mapToDouble(this::receivedAmount)
                .sum();
    }

    @Override
    public Double calculateRevenueByMonth(int month, int year) {

        return invoiceRepository.findAll()
                .stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.CANCELLED)
                .filter(invoice -> invoice.getIssueDate().getMonthValue() == month)
                .filter(invoice -> invoice.getIssueDate().getYear() == year)
                .mapToDouble(this::receivedAmount)
                .sum();
    }

    private String resolveInvoiceNumber(InvoiceCreationRequest request, Trip trip) {
        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isBlank()) {
            return request.getInvoiceNumber().trim();
        }

        String issueDate = request.getIssueDate() != null
                ? request.getIssueDate().format(DateTimeFormatter.BASIC_ISO_DATE)
                : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        String tripCode = trip.getId().substring(0, Math.min(8, trip.getId().length())).toUpperCase();
        String baseNumber = "INV-" + issueDate + "-" + tripCode;
        if (!invoiceRepository.existsByInvoiceNumber(baseNumber)) {
            return baseNumber;
        }

        int attempt = 2;
        String invoiceNumber = baseNumber + "-R" + attempt;
        while (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            attempt++;
            invoiceNumber = baseNumber + "-R" + attempt;
        }

        return invoiceNumber;
    }

    private Double resolveTotalAmount(InvoiceCreationRequest request, Trip trip) {
        Double totalAmount = request.getTotalAmount();

        if (totalAmount == null || totalAmount <= 0) {
            totalAmount = trip.getFreightAmount();
        }

        if (totalAmount == null || totalAmount <= 0) {
            throw new AppException(ErrorCode.INVALID_INVOICE_AMOUNT);
        }

        return totalAmount;
    }

    private void validateInvoiceDates(InvoiceCreationRequest request) {
        if (request.getIssueDate() == null
                || request.getDueDate() == null
                || request.getIssueDate().isAfter(LocalDate.now())
                || request.getDueDate().isBefore(request.getIssueDate())) {
            throw new AppException(ErrorCode.INVALID_INVOICE_DATE);
        }
    }

    private double valueOrZero(Double value) {
        return value == null ? 0D : value;
    }

    private double receivedAmount(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.PAID
                ? valueOrZero(invoice.getTotalAmount())
                : valueOrZero(invoice.getPaidAmount());
    }

    private void validatePayment(Invoice invoice, InvoicePaymentRequest request) {
        if (request == null || request.getPaymentMethod() == null) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        LocalDate paymentDate = request.getPaymentDate();
        if (paymentDate == null
                || paymentDate.isAfter(LocalDate.now())
                || (invoice.getIssueDate() != null && paymentDate.isBefore(invoice.getIssueDate()))) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_DATE);
        }

        if (request.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                && (trimToNull(request.getBankName()) == null
                || trimToNull(request.getTransactionReference()) == null)) {
            throw new AppException(ErrorCode.BANK_TRANSFER_DETAILS_REQUIRED);
        }
    }

    private InvoicePaymentDto toPaymentDto(InvoicePayment payment) {
        return InvoicePaymentDto.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .bankName(payment.getBankName())
                .accountHolder(payment.getAccountHolder())
                .accountNumber(payment.getAccountNumber())
                .transactionReference(payment.getTransactionReference())
                .note(payment.getNote())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
