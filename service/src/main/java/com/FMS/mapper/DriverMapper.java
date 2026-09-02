package com.FMS.mapper;

import com.FMS.dto.DriverDto;
import com.FMS.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    @Mapping(target = "username", ignore = true)
    DriverDto toDto(Driver driver);
}
