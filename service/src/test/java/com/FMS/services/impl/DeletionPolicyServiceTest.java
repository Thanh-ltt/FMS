package com.FMS.services.impl;

import com.FMS.entity.Contract;
import com.FMS.entity.Invoice;
import com.FMS.entity.Trip;
import com.FMS.entity.Vehicle;
import com.FMS.enums.InvoiceStatus;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.ContractMapper;
import com.FMS.mapper.ExpenseMapper;
import com.FMS.mapper.InvoiceMapper;
import com.FMS.mapper.TripMapper;
import com.FMS.repositories.CargoRateRepository;
import com.FMS.repositories.ContractRepository;
import com.FMS.repositories.CustomerRepository;
import com.FMS.repositories.DriverRepository;
import com.FMS.repositories.DepositRepository;
import com.FMS.repositories.ExpenseRepository;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.InvoicePaymentRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.VehicleRepository;
import com.FMS.services.DepositService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletionPolicyServiceTest {
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CargoRateRepository cargoRateRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private DepositRepository depositRepository;
    @Mock
    private ContractMapper contractMapper;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private TripMapper tripMapper;
    @Mock
    private DepositService depositService;
    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;

    @Test
    void deleteContract_rejectsContractUsedByTrip() {
        Contract contract = Contract.builder().id("contract-1").build();
        when(contractRepository.findById("contract-1")).thenReturn(Optional.of(contract));
        when(tripRepository.existsByContractId("contract-1")).thenReturn(true);

        ContractServiceImpl service = new ContractServiceImpl(
                contractRepository,
                customerRepository,
                cargoRateRepository,
                tripRepository,
                depositRepository,
                contractMapper
        );

        assertThatThrownBy(() -> service.delete("contract-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);
        verify(contractRepository, never()).delete(any(Contract.class));
    }

    @Test
    void deleteInvoice_rejectsPaidInvoice() {
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .status(InvoiceStatus.PAID)
                .build();
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));

        InvoiceServiceImpl service = new InvoiceServiceImpl(
                invoiceRepository,
                tripRepository,
                invoiceMapper,
                depositService,
                invoicePaymentRepository
        );

        assertThatThrownBy(() -> service.deleteInvoice("invoice-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAID_INVOICE_CANNOT_DELETE);
        verify(invoiceRepository, never()).delete(any(Invoice.class));
    }

    @Test
    void deleteTrip_rejectsTripWithFinancialData() {
        Trip trip = Trip.builder().id("trip-1").status(TripStatus.COMPLETED).build();
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(invoiceRepository.existsByTrip_Id("trip-1")).thenReturn(true);

        TripServiceImpl service = tripService();

        assertThatThrownBy(() -> service.deleteTrip("trip-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);
        verify(tripRepository, never()).delete(any(Trip.class));
    }

    @Test
    void deleteTrip_releasesVehicleWhenTripIsActive() {
        Vehicle vehicle = Vehicle.builder()
                .id("vehicle-1")
                .status(VehicleStatus.IN_TRIP)
                .build();
        Trip trip = Trip.builder()
                .id("trip-1")
                .vehicle(vehicle)
                .status(TripStatus.IN_PROGRESS)
                .build();
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(invoiceRepository.existsByTrip_Id("trip-1")).thenReturn(false);
        when(expenseRepository.existsByTripId("trip-1")).thenReturn(false);

        tripService().deleteTrip("trip-1");

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepository).save(vehicle);
        verify(tripRepository).delete(trip);
    }

    private TripServiceImpl tripService() {
        return new TripServiceImpl(
                tripRepository,
                driverRepository,
                vehicleRepository,
                customerRepository,
                contractRepository,
                invoiceRepository,
                expenseRepository,
                depositRepository,
                invoiceMapper,
                expenseMapper,
                tripMapper,
                depositService
        );
    }
}
