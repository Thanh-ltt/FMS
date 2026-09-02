package com.FMS.repositories;

import com.FMS.entity.Vehicle;
import com.FMS.enums.VehicleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle,String> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, String id);

    List<Vehicle> findByStatus(VehicleStatus status);

    List<Vehicle> findByCapacityBetween(Double min, Double max);

    List<Vehicle> findByLicensePlateContainingIgnoreCase(String keyword);

    long countByStatus(VehicleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vehicle from Vehicle vehicle where vehicle.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") String id);
}
