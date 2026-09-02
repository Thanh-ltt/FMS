package com.FMS.services;

import com.FMS.entity.CargoRate;

import java.util.List;

public interface CargoRateService {
    List<CargoRate> getAll();

    CargoRate updateRate(String cargoType, CargoRate request);
}
