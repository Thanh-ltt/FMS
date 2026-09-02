package com.FMS.services.impl;

import com.FMS.dto.VehicleLocationDto;
import com.FMS.dto.request.VehicleLocationRecordRequest;
import com.FMS.entity.Trip;
import com.FMS.entity.Vehicle;
import com.FMS.entity.VehicleLocation;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.VehicleLocationMapper;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.VehicleLocationRepository;
import com.FMS.repositories.VehicleRepository;
import com.FMS.services.GpsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GpsServiceImpl implements GpsService {

    VehicleLocationRepository vehicleLocationRepository;
    VehicleRepository vehicleRepository;
    TripRepository tripRepository;
    VehicleLocationMapper vehicleLocationMapper;

    private static final double DEFAULT_LAT = 10.7769; // TP.HCM Bến Thành
    private static final double DEFAULT_LNG = 106.7009;

    @Override
    @Transactional
    public VehicleLocationDto recordLocation(String vehicleId, VehicleLocationRecordRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        Trip trip = null;
        if (request.getTripId() != null && !request.getTripId().isBlank()) {
            trip = tripRepository.findById(request.getTripId()).orElse(null);
        }

        VehicleLocation location = VehicleLocation.builder()
                .vehicle(vehicle)
                .trip(trip)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speed(request.getSpeed() != null ? request.getSpeed() : 50.0)
                .heading(request.getHeading() != null ? request.getHeading() : 0.0)
                .recordedAt(LocalDateTime.now())
                .build();

        return vehicleLocationMapper.toDto(vehicleLocationRepository.save(location));
    }

    @Override
    public VehicleLocationDto getLatestLocation(String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        return vehicleLocationRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId)
                .map(vehicleLocationMapper::toDto)
                .orElseGet(() -> VehicleLocationDto.builder()
                        .vehicleId(vehicle.getId())
                        .licensePlate(vehicle.getLicensePlate())
                        .latitude(DEFAULT_LAT)
                        .longitude(DEFAULT_LNG)
                        .speed(0.0)
                        .heading(0.0)
                        .recordedAt(LocalDateTime.now())
                        .build());
    }

    @Override
    public List<VehicleLocationDto> getTripHistory(String tripId) {
        return vehicleLocationRepository.findByTripIdOrderByRecordedAtAsc(tripId)
                .stream()
                .map(vehicleLocationMapper::toDto)
                .toList();
    }

    @Override
    public List<VehicleLocationDto> getVehicleHistory(String vehicleId) {
        return vehicleLocationRepository.findByVehicleIdOrderByRecordedAtAsc(vehicleId)
                .stream()
                .map(vehicleLocationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public VehicleLocationDto simulateMovement(String vehicleId, String tripId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        Trip trip = null;
        if (tripId != null && !tripId.isBlank()) {
            trip = tripRepository.findById(tripId).orElse(null);
        }

        VehicleLocation lastLoc = vehicleLocationRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId).orElse(null);

        double baseLat = lastLoc != null ? lastLoc.getLatitude() : DEFAULT_LAT;
        double baseLng = lastLoc != null ? lastLoc.getLongitude() : DEFAULT_LNG;

        Random random = new Random();
        double nextLat = baseLat + (random.nextDouble() - 0.48) * 0.005;
        double nextLng = baseLng + (random.nextDouble() - 0.48) * 0.005;
        double speed = 40.0 + random.nextDouble() * 30.0;
        double heading = random.nextDouble() * 360.0;

        VehicleLocation location = VehicleLocation.builder()
                .vehicle(vehicle)
                .trip(trip)
                .latitude(nextLat)
                .longitude(nextLng)
                .speed(Math.round(speed * 10.0) / 10.0)
                .heading(Math.round(heading * 10.0) / 10.0)
                .recordedAt(LocalDateTime.now())
                .build();

        return vehicleLocationMapper.toDto(vehicleLocationRepository.save(location));
    }
}
