package com.FMS.services.impl;

import com.FMS.dto.TripDto;
import com.FMS.dto.TripReadinessDto;
import com.FMS.dto.TripReportDto;
import com.FMS.dto.request.TripCreationRequest;
import com.FMS.entity.Contract;
import com.FMS.entity.Customer;
import com.FMS.entity.Driver;
import com.FMS.entity.Trip;
import com.FMS.entity.User;
import com.FMS.entity.Vehicle;
import com.FMS.enums.Role;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.ExpenseMapper;
import com.FMS.mapper.InvoiceMapper;
import com.FMS.mapper.TripMapper;
import com.FMS.repositories.*;
import com.FMS.services.DepositService;
import com.FMS.services.TripService;
import com.FMS.services.impl.trip.TripExpenseHelper;
import com.FMS.services.impl.trip.TripFormatter;
import com.FMS.services.impl.trip.TripReadinessAssessor;
import com.FMS.services.impl.trip.TripValidator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TripServiceImpl implements TripService {

    TripRepository tripRepository;
    DriverRepository driverRepository;
    VehicleRepository vehicleRepository;
    CustomerRepository customerRepository;
    ContractRepository contractRepository;
    InvoiceRepository invoiceRepository;
    ExpenseRepository expenseRepository;
    DepositRepository depositRepository;
    TripMapper tripMapper;
    DepositService depositService;

    TripReadinessAssessor readinessAssessor;
    TripValidator validator;
    TripExpenseHelper expenseHelper;

    @Autowired
    public TripServiceImpl(
            TripRepository tripRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            CustomerRepository customerRepository,
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository,
            ExpenseRepository expenseRepository,
            DepositRepository depositRepository,
            TripMapper tripMapper,
            DepositService depositService,
            TripReadinessAssessor readinessAssessor,
            TripValidator validator,
            TripExpenseHelper expenseHelper
    ) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
        this.depositRepository = depositRepository;
        this.tripMapper = tripMapper;
        this.depositService = depositService;
        this.readinessAssessor = readinessAssessor;
        this.validator = validator;
        this.expenseHelper = expenseHelper;
    }

    // Backward-compatible constructor for existing tests instantiating TripServiceImpl directly
    public TripServiceImpl(
            TripRepository tripRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            CustomerRepository customerRepository,
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository,
            ExpenseRepository expenseRepository,
            DepositRepository depositRepository,
            InvoiceMapper invoiceMapper,
            ExpenseMapper expenseMapper,
            TripMapper tripMapper,
            DepositService depositService
    ) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
        this.depositRepository = depositRepository;
        this.tripMapper = tripMapper;
        this.depositService = depositService;

        TripFormatter formatter = new TripFormatter();
        this.validator = new TripValidator(tripRepository);
        this.readinessAssessor = new TripReadinessAssessor(depositService, this.validator, formatter);
        this.expenseHelper = new TripExpenseHelper(invoiceRepository, expenseRepository, invoiceMapper, expenseMapper, formatter);
    }

    @Override
    @Transactional
    public TripDto createTrip(TripCreationRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        Contract contract = null;
        if (request.getContractId() != null && !request.getContractId().isBlank()) {
            contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        }

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new AppException(ErrorCode.VEHICLE_NOT_AVAILABLE);
        }

        TripValidator.TimeWindow requestedWindow = validator.getTimeWindow(request.getStartTime(), request.getEndTime(), true);
        if (!requestedWindow.isComplete()) {
            throw new AppException(ErrorCode.INVALID_TRIP_TIME);
        }
        validator.validateContractForTrip(contract, customer, requestedWindow);

        if (validator.hasVehicleScheduleConflict(vehicle.getId(), requestedWindow, null)) {
            throw new AppException(ErrorCode.VEHICLE_HAS_ACTIVE_TRIP);
        }

        if (validator.hasDriverScheduleConflict(driver.getId(), requestedWindow, null)) {
            throw new AppException(ErrorCode.DRIVER_HAS_ACTIVE_TRIP);
        }

        Double freightRatePerTonKm = validator.resolveFreightRate(request, contract);

        Trip trip = Trip.builder()
                .vehicle(vehicle)
                .driver(driver)
                .customer(customer)
                .contract(contract)
                .startLocation(request.getStartLocation())
                .endLocation(request.getEndLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .distanceKm(request.getDistanceKm())
                .cargoWeightTon(request.getCargoWeightTon())
                .freightRatePerTonKm(freightRatePerTonKm)
                .freightAmount(validator.calculateFreightAmount(
                        request.getDistanceKm(),
                        request.getCargoWeightTon(),
                        freightRatePerTonKm
                ))
                .status(TripStatus.CREATED)
                .build();

        return toDto(tripRepository.save(trip));
    }

    @Override
    @Transactional
    public TripDto updateTrip(String id, Trip request) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));

        if (trip.getStatus() != TripStatus.CREATED && trip.getStatus() != TripStatus.ASSIGNED) {
            throw new AppException(ErrorCode.TRIP_CANNOT_EDIT);
        }

        TripValidator.TimeWindow requestedWindow = validator.getTimeWindow(request.getStartTime(), request.getEndTime(), true);
        if (!requestedWindow.isComplete()) {
            throw new AppException(ErrorCode.INVALID_TRIP_TIME);
        }
        validator.validateContractForTrip(trip.getContract(), trip.getCustomer(), requestedWindow);
        if (trip.getVehicle() != null
                && validator.hasVehicleScheduleConflict(trip.getVehicle().getId(), requestedWindow, trip.getId())) {
            throw new AppException(ErrorCode.VEHICLE_HAS_ACTIVE_TRIP);
        }
        if (trip.getDriver() != null
                && validator.hasDriverScheduleConflict(trip.getDriver().getId(), requestedWindow, trip.getId())) {
            throw new AppException(ErrorCode.DRIVER_HAS_ACTIVE_TRIP);
        }

        trip.setStartLocation(request.getStartLocation());
        trip.setEndLocation(request.getEndLocation());
        trip.setStartTime(request.getStartTime());
        trip.setEndTime(request.getEndTime());
        trip.setDistanceKm(request.getDistanceKm());
        trip.setCargoWeightTon(request.getCargoWeightTon());
        trip.setFreightRatePerTonKm(request.getFreightRatePerTonKm());
        trip.setFreightAmount(validator.calculateFreightAmount(
                request.getDistanceKm(),
                request.getCargoWeightTon(),
                request.getFreightRatePerTonKm()
        ));

        return toDto(tripRepository.save(trip));
    }

    @Override
    public TripDto getTripById(String id) {
        return toDto(tripRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND)));
    }

    @Override
    public List<TripDto> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<TripDto> getMyTrips(User actor) {
        if (actor == null || actor.getRole() != Role.DRIVER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        Driver driver = driverRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));
        return tripRepository.findByDriverId(driver.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTrip(String id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));

        if (invoiceRepository.existsByTrip_Id(id)
                || expenseRepository.existsByTripId(id)
                || depositRepository.existsByTripId(id)) {
            throw new AppException(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);
        }

        Vehicle vehicle = trip.getVehicle();
        if (trip.getStatus() == TripStatus.IN_PROGRESS
                && vehicle != null
                && vehicle.getStatus() == VehicleStatus.IN_TRIP) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        tripRepository.delete(trip);
    }

    @Override
    public List<TripDto> getByStatus(TripStatus status) {
        return tripRepository.findByStatus(status)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void startTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));

        TripReadinessAssessor.ReadinessAssessment assessment = readinessAssessor.assessStartReadiness(trip);
        if (!Boolean.TRUE.equals(assessment.result().getReady())) {
            throw new AppException(assessment.firstBlocker() == null
                    ? ErrorCode.TRIP_CANNOT_START
                    : assessment.firstBlocker());
        }

        Vehicle vehicle = trip.getVehicle();
        trip.setStatus(TripStatus.IN_PROGRESS);
        vehicle.setStatus(VehicleStatus.IN_TRIP);
        vehicleRepository.save(vehicle);
        tripRepository.save(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public TripReadinessDto getStartReadiness(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return readinessAssessor.assessStartReadiness(trip).result();
    }

    @Override
    @Transactional
    public void completeTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.TRIP_CANNOT_COMPLETE);
        }

        trip.setStatus(TripStatus.COMPLETED);
        Vehicle vehicle = trip.getVehicle();

        if (vehicle != null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void cancelTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new AppException(ErrorCode.TRIP_CANNOT_CANCEL);
        }

        trip.setStatus(TripStatus.CANCELLED);
        Vehicle vehicle = trip.getVehicle();

        if (vehicle != null && vehicle.getStatus() == VehicleStatus.IN_TRIP) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        tripRepository.save(trip);
    }

    @Override
    public List<TripDto> getByDriver(String driverId) {
        return tripRepository.findByDriverId(driverId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<TripDto> getByVehicle(String vehicleId) {
        return tripRepository.findByVehicleId(vehicleId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<TripDto> getByCustomer(String customerId) {
        return tripRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public TripReportDto generateReport(String tripId) {
        return expenseHelper.generateReport(tripId);
    }

    private TripDto toDto(Trip trip) {
        TripDto dto = tripMapper.toDto(trip);
        if (trip.getId() != null) {
            dto.setDepositSummary(depositService.getSummaryForTrip(trip.getId()));
            dto.setExpenseSummary(expenseHelper.expenseSummary(trip.getId()));
        }
        return dto;
    }
}
