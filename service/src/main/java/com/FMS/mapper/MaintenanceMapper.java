package com.FMS.mapper;

import com.FMS.dto.MaintenanceDto;
import com.FMS.entity.Maintenance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehiclePlate", source = "vehicle.licensePlate")
    MaintenanceDto toDto(Maintenance maintenance);
}
