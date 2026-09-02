package com.FMS.services;

import com.FMS.dto.TripDto;
import com.FMS.dto.TripReadinessDto;
import com.FMS.dto.TripReportDto;
import com.FMS.dto.request.TripCreationRequest;
import com.FMS.entity.Trip;
import com.FMS.entity.User;
import com.FMS.enums.TripStatus;

import java.util.List;

public interface TripService {

    TripDto createTrip(TripCreationRequest request);

    TripDto updateTrip(String id, Trip request);

    TripDto getTripById(String id);

    List<TripDto> getAllTrips();

    List<TripDto> getMyTrips(User actor);

    void deleteTrip(String id);

    List<TripDto> getByStatus(TripStatus status);

    void startTrip(String tripId);

    TripReadinessDto getStartReadiness(String tripId);

    void completeTrip(String tripId);

    void cancelTrip(String tripId);

    List<TripDto> getByDriver(String driverId);

    List<TripDto> getByVehicle(String vehicleId);

    List<TripDto> getByCustomer(String customerId);

    /**
     * Generates a revenue and expense report for a given trip.
     * @param tripId the ID of the trip
     * @return a TripReportDto containing totals and optional details
     */
    TripReportDto generateReport(String tripId);
}
