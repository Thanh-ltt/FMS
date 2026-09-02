package com.FMS.services.impl;

import com.FMS.entity.CargoRate;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.repositories.CargoRateRepository;
import com.FMS.services.CargoRateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CargoRateServiceImpl implements CargoRateService {
    CargoRateRepository cargoRateRepository;

    @Override
    public List<CargoRate> getAll() {
        return cargoRateRepository.findAll();
    }

    @Override
    public CargoRate updateRate(String cargoType, CargoRate request) {
        if (request.getRatePerTonKm() == null || request.getRatePerTonKm() <= 0) {
            throw new AppException(ErrorCode.INVALID_FREIGHT_INPUT);
        }

        CargoRate cargoRate = cargoRateRepository.findByCargoType(cargoType)
                .orElseGet(() -> CargoRate.builder().cargoType(cargoType).build());

        cargoRate.setCargoLabel(request.getCargoLabel() == null || request.getCargoLabel().isBlank()
                ? cargoType
                : request.getCargoLabel());
        cargoRate.setRatePerTonKm(request.getRatePerTonKm());

        return cargoRateRepository.save(cargoRate);
    }
}
