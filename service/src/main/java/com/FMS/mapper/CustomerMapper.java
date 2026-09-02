package com.FMS.mapper;

import com.FMS.dto.CustomerDto;
import com.FMS.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    CustomerDto toDto(Customer customer);
}
