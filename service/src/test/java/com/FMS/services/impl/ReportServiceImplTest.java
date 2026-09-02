package com.FMS.services.impl;

import com.FMS.dto.FinancialReportDto;
import com.FMS.entity.Customer;
import com.FMS.entity.Expense;
import com.FMS.entity.Invoice;
import com.FMS.entity.Maintenance;
import com.FMS.entity.Trip;
import com.FMS.enums.ExpenseType;
import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.InvoiceStatus;
import com.FMS.enums.MaintenanceStatus;
import com.FMS.enums.TripStatus;
import com.FMS.repositories.ExpenseRepository;
import com.FMS.repositories.DepositRepository;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.MaintenanceRepository;
import com.FMS.repositories.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.FMS.repositories.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock
    private TripRepository tripRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private MaintenanceRepository maintenanceRepository;
    @Mock
    private DepositRepository depositRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    private ReportServiceImpl reportService;
    private Customer customer;
    private Trip juneTrip;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                tripRepository,
                invoiceRepository,
                expenseRepository,
                maintenanceRepository,
                depositRepository,
                vehicleRepository
        );

        customer = Customer.builder()
                .id("customer-1")
                .name("Cong ty An Phat")
                .build();

        juneTrip = Trip.builder()
                .id("trip-june-001")
                .customer(customer)
                .startTime("2026-06-10T08:00")
                .endTime("2026-06-10T11:00")
                .distanceKm(120.0)
                .cargoWeightTon(5.0)
                .freightAmount(1_000_000.0)
                .status(TripStatus.COMPLETED)
                .build();
    }

    @Test
    void getFinancialReport_filtersPeriodAndExcludesCancelledInvoices() {
        Trip julyTrip = Trip.builder()
                .id("trip-july-001")
                .customer(customer)
                .startTime("2026-07-02T08:00")
                .status(TripStatus.COMPLETED)
                .build();

        Invoice paidInvoice = invoice(juneTrip, 1_000_000.0, InvoiceStatus.PAID);
        Invoice cancelledInvoice = invoice(juneTrip, 500_000.0, InvoiceStatus.CANCELLED);
        Invoice julyInvoice = invoice(julyTrip, 2_000_000.0, InvoiceStatus.PENDING);

        Expense fuel = Expense.builder()
                .trip(juneTrip)
                .expenseType(ExpenseType.FUEL)
                .amount(300_000.0)
                .expenseDate(LocalDate.of(2026, 6, 10))
                .status(ExpenseStatus.APPROVED)
                .build();
        Expense pendingToll = Expense.builder()
                .trip(juneTrip)
                .expenseType(ExpenseType.TOLL)
                .amount(200_000.0)
                .expenseDate(LocalDate.of(2026, 6, 10))
                .status(ExpenseStatus.PENDING)
                .build();

        Maintenance maintenance = Maintenance.builder()
                .cost(100_000.0)
                .maintenanceDate(LocalDate.of(2026, 6, 12))
                .status(MaintenanceStatus.COMPLETED)
                .build();

        when(tripRepository.findAll()).thenReturn(List.of(juneTrip, julyTrip));
        when(invoiceRepository.findAll()).thenReturn(List.of(paidInvoice, cancelledInvoice, julyInvoice));
        when(expenseRepository.findAll()).thenReturn(List.of(fuel, pendingToll));
        when(maintenanceRepository.findAll()).thenReturn(List.of(maintenance));

        FinancialReportDto result = reportService.getFinancialReport(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null,
                null
        );

        assertThat(result.getTrips()).hasSize(1);
        assertThat(result.getTotals().getRecognizedRevenue()).isEqualTo(1_000_000.0);
        assertThat(result.getTotals().getPaidRevenue()).isEqualTo(1_000_000.0);
        assertThat(result.getTotals().getTripExpense()).isEqualTo(300_000.0);
        assertThat(result.getTotals().getMaintenanceExpense()).isEqualTo(100_000.0);
        assertThat(result.getTotals().getNetProfit()).isEqualTo(600_000.0);
        assertThat(result.getMonthly()).singleElement()
                .extracting(FinancialReportDto.MonthlyRow::getPeriod)
                .isEqualTo("2026-06");
    }

    @Test
    void getFinancialReport_doesNotAllocateMaintenanceToCustomer() {
        Invoice invoice = invoice(juneTrip, 1_000_000.0, InvoiceStatus.PENDING);
        Expense expense = Expense.builder()
                .trip(juneTrip)
                .amount(300_000.0)
                .status(ExpenseStatus.APPROVED)
                .build();

        when(tripRepository.findAll()).thenReturn(List.of(juneTrip));
        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(expenseRepository.findAll()).thenReturn(List.of(expense));

        FinancialReportDto result = reportService.getFinancialReport(
                null,
                null,
                customer.getId(),
                null
        );

        assertThat(result.isMaintenanceIncluded()).isFalse();
        assertThat(result.getTotals().getOutstanding()).isEqualTo(1_000_000.0);
        assertThat(result.getTotals().getMaintenanceExpense()).isZero();
        assertThat(result.getTotals().getNetProfit()).isEqualTo(700_000.0);
    }

    private Invoice invoice(Trip trip, double amount, InvoiceStatus status) {
        return Invoice.builder()
                .trip(trip)
                .customer(customer)
                .totalAmount(amount)
                .status(status)
                .build();
    }
}
