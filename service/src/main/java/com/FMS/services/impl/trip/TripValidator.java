package com.FMS.services.impl.trip;

import com.FMS.dto.request.TripCreationRequest;
import com.FMS.entity.Contract;
import com.FMS.entity.Customer;
import com.FMS.entity.Trip;
import com.FMS.enums.ContractStatus;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.repositories.TripRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripValidator {

    public static final List<TripStatus> RESERVED_TRIP_STATUSES = List.of(
            TripStatus.CREATED,
            TripStatus.ASSIGNED,
            TripStatus.IN_PROGRESS
    );

    TripRepository tripRepository;

    public record TimeWindow(LocalDateTime start, LocalDateTime end) {
        public boolean isComplete() {
            return start != null && end != null;
        }
    }

    public TimeWindow getTimeWindow(String startTime, String endTime, boolean strict) {
        LocalDateTime start = parseTripTime(startTime, strict);
        LocalDateTime end = parseTripTime(endTime, strict);

        if (start != null && end != null && !start.isBefore(end)) {
            if (strict) {
                throw new AppException(ErrorCode.INVALID_TRIP_TIME);
            }
            return new TimeWindow(null, null);
        }

        return new TimeWindow(start, end);
    }

    public LocalDateTime parseTripTime(String value, boolean strict) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            if (strict) {
                throw new AppException(ErrorCode.INVALID_TRIP_TIME);
            }
            return null;
        }
    }

    public boolean hasVehicleScheduleConflict(String vehicleId, TimeWindow requestedWindow, String ignoredTripId) {
        return tripRepository.findByVehicleId(vehicleId)
                .stream()
                .anyMatch(trip -> hasScheduleConflict(trip, requestedWindow, ignoredTripId));
    }

    public boolean hasDriverScheduleConflict(String driverId, TimeWindow requestedWindow, String ignoredTripId) {
        return tripRepository.findByDriverId(driverId)
                .stream()
                .anyMatch(trip -> hasScheduleConflict(trip, requestedWindow, ignoredTripId));
    }

    private boolean hasScheduleConflict(Trip existingTrip, TimeWindow requestedWindow, String ignoredTripId) {
        if (ignoredTripId != null && ignoredTripId.equals(existingTrip.getId())) {
            return false;
        }

        if (!RESERVED_TRIP_STATUSES.contains(existingTrip.getStatus())) {
            return false;
        }

        if (!requestedWindow.isComplete()) {
            return true;
        }

        TimeWindow existingWindow = getTimeWindow(existingTrip.getStartTime(), existingTrip.getEndTime(), false);
        if (!existingWindow.isComplete()) {
            return true;
        }

        return requestedWindow.start().isBefore(existingWindow.end())
                && existingWindow.start().isBefore(requestedWindow.end());
    }

    public void validateContractForTrip(Contract contract, Customer customer, TimeWindow window) {
        if (contract == null) {
            return;
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        if (contract.getCustomer() == null
                || customer == null
                || !contract.getCustomer().getId().equals(customer.getId())) {
            throw new AppException(ErrorCode.CONTRACT_CUSTOMER_MISMATCH);
        }
        if (!window.isComplete()
                || contract.getStartDate() == null
                || contract.getEndDate() == null
                || window.start().toLocalDate().isBefore(contract.getStartDate())
                || window.end().toLocalDate().isAfter(contract.getEndDate())) {
            throw new AppException(ErrorCode.CONTRACT_OUTSIDE_VALIDITY);
        }
    }

    public Double calculateFreightAmount(Double distanceKm, Double cargoWeightTon, Double freightRatePerTonKm) {
        boolean hasAnyValue = distanceKm != null || cargoWeightTon != null || freightRatePerTonKm != null;
        boolean hasAllValues = distanceKm != null && cargoWeightTon != null && freightRatePerTonKm != null;

        if (!hasAnyValue) {
            return null;
        }

        if (!hasAllValues || distanceKm <= 0 || cargoWeightTon <= 0 || freightRatePerTonKm <= 0) {
            throw new AppException(ErrorCode.INVALID_FREIGHT_INPUT);
        }

        return distanceKm * cargoWeightTon * freightRatePerTonKm;
    }

    public Double resolveFreightRate(TripCreationRequest request, Contract contract) {
        if (request.getFreightRatePerTonKm() != null && request.getFreightRatePerTonKm() > 0) {
            return request.getFreightRatePerTonKm();
        }

        return contract == null ? null : contract.getFreightRatePerTonKm();
    }
}
