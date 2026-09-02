package com.FMS.mapper;

import com.FMS.dto.ExpenseDto;
import com.FMS.entity.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "tripId", source = "trip.id")
    ExpenseDto toDto(Expense expense);
}
