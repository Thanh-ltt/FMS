package com.FMS.repositories;

import com.FMS.entity.CargoRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CargoRateRepository extends JpaRepository<CargoRate, String> {
    Optional<CargoRate> findByCargoType(String cargoType);
}
