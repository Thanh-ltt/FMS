package com.FMS.repositories;

import com.FMS.entity.Trip;
import com.FMS.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip,String> {
    List<Trip> findByStatus(TripStatus status);

    boolean existsByDriverIdAndStatus(String driverId, TripStatus status);

    boolean existsByDriverIdAndStatusIn(String driverId, List<TripStatus> statuses);

    boolean existsByVehicleIdAndStatusIn(String vehicleId, List<TripStatus> statuses);

    boolean existsByContractId(String contractId);

    boolean existsByContractIdAndStatusIn(String contractId, List<TripStatus> statuses);

    List<Trip> findByDriverId(String driverId);

    List<Trip> findByVehicleId(String vehicleId);

    List<Trip> findByCustomerId(String customerId);
}
