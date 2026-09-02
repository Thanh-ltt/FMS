package com.FMS.services.impl;

import com.FMS.dto.TripProofDto;
import com.FMS.dto.request.TripProofCreateRequest;
import com.FMS.entity.Trip;
import com.FMS.entity.TripProof;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.TripProofMapper;
import com.FMS.repositories.TripProofRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.services.DriverPortalService;
import com.FMS.services.TripService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DriverPortalServiceImpl implements DriverPortalService {

    TripProofRepository tripProofRepository;
    TripRepository tripRepository;
    TripService tripService;
    TripProofMapper tripProofMapper;

    @Override
    @Transactional
    public TripProofDto createProof(String tripId, TripProofCreateRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));

        TripProof proof = tripProofRepository.findByTripId(tripId)
                .orElseGet(() -> TripProof.builder().trip(trip).build());

        proof.setRecipientName(request.getRecipientName());
        proof.setRecipientPhone(request.getRecipientPhone());
        proof.setSignatureBase64(request.getSignatureBase64());
        proof.setPhotoUrls(request.getPhotoUrls());
        proof.setNotes(request.getNotes());
        proof.setSignedAt(LocalDateTime.now());

        return tripProofMapper.toDto(tripProofRepository.save(proof));
    }

    @Override
    public TripProofDto getProofByTripId(String tripId) {
        return tripProofRepository.findByTripId(tripId)
                .map(tripProofMapper::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public void startTrip(String tripId) {
        tripService.startTrip(tripId);
    }

    @Override
    @Transactional
    public TripProofDto completeTripWithProof(String tripId, TripProofCreateRequest request) {
        TripProofDto proof = createProof(tripId, request);
        tripService.completeTrip(tripId);
        return proof;
    }
}
