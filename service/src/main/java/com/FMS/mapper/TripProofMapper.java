package com.FMS.mapper;

import com.FMS.dto.TripProofDto;
import com.FMS.entity.TripProof;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TripProofMapper {

    @Mapping(target = "tripId", source = "trip.id")
    TripProofDto toDto(TripProof tripProof);
}
