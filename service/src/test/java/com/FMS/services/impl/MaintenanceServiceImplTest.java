package com.FMS.services.impl;

import com.FMS.dto.MaintenanceDto;
import com.FMS.dto.request.MaintenanceCreationRequest;
import com.FMS.entity.Maintenance;
import com.FMS.entity.Trip;
import com.FMS.entity.Vehicle;
import com.FMS.enums.MaintenanceStatus;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.MaintenanceMapper;
import com.FMS.repositories.MaintenanceRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceImplTest {
    @Mock
    private MaintenanceRepository maintenanceRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private MaintenanceMapper maintenanceMapper;

    private MaintenanceServiceImpl maintenanceService;

    @BeforeEach
    void setUp() {
        maintenanceService = new MaintenanceServiceImpl(
                maintenanceRepository,
                vehicleRepository,
                tripRepository,
                maintenanceMapper
        );
    }

    @Test
    void createMaintenance_createsPendingScheduleWithoutChangingVehicleStatus() {
        Vehicle vehicle = Vehicle.builder()
                .id("vehicle-1")
                .status(VehicleStatus.AVAILABLE)
                .build();
        MaintenanceCreationRequest request = maintenanceRequest();

        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findByVehicleId("vehicle-1")).thenReturn(List.of());
        when(tripRepository.findByVehicleId("vehicle-1")).thenReturn(List.of());
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(maintenanceMapper.toDto(any(Maintenance.class))).thenReturn(MaintenanceDto.builder().build());

        maintenanceService.createMaintenance(request);

        ArgumentCaptor<Maintenance> maintenanceCaptor = ArgumentCaptor.forClass(Maintenance.class);
        verify(maintenanceRepository).save(maintenanceCaptor.capture());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(maintenanceCaptor.getValue().getStatus()).isEqualTo(MaintenanceStatus.PENDING);
        assertThat(maintenanceCaptor.getValue().getVehicle()).isSameAs(vehicle);
        assertThat(maintenanceCaptor.getValue().getMaintenanceTypes())
                .containsExactly("OIL_CHANGE", "TIRE");
        assertThat(maintenanceCaptor.getValue().getMaintenanceType()).isEqualTo("OIL_CHANGE");
    }

    @Test
    void createMaintenance_rejectsVehicleWithActiveTrip() {
        Vehicle vehicle = Vehicle.builder()
                .id("vehicle-1")
                .status(VehicleStatus.AVAILABLE)
                .build();
        Trip activeTrip = Trip.builder()
                .vehicle(vehicle)
                .status(TripStatus.IN_PROGRESS)
                .build();

        when(vehicleRepository.findById("vehicle-1")).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findByVehicleId("vehicle-1")).thenReturn(List.of());
        when(tripRepository.findByVehicleId("vehicle-1")).thenReturn(List.of(activeTrip));

        assertThatThrownBy(() -> maintenanceService.createMaintenance(maintenanceRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_AVAILABLE_FOR_MAINTENANCE);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void deleteMaintenance_rejectsActiveMaintenance() {
        Vehicle vehicle = Vehicle.builder()
                .id("vehicle-1")
                .status(VehicleStatus.MAINTENANCE)
                .build();
        Maintenance maintenance = Maintenance.builder()
                .id("maintenance-1")
                .vehicle(vehicle)
                .status(MaintenanceStatus.IN_PROGRESS)
                .build();
        when(maintenanceRepository.findByIdForUpdate("maintenance-1")).thenReturn(Optional.of(maintenance));

        assertThatThrownBy(() -> maintenanceService.deleteMaintenance("maintenance-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MAINTENANCE_CANNOT_DELETE);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(maintenanceRepository, never()).delete(maintenance);
    }

    @Test
    void startMaintenance_marksVehicleOnlyWhenScheduledDateIsDue() {
        Vehicle vehicle = Vehicle.builder()
                .id("vehicle-1")
                .status(VehicleStatus.AVAILABLE)
                .build();
        Maintenance maintenance = Maintenance.builder()
                .id("maintenance-1")
                .vehicle(vehicle)
                .maintenanceDate(LocalDate.now())
                .status(MaintenanceStatus.PENDING)
                .build();
        when(maintenanceRepository.findByIdForUpdate("maintenance-1")).thenReturn(Optional.of(maintenance));
        when(vehicleRepository.findByIdForUpdate("vehicle-1")).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findByVehicleId("vehicle-1")).thenReturn(List.of(maintenance));
        when(tripRepository.findByVehicleId("vehicle-1")).thenReturn(List.of());

        maintenanceService.startMaintenance("maintenance-1");

        assertThat(maintenance.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(maintenance.getStartedAt()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        verify(vehicleRepository).save(vehicle);
        verify(maintenanceRepository).save(maintenance);
    }

    @Test
    void startMaintenance_rejectsScheduleBeforeDueDate() {
        Maintenance maintenance = Maintenance.builder()
                .id("maintenance-1")
                .maintenanceDate(LocalDate.now().plusDays(1))
                .status(MaintenanceStatus.PENDING)
                .build();
        when(maintenanceRepository.findByIdForUpdate("maintenance-1")).thenReturn(Optional.of(maintenance));

        assertThatThrownBy(() -> maintenanceService.startMaintenance("maintenance-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MAINTENANCE_NOT_DUE);
    }

    private MaintenanceCreationRequest maintenanceRequest() {
        return MaintenanceCreationRequest.builder()
                .vehicleId("vehicle-1")
                .maintenanceTypes(List.of("OIL_CHANGE", "TIRE"))
                .maintenanceDate(LocalDate.now())
                .cost(0D)
                .build();
    }
}
