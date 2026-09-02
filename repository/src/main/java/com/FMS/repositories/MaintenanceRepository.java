package com.FMS.repositories;

import com.FMS.entity.Maintenance;
import com.FMS.enums.MaintenanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance,String> {
    List<Maintenance> findByVehicleId(String vehicleId);
    @Query("""
        SELECT COALESCE(SUM(m.cost),0)
        FROM Maintenance m
        WHERE m.vehicle.id = :vehicleId
          AND m.status = :status
        """)
    Double calculateMaintenanceCost(
            @Param("vehicleId") String vehicleId,
            @Param("status") MaintenanceStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select maintenance from Maintenance maintenance where maintenance.id = :id")
    Optional<Maintenance> findByIdForUpdate(@Param("id") String id);
}
