package com.FMS.services.impl;

import com.FMS.dto.DriverDto;
import com.FMS.dto.UserDto;
import com.FMS.dto.request.DriverAccountProvisionRequest;
import com.FMS.entity.Driver;
import com.FMS.dto.request.DriverPasswordResetRequest;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.DriverMapper;
import com.FMS.repositories.DriverRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.UserRepository;
import com.FMS.services.DriverService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import com.FMS.dto.request.DriverCreationRequest;
import com.FMS.entity.User;
import com.FMS.enums.Role;
import com.FMS.services.UserService;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class DriverServiceImpl implements DriverService {

    static final int MAX_AVATAR_LENGTH = 2_200_000;

    DriverRepository driverRepository;
    DriverMapper driverMapper;
    TripRepository tripRepository;
    UserService userService;
    UserRepository userRepository;

    @Override
    @Transactional
    public DriverDto createDriver(DriverCreationRequest request) {
        validateDriverDates(request.getDob(), request.getLicenseExpiration());
        String licenseNumber = normalizeUppercase(request.getLicenseNumber());

        if (driverRepository.existsByLicenseNumberIgnoreCase(licenseNumber)) {
            throw new AppException(ErrorCode.DRIVER_ALREADY_EXISTS);
        }

        String avatarUrl = validateAvatar(request.getAvatarUrl());

        Driver driver = new Driver();
        driver.setName(request.getName().trim());
        driver.setDob(request.getDob());
        driver.setPhone(normalizePhone(request.getPhone()));
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiration(request.getLicenseExpiration());
        driver.setAddress(request.getAddress().trim());
        driver.setAvatarUrl(avatarUrl);

        return driverMapper.toDto(driverRepository.save(driver));
    }

    @Override
    public DriverDto getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));
                
        return toDto(driver);
    }
    public DriverDto updateDriver(String id, Driver request) {
        Driver driver = driverRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.DRIVER_NOT_FOUND));

        validateDriverDates(request.getDob(), request.getLicenseExpiration());
        String licenseNumber = normalizeUppercase(request.getLicenseNumber());
        if (driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber, id)) {
            throw new AppException(ErrorCode.DRIVER_ALREADY_EXISTS);
        }

        driver.setName(request.getName().trim());
        driver.setPhone(normalizePhone(request.getPhone()));
        driver.setDob(request.getDob());
        driver.setAddress(request.getAddress().trim());
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiration(request.getLicenseExpiration());
        driver.setAvatarUrl(validateAvatar(request.getAvatarUrl()));

        return toDto(driverRepository.save(driver));
    }

    @Override
    public DriverDto getById(String id) {
        return toDto(driverRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.DRIVER_NOT_FOUND)));
    }

    @Override
    public List<DriverDto> getAll() {

        return driverRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(String id) {
        Driver driver = driverRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.DRIVER_NOT_FOUND));

        if (!tripRepository.findByDriverId(id).isEmpty()) {
            throw new AppException(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);
        }

        String userId = driver.getUserId();
        driverRepository.delete(driver);
        driverRepository.flush();
        if (userId != null && !userId.isBlank()) {
            userRepository.findById(userId)
                    .filter(linkedUser -> linkedUser.getRole() == Role.DRIVER)
                    .ifPresent(userRepository::delete);
        }
    }

    @Override
    public List<DriverDto> findExpiredLicenses() {
        return driverRepository
                .findByLicenseExpirationBefore(LocalDate.now())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<DriverDto> findDriversWithoutActiveTrip() {

        return driverRepository.findAll()
                .stream()
                .filter(driver -> !tripRepository
                        .existsByDriverIdAndStatus(driver.getId(), TripStatus.IN_PROGRESS))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DriverDto provisionAccount(String driverId, DriverAccountProvisionRequest request, User actor) {
        validateCanProvisionAccount(actor);
        Driver driver = findDriver(driverId);
        if (driver.getUserId() != null && !driver.getUserId().isBlank()) {
            User existingAccount = userRepository.findById(driver.getUserId()).orElse(null);
            if (existingAccount != null) {
                validateDriverAccount(existingAccount);
                throw new AppException(ErrorCode.DRIVER_ACCOUNT_ALREADY_LINKED);
            }
            driver.setUserId(null);
        }

        User account = User.builder()
                .username(request.getUsername().trim())
                .password(request.getPassword())
                .role(Role.DRIVER)
                .active(true)
                .mustChangePassword(true)
                .build();
        UserDto createdAccount = userService.createUser(account);

        driver.setUserId(createdAccount.getId());
        driver.setAccountProvisionedByUserId(actor.getId());
        driver.setAccountProvisionedByName(actorDisplayName(actor));
        driver.setAccountProvisionedByRole(actor.getRole());
        driver.setAccountProvisionedAt(LocalDateTime.now());
        Driver savedDriver = driverRepository.save(driver);
        DriverDto dto = driverMapper.toDto(savedDriver);
        dto.setUsername(createdAccount.getUsername());
        return dto;
    }

    @Override
    @Transactional
    public DriverDto resetAccountPassword(String driverId, DriverPasswordResetRequest request, User actor) {
        validateAdmin(actor);
        Driver driver = findDriver(driverId);
        User account = requireDriverAccount(driver);
        User passwordUpdate = new User();
        passwordUpdate.setPassword(request.getPassword());
        passwordUpdate.setMustChangePassword(true);
        userService.updateUser(account.getId(), passwordUpdate);

        DriverDto dto = driverMapper.toDto(driver);
        dto.setUsername(account.getUsername());
        return dto;
    }

    @Override
    @Transactional
    public DriverDto revokeAccount(String driverId, User actor) {
        validateAdmin(actor);
        Driver driver = findDriver(driverId);
        User account = requireDriverAccount(driver);

        driver.setUserId(null);
        Driver savedDriver = driverRepository.saveAndFlush(driver);
        userRepository.delete(account);

        DriverDto dto = driverMapper.toDto(savedDriver);
        dto.setUsername(null);
        return dto;
    }

    private DriverDto toDto(Driver driver) {
        DriverDto dto = driverMapper.toDto(driver);
        if (driver.getUserId() != null && !driver.getUserId().isBlank()) {
            userRepository.findById(driver.getUserId())
                    .filter(linkedUser -> linkedUser.getRole() == Role.DRIVER)
                    .ifPresent(linkedUser -> dto.setUsername(linkedUser.getUsername()));
        }
        return dto;
    }

    private Driver findDriver(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));
    }

    private User requireDriverAccount(Driver driver) {
        if (driver.getUserId() == null || driver.getUserId().isBlank()) {
            throw new AppException(ErrorCode.DRIVER_ACCOUNT_NOT_LINKED);
        }
        User account = userRepository.findById(driver.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_ACCOUNT_NOT_LINKED));
        validateDriverAccount(account);
        return account;
    }

    private void validateDriverAccount(User account) {
        if (account.getRole() != Role.DRIVER) {
            throw new AppException(ErrorCode.INVALID_DRIVER_ACCOUNT);
        }
    }

    private void validateCanProvisionAccount(User actor) {
        if (actor == null || (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MANAGER)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateAdmin(User actor) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String actorDisplayName(User actor) {
        return actor.getFullName() == null || actor.getFullName().isBlank()
                ? actor.getUsername()
                : actor.getFullName().trim();
    }

    private String validateAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }

        String normalized = avatarUrl.trim();
        boolean supportedSource = normalized.startsWith("data:image/")
                || normalized.startsWith("https://")
                || normalized.startsWith("http://");
        if (!supportedSource || normalized.length() > MAX_AVATAR_LENGTH) {
            throw new AppException(ErrorCode.INVALID_AVATAR);
        }
        return normalized;
    }

    private void validateDriverDates(LocalDate dob, LocalDate licenseExpiration) {
        if (dob == null || Period.between(dob, LocalDate.now()).getYears() < 18) {
            throw new AppException(ErrorCode.DRIVER_AGE_INVALID);
        }
        if (licenseExpiration == null || licenseExpiration.isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_DRIVER_LICENSE);
        }
    }

    private String normalizePhone(String value) {
        return value == null ? null : value.trim().replaceAll("[ .-]", "");
    }

    private String normalizeUppercase(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
