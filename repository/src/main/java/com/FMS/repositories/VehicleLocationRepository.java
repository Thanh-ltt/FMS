package com.FMS.repositories;

import com.FMS.entity.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, String> {

    Optional<VehicleLocation> findTopByVehicleIdOrderByRecordedAtDesc(String vehicleId);

    List<VehicleLocation> findByTripIdOrderByRecordedAtAsc(String tripId);

    List<VehicleLocation> findByVehicleIdOrderByRecordedAtAsc(String vehicleId);

    Optional<VehicleLocation> findTopByTripIdOrderByRecordedAtDesc(String tripId);
}
