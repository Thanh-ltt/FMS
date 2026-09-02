package com.FMS.mapper;

import com.FMS.dto.ContractDto;
import com.FMS.entity.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerUsername", source = "customer.user.username")
    ContractDto toDto(Contract contract);
}
