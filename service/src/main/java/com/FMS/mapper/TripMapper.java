package com.FMS.mapper;

import com.FMS.dto.TripDto;
import com.FMS.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TripMapper {
    @Mapping(target = "depositSummary", ignore = true)
    @Mapping(target = "expenseSummary", ignore = true)
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehiclePlate", source = "vehicle.licensePlate")
    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", source = "driver.name")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerUsername", source = "customer.user.username")
    @Mapping(target = "contractId", source = "contract.id")
    @Mapping(target = "contractCode", source = "contract.contractCode")
    TripDto toDto(Trip trip);
}
