package com.FMS.mapper;

import com.FMS.dto.VehicleLocationDto;
import com.FMS.entity.VehicleLocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleLocationMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "tripId", source = "trip.id")
    VehicleLocationDto toDto(VehicleLocation location);
}
