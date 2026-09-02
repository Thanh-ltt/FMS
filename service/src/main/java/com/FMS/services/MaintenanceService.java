package com.FMS.services;

import com.FMS.dto.MaintenanceDto;
import com.FMS.dto.request.MaintenanceCreationRequest;
import com.FMS.dto.request.MaintenanceUpdateRequest;

import java.util.List;

public interface MaintenanceService {
    MaintenanceDto createMaintenance(MaintenanceCreationRequest request);

    MaintenanceDto updateMaintenance(String id, MaintenanceUpdateRequest request);

    void startMaintenance(String id);

    void completeMaintenance(String id);

    void cancelMaintenance(String id);

    void deleteMaintenance(String id);

    List<MaintenanceDto> getAllMaintenances();

    List<MaintenanceDto> getByVehicle(String id);

    Double calculateMaintenanceCost(String vehicleId);
}
