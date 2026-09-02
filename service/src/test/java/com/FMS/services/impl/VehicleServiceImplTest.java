package com.FMS.services.impl;

import com.FMS.dto.VehicleDto;
import com.FMS.entity.Trip;
import com.FMS.entity.Vehicle;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.VehicleMapper;
import com.FMS.repositories.MaintenanceRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleMapper vehicleMapper;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private MaintenanceRepository maintenanceRepository;

    private VehicleServiceImpl vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleServiceImpl(
                vehicleRepository,
                vehicleMapper,
                tripRepository,
                maintenanceRepository
        );
    }

    @Test
    void update_rejectsManuallyManagedStatus() {
        Vehicle existing = Vehicle.builder()
                .id("vehicle-1")
                .licensePlate("51A-12345")
                .status(VehicleStatus.AVAILABLE)
                .build();
        Vehicle request = Vehicle.builder()
                .licensePlate("51A-12345")
                .vehicleType("Truck")
                .capacity(10D)
                .status(VehicleStatus.MAINTENANCE)
                .build();

        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> vehicleService.update("vehicle-1", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_STATUS_MANAGED);

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void update_allowsRecoveringStaleInTripStatus() {
        Vehicle existing = Vehicle.builder()
                .id("vehicle-1")
                .licensePlate("51A-12345")
                .status(VehicleStatus.IN_TRIP)
                .build();
        Vehicle request = Vehicle.builder()
                .licensePlate("51A-12345")
                .vehicleType("Truck")
                .capacity(10D)
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(existing));
        when(tripRepository.existsByVehicleIdAndStatusIn("vehicle-1", List.of(TripStatus.IN_PROGRESS)))
                .thenReturn(false);
        when(maintenanceRepository.findByVehicleId("vehicle-1")).thenReturn(List.of());
        when(vehicleRepository.save(existing)).thenReturn(existing);
        when(vehicleMapper.toDto(existing)).thenReturn(VehicleDto.builder().status(VehicleStatus.AVAILABLE).build());

        vehicleService.update("vehicle-1", request);

        assertThat(existing.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepository).save(existing);
    }

    @Test
    void update_rejectsAvailableStatusWhileTripIsActive() {
        Vehicle existing = Vehicle.builder()
                .id("vehicle-1")
                .licensePlate("51A-12345")
                .status(VehicleStatus.IN_TRIP)
                .build();
        Vehicle request = Vehicle.builder()
                .licensePlate("51A-12345")
                .vehicleType("Truck")
                .capacity(10D)
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(existing));
        when(tripRepository.existsByVehicleIdAndStatusIn("vehicle-1", List.of(TripStatus.IN_PROGRESS)))
                .thenReturn(true);
        when(maintenanceRepository.findByVehicleId("vehicle-1")).thenReturn(List.of());

        assertThatThrownBy(() -> vehicleService.update("vehicle-1", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_STATUS_MANAGED);

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void delete_rejectsVehicleWithTripHistory() {
        Vehicle vehicle = Vehicle.builder()
                .id("vehicle-1")
                .licensePlate("51A-12345")
                .status(VehicleStatus.AVAILABLE)
                .build();
        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(vehicle));
        when(tripRepository.findByVehicleId("vehicle-1"))
                .thenReturn(List.of(Trip.builder().vehicle(vehicle).status(TripStatus.COMPLETED).build()));

        assertThatThrownBy(() -> vehicleService.delete("vehicle-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);

        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }
}
