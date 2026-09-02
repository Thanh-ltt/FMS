package com.FMS.services;

import com.FMS.dto.TripProofDto;
import com.FMS.dto.request.TripProofCreateRequest;

public interface DriverPortalService {
    TripProofDto createProof(String tripId, TripProofCreateRequest request);
    TripProofDto getProofByTripId(String tripId);
    void startTrip(String tripId);
    TripProofDto completeTripWithProof(String tripId, TripProofCreateRequest request);
}
