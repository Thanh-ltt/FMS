package com.FMS.mapper;

import com.FMS.dto.VehicleDto;
import com.FMS.entity.Vehicle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    VehicleDto toDto(Vehicle vehicle);
}
