package com.FMS.services;

import com.FMS.dto.DriverDto;
import com.FMS.entity.Driver;

import java.util.List;

import com.FMS.dto.request.DriverCreationRequest;
import com.FMS.dto.request.DriverAccountProvisionRequest;
import com.FMS.dto.request.DriverPasswordResetRequest;
import com.FMS.entity.User;

public interface DriverService {
    DriverDto createDriver(DriverCreationRequest request);

    DriverDto getMyProfile(String username);

    DriverDto updateDriver(String id, Driver request);

    DriverDto getById(String id);

    List<DriverDto> getAll();

    void delete(String id);

    List<DriverDto> findDriversWithoutActiveTrip();

    List<DriverDto> findExpiredLicenses();

    DriverDto provisionAccount(String driverId, DriverAccountProvisionRequest request, User actor);

    DriverDto resetAccountPassword(String driverId, DriverPasswordResetRequest request, User actor);

    DriverDto revokeAccount(String driverId, User actor);

}
