package com.FMS.repositories;

import com.FMS.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver,String> {
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);
    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, String id);
    boolean existsByName(String name);
    List<Driver> findByLicenseExpirationBefore(LocalDate date);
    java.util.Optional<Driver> findByUserId(String userId);
}
