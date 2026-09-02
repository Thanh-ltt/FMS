package com.FMS.services.impl;

import com.FMS.dto.InvoiceDto;
import com.FMS.dto.InvoicePaymentDto;
import com.FMS.dto.request.InvoiceCreationRequest;
import com.FMS.dto.request.InvoicePaymentRequest;
import com.FMS.entity.Customer;
import com.FMS.entity.Invoice;
import com.FMS.entity.InvoicePayment;
import com.FMS.entity.Trip;
import com.FMS.enums.InvoiceStatus;
import com.FMS.enums.PaymentMethod;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.InvoiceMapper;
import com.FMS.repositories.InvoicePaymentRepository;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.services.DepositService;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private DepositService depositService;
    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;

    private InvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(
                invoiceRepository,
                tripRepository,
                invoiceMapper,
                depositService,
                invoicePaymentRepository
        );
    }

    @Test
    void createInvoice_allocatesRequestedDepositForCompletedTrip() {
        Customer customer = Customer.builder().id("customer-1").build();
        Trip trip = Trip.builder()
                .id("trip-1")
                .customer(customer)
                .status(TripStatus.COMPLETED)
                .freightAmount(500D)
                .build();
        InvoiceCreationRequest request = InvoiceCreationRequest.builder()
                .tripId("trip-1")
                .totalAmount(500D)
                .issueDate(LocalDate.of(2026, 7, 14))
                .dueDate(LocalDate.of(2026, 7, 20))
                .applyDeposit(true)
                .depositAmount(150D)
                .build();

        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(invoiceRepository.findByTrip_Id("trip-1")).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId("invoice-1");
            return invoice;
        });
        when(invoiceMapper.toDto(any(Invoice.class))).thenReturn(InvoiceDto.builder().id("invoice-1").build());

        InvoiceDto result = service.createInvoice(request);

        assertThat(result.getId()).isEqualTo("invoice-1");
        verify(depositService).allocateToInvoice(any(Invoice.class), org.mockito.ArgumentMatchers.eq(150D));
    }

    @Test
    void cancelInvoice_releasesDepositAllocationBeforeCancelling() {
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .totalAmount(500D)
                .depositAppliedAmount(150D)
                .paidAmount(150D)
                .issueDate(LocalDate.now().minusDays(1))
                .status(InvoiceStatus.PENDING)
                .build();
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toDto(invoice)).thenReturn(InvoiceDto.builder().id("invoice-1").build());

        service.cancelInvoice("invoice-1");

        verify(depositService).releaseInvoiceAllocations(invoice);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void markAsPaid_setsPaidAmountToFullInvoiceValue() {
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .totalAmount(500D)
                .depositAppliedAmount(150D)
                .paidAmount(150D)
                .status(InvoiceStatus.PENDING)
                .build();
        when(invoiceRepository.findByIdForUpdate("invoice-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toDto(invoice)).thenReturn(InvoiceDto.builder().id("invoice-1").build());

        service.markAsPaid("invoice-1", InvoicePaymentRequest.builder()
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .bankName("Vietcombank")
                .transactionReference("FT261950001")
                .build());

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getPaidAmount()).isEqualTo(500D);
        assertThat(invoice.getAmountDue()).isZero();
        verify(invoicePaymentRepository).save(argThat((InvoicePayment payment) ->
                payment.getAmount().equals(350D)
                        && payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                        && "Vietcombank".equals(payment.getBankName())
                        && "FT261950001".equals(payment.getTransactionReference())));
    }

    @Test
    void markAsPaid_rejectsBankTransferWithoutReconciliationDetails() {
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .totalAmount(500D)
                .paidAmount(0D)
                .issueDate(LocalDate.now().minusDays(1))
                .status(InvoiceStatus.PENDING)
                .build();
        when(invoiceRepository.findByIdForUpdate("invoice-1")).thenReturn(Optional.of(invoice));

        InvoicePaymentRequest request = InvoicePaymentRequest.builder()
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        assertThatThrownBy(() -> service.markAsPaid("invoice-1", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BANK_TRANSFER_DETAILS_REQUIRED);
    }

    @Test
    void getPayments_returnsReconciliationDetails() {
        Invoice invoice = Invoice.builder().id("invoice-1").build();
        InvoicePayment payment = InvoicePayment.builder()
                .id("payment-1")
                .invoice(invoice)
                .amount(350D)
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .bankName("Vietcombank")
                .accountHolder("NGUYEN VAN A")
                .transactionReference("FT261950001")
                .build();
        when(invoiceRepository.existsById("invoice-1")).thenReturn(true);
        when(invoicePaymentRepository.findByInvoice_IdOrderByPaymentDateDescCreatedAtDesc("invoice-1"))
                .thenReturn(List.of(payment));

        List<InvoicePaymentDto> result = service.getPayments("invoice-1");

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getAmount()).isEqualTo(350D);
            assertThat(item.getBankName()).isEqualTo("Vietcombank");
            assertThat(item.getTransactionReference()).isEqualTo("FT261950001");
        });
    }

    @Test
    void findByStatus_treatsPastDuePendingInvoiceAsOverdue() {
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .totalAmount(500D)
                .paidAmount(0D)
                .dueDate(LocalDate.now().minusDays(1))
                .status(InvoiceStatus.PENDING)
                .build();
        InvoiceDto dto = InvoiceDto.builder().id("invoice-1").status(InvoiceStatus.OVERDUE).build();
        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(invoiceMapper.toDto(invoice)).thenReturn(dto);

        List<InvoiceDto> result = service.findByStatus(InvoiceStatus.OVERDUE);

        assertThat(result).containsExactly(dto);
        assertThat(invoice.getEffectiveStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }
}
