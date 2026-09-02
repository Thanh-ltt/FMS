package com.FMS.services.impl;

import com.FMS.dto.DepositSummaryDto;
import com.FMS.dto.TripReadinessCheckDto;
import com.FMS.dto.TripReadinessDto;
import com.FMS.dto.request.TripCreationRequest;
import com.FMS.entity.Contract;
import com.FMS.entity.Customer;
import com.FMS.entity.Driver;
import com.FMS.entity.Trip;
import com.FMS.entity.Vehicle;
import com.FMS.enums.ContractStatus;
import com.FMS.enums.TripReadinessCheckStatus;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.ExpenseMapper;
import com.FMS.mapper.InvoiceMapper;
import com.FMS.mapper.TripMapper;
import com.FMS.repositories.ContractRepository;
import com.FMS.repositories.CustomerRepository;
import com.FMS.repositories.DepositRepository;
import com.FMS.repositories.DriverRepository;
import com.FMS.repositories.ExpenseRepository;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.VehicleRepository;
import com.FMS.services.DepositService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {
    @Mock private TripRepository tripRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private DepositRepository depositRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private ExpenseMapper expenseMapper;
    @Mock private TripMapper tripMapper;
    @Mock private DepositService depositService;

    private TripServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TripServiceImpl(
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

    @Test
    void createTrip_rejectsContractThatIsNotActive() {
        Customer customer = Customer.builder().id("customer-1").build();
        Contract contract = Contract.builder()
                .id("contract-1")
                .customer(customer)
                .status(ContractStatus.DRAFT)
                .build();
        stubCreationEntities(customer, contract);

        assertThatThrownBy(() -> service.createTrip(creationRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONTRACT_NOT_ACTIVE);
    }

    @Test
    void createTrip_rejectsScheduleOutsideContractPeriod() {
        Customer customer = Customer.builder().id("customer-1").build();
        Contract contract = Contract.builder()
                .id("contract-1")
                .customer(customer)
                .status(ContractStatus.ACTIVE)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .build();
        stubCreationEntities(customer, contract);

        assertThatThrownBy(() -> service.createTrip(creationRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONTRACT_OUTSIDE_VALIDITY);
    }

    @Test
    void startTrip_rejectsRequiredDepositShortfall() {
        Trip trip = readyTrip();
        DepositSummaryDto depositSummary = DepositSummaryDto.builder()
                .required(true)
                .requiredAmount(2_000D)
                .receivedAmount(1_500D)
                .shortfallAmount(500D)
                .build();
        stubReadiness(trip, depositSummary, List.of(trip), List.of(trip));

        assertThatThrownBy(() -> service.startTrip("trip-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REQUIRED_DEPOSIT_NOT_RECEIVED);
    }

    @Test
    void getStartReadiness_reportsReadyWhenEveryConditionPasses() {
        Trip trip = readyTrip();
        stubReadiness(trip, noDepositRequired(), List.of(trip), List.of(trip));

        TripReadinessDto result = service.getStartReadiness("trip-1");

        assertThat(result.getReady()).isTrue();
        assertThat(result.getBlockedCount()).isZero();
        assertThat(result.getWaitingCount()).isZero();
        assertThat(result.getPassedCount()).isEqualTo(17);
        assertThat(result.getNotApplicableCount()).isZero();
        assertThat(result.getPrimaryBlockerCode()).isNull();
        assertThat(result.getChecks()).extracting(TripReadinessCheckDto::getKey)
                .containsExactly(
                        "TRIP_STATUS",
                        "TRIP_SCHEDULE",
                        "TRIP_START_TIME",
                        "ROUTE",
                        "FREIGHT",
                        "CUSTOMER",
                        "CONTRACT_STATUS",
                        "CONTRACT_CUSTOMER",
                        "CONTRACT_SCHEDULE",
                        "CONTRACT_CURRENT_DATE",
                        "VEHICLE_STATUS",
                        "VEHICLE_CAPACITY",
                        "VEHICLE_SCHEDULE",
                        "DRIVER_ASSIGNED",
                        "DRIVER_LICENSE",
                        "DRIVER_SCHEDULE",
                        "DEPOSIT"
                );
    }

    @Test
    void getStartReadiness_blocksContractBeforeItsEffectiveDate() {
        Trip trip = readyTrip();
        LocalDate contractStart = LocalDate.now().plusDays(1);
        trip.getContract().setStartDate(contractStart);
        trip.getContract().setEndDate(contractStart.plusDays(30));
        trip.setStartTime(contractStart.atTime(9, 0).toString());
        trip.setEndTime(contractStart.atTime(12, 0).toString());
        stubReadiness(trip, noDepositRequired(), List.of(trip), List.of(trip));

        TripReadinessDto result = service.getStartReadiness("trip-1");

        assertThat(result.getReady()).isFalse();
        assertThat(result.getPrimaryBlockerCode()).isEqualTo(ErrorCode.TRIP_START_TOO_EARLY.name());
        assertThat(check(result, "CONTRACT_CURRENT_DATE").getStatus())
                .isEqualTo(TripReadinessCheckStatus.BLOCKED);
        assertThat(check(result, "CONTRACT_SCHEDULE").getStatus())
                .isEqualTo(TripReadinessCheckStatus.PASSED);
    }

    @Test
    void getStartReadiness_blocksCargoThatExceedsVehicleCapacity() {
        Trip trip = readyTrip();
        trip.setCargoWeightTon(12D);
        trip.setFreightAmount(120_000D);
        stubReadiness(trip, noDepositRequired(), List.of(trip), List.of(trip));

        TripReadinessDto result = service.getStartReadiness("trip-1");

        assertThat(result.getReady()).isFalse();
        assertThat(result.getPrimaryBlockerCode()).isEqualTo(ErrorCode.VEHICLE_CAPACITY_EXCEEDED.name());
        assertThat(check(result, "VEHICLE_CAPACITY").getStatus())
                .isEqualTo(TripReadinessCheckStatus.BLOCKED);
        assertThat(check(result, "VEHICLE_CAPACITY").getResolution()).contains("chọn xe");
    }

    @Test
    void getStartReadiness_blocksDriverLicenseThatExpiresBeforeTripEnds() {
        Trip trip = readyTrip();
        trip.getDriver().setLicenseExpiration(LocalDate.now().minusDays(1));
        stubReadiness(trip, noDepositRequired(), List.of(trip), List.of(trip));

        TripReadinessDto result = service.getStartReadiness("trip-1");

        assertThat(result.getReady()).isFalse();
        assertThat(result.getPrimaryBlockerCode()).isEqualTo(ErrorCode.DRIVER_LICENSE_EXPIRED.name());
        assertThat(check(result, "DRIVER_LICENSE").getStatus())
                .isEqualTo(TripReadinessCheckStatus.BLOCKED);
    }

    @Test
    void getStartReadiness_blocksTripThatIsStartedTooEarly() {
        Trip trip = readyTrip();
        LocalDateTime start = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        trip.setStartTime(start.toString());
        trip.setEndTime(start.plusHours(3).toString());
        stubReadiness(trip, noDepositRequired(), List.of(trip), List.of(trip));

        TripReadinessDto result = service.getStartReadiness("trip-1");

        assertThat(result.getReady()).isFalse();
        assertThat(result.getPrimaryBlockerCode()).isEqualTo(ErrorCode.TRIP_START_TOO_EARLY.name());
        assertThat(check(result, "TRIP_START_TIME").getStatus())
                .isEqualTo(TripReadinessCheckStatus.BLOCKED);
        assertThat(check(result, "TRIP_START_TIME").getResolution()).contains("Đợi đến");
    }

    @Test
    void getStartReadiness_blocksOverlappingVehicleSchedule() {
        Trip trip = readyTrip();
        Trip conflictingTrip = Trip.builder()
                .id("trip-2")
                .vehicle(trip.getVehicle())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .status(TripStatus.ASSIGNED)
                .build();
        stubReadiness(trip, noDepositRequired(), List.of(trip, conflictingTrip), List.of(trip));

        TripReadinessDto result = service.getStartReadiness("trip-1");

        assertThat(result.getReady()).isFalse();
        assertThat(result.getPrimaryBlockerCode()).isEqualTo(ErrorCode.VEHICLE_HAS_ACTIVE_TRIP.name());
        assertThat(check(result, "VEHICLE_SCHEDULE").getStatus())
                .isEqualTo(TripReadinessCheckStatus.BLOCKED);
    }

    @Test
    void getStartReadiness_marksContractChecksNotApplicableWithoutContract() {
        Trip trip = readyTrip();
        trip.setContract(null);
        stubReadiness(trip, noDepositRequired(), List.of(trip), List.of(trip));

        TripReadinessDto result = service.getStartReadiness("trip-1");

        assertThat(result.getReady()).isTrue();
        assertThat(result.getNotApplicableCount()).isEqualTo(4);
        assertThat(result.getPassedCount()).isEqualTo(13);
        assertThat(result.getChecks())
                .filteredOn(item -> "CONTRACT".equals(item.getGroup()))
                .allMatch(item -> item.getStatus() == TripReadinessCheckStatus.NOT_APPLICABLE);
    }

    @Test
    void startTrip_updatesTripAndVehicleAfterRecheckingReadiness() {
        Trip trip = readyTrip();
        stubReadiness(trip, noDepositRequired(), List.of(trip), List.of(trip));

        service.startTrip("trip-1");

        assertThat(trip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        assertThat(trip.getVehicle().getStatus()).isEqualTo(VehicleStatus.IN_TRIP);
        verify(vehicleRepository).save(trip.getVehicle());
        verify(tripRepository).save(trip);
    }

    private void stubCreationEntities(Customer customer, Contract contract) {
        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(
                Vehicle.builder().id("vehicle-1").status(VehicleStatus.AVAILABLE).build()
        ));
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(
                Driver.builder().id("driver-1").build()
        ));
        when(customerRepository.findById("customer-1")).thenReturn(Optional.of(customer));
        when(contractRepository.findById("contract-1")).thenReturn(Optional.of(contract));
    }

    private TripCreationRequest creationRequest() {
        return TripCreationRequest.builder()
                .vehicleId("vehicle-1")
                .driverId("driver-1")
                .customerId("customer-1")
                .contractId("contract-1")
                .startLocation("Điểm đi")
                .endLocation("Điểm đến")
                .startTime(LocalDateTime.now().plusDays(1).toString())
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2).toString())
                .distanceKm(10D)
                .cargoWeightTon(2D)
                .freightRatePerTonKm(1000D)
                .build();
    }

    private Trip readyTrip() {
        LocalDateTime start = LocalDateTime.now()
                .minusMinutes(5)
                .withSecond(0)
                .withNano(0);
        Customer customer = Customer.builder()
                .id("customer-1")
                .name("Công ty Minh An")
                .build();
        Contract contract = Contract.builder()
                .id("contract-1")
                .contractCode("HD-001")
                .customer(customer)
                .status(ContractStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .build();
        Vehicle vehicle = Vehicle.builder()
                .id("vehicle-1")
                .licensePlate("51C-123.45")
                .capacity(10D)
                .status(VehicleStatus.AVAILABLE)
                .build();
        Driver driver = Driver.builder()
                .id("driver-1")
                .name("Nguyễn Văn Bình")
                .licenseExpiration(LocalDate.now().plusDays(30))
                .build();

        return Trip.builder()
                .id("trip-1")
                .vehicle(vehicle)
                .driver(driver)
                .customer(customer)
                .contract(contract)
                .startLocation("4418 Nguyễn Cửu Phú, Bình Tân, TP.HCM")
                .endLocation("742 Hương lộ 2, Bình Tân, TP.HCM")
                .startTime(start.toString())
                .endTime(start.plusHours(3).toString())
                .distanceKm(10D)
                .cargoWeightTon(2D)
                .freightRatePerTonKm(1_000D)
                .freightAmount(20_000D)
                .status(TripStatus.CREATED)
                .build();
    }

    private DepositSummaryDto noDepositRequired() {
        return DepositSummaryDto.builder()
                .required(false)
                .requiredAmount(0D)
                .receivedAmount(0D)
                .shortfallAmount(0D)
                .build();
    }

    private void stubReadiness(
            Trip trip,
            DepositSummaryDto depositSummary,
            List<Trip> vehicleTrips,
            List<Trip> driverTrips
    ) {
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(tripRepository.findByVehicleId(trip.getVehicle().getId())).thenReturn(vehicleTrips);
        when(tripRepository.findByDriverId(trip.getDriver().getId())).thenReturn(driverTrips);
        when(depositService.getSummaryForTrip(trip.getId())).thenReturn(depositSummary);
    }

    private TripReadinessCheckDto check(TripReadinessDto readiness, String key) {
        return readiness.getChecks().stream()
                .filter(item -> key.equals(item.getKey()))
                .findFirst()
                .orElseThrow();
    }
}
