package com.FMS.services.impl;

import com.FMS.dto.request.DriverCreationRequest;
import com.FMS.dto.DriverDto;
import com.FMS.dto.UserDto;
import com.FMS.dto.request.DriverAccountProvisionRequest;
import com.FMS.dto.request.DriverPasswordResetRequest;
import com.FMS.entity.Driver;
import com.FMS.entity.User;
import com.FMS.enums.Role;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.DriverMapper;
import com.FMS.repositories.DriverRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.UserRepository;
import com.FMS.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private DriverMapper driverMapper;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    private DriverServiceImpl driverService;

    @BeforeEach
    void setUp() {
        driverService = new DriverServiceImpl(
                driverRepository,
                driverMapper,
                tripRepository,
                userService,
                userRepository
        );
    }

    @Test
    void createDriver_rejectsDriverYoungerThanEighteen() {
        DriverCreationRequest request = DriverCreationRequest.builder()
                .dob(LocalDate.now().minusYears(17))
                .licenseExpiration(LocalDate.now().plusYears(1))
                .build();

        assertThatThrownBy(() -> driverService.createDriver(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DRIVER_AGE_INVALID);
    }

    @Test
    void createDriver_rejectsExpiredLicense() {
        DriverCreationRequest request = DriverCreationRequest.builder()
                .dob(LocalDate.now().minusYears(30))
                .licenseExpiration(LocalDate.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> driverService.createDriver(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DRIVER_LICENSE);
    }

    @Test
    void createDriver_createsProfileWithoutLoginAccount() {
        DriverCreationRequest request = DriverCreationRequest.builder()
                .name("Nguyen Van Tai")
                .dob(LocalDate.now().minusYears(30))
                .phone("0901234567")
                .licenseNumber("B2-123456")
                .licenseExpiration(LocalDate.now().plusYears(2))
                .address("123 Nguyen Trai, TP.HCM")
                .build();
        DriverDto mappedDriver = DriverDto.builder()
                .id("driver-1")
                .name("Nguyen Van Tai")
                .build();
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver saved = invocation.getArgument(0);
            saved.setId("driver-1");
            return saved;
        });
        when(driverMapper.toDto(any(Driver.class))).thenReturn(mappedDriver);

        DriverDto result = driverService.createDriver(request);

        assertThat(result.getId()).isEqualTo("driver-1");
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void getById_includesLinkedUsername() {
        Driver driver = Driver.builder()
                .id("driver-1")
                .userId("user-1")
                .build();
        DriverDto mappedDriver = DriverDto.builder()
                .id("driver-1")
                .userId("user-1")
                .build();
        User linkedUser = User.builder()
                .id("user-1")
                .username("taixe.nguyen")
                .role(Role.DRIVER)
                .build();

        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverMapper.toDto(driver)).thenReturn(mappedDriver);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(linkedUser));

        DriverDto result = driverService.getById("driver-1");

        assertThat(result.getUsername()).isEqualTo("taixe.nguyen");
    }

    @Test
    void provisionAccount_createsDriverUserAndLinksExistingProfile() {
        Driver driver = Driver.builder().id("driver-1").build();
        User manager = User.builder()
                .id("manager-1")
                .username("manager01")
                .fullName("Nguyen Quan Ly")
                .role(Role.MANAGER)
                .build();
        DriverDto mappedDriver = DriverDto.builder().id("driver-1").build();
        UserDto createdAccount = UserDto.builder()
                .id("user-1")
                .username("taixe.nguyen")
                .role(Role.DRIVER)
                .build();
        DriverAccountProvisionRequest request = DriverAccountProvisionRequest.builder()
                .username(" taixe.nguyen ")
                .password("password123")
                .build();

        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(userService.createUser(any(User.class))).thenReturn(createdAccount);
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toDto(driver)).thenReturn(mappedDriver);

        DriverDto result = driverService.provisionAccount("driver-1", request, manager);

        ArgumentCaptor<User> accountCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getUsername()).isEqualTo("taixe.nguyen");
        assertThat(accountCaptor.getValue().getPassword()).isEqualTo("password123");
        assertThat(accountCaptor.getValue().getRole()).isEqualTo(Role.DRIVER);
        assertThat(accountCaptor.getValue().getMustChangePassword()).isTrue();
        assertThat(driver.getUserId()).isEqualTo("user-1");
        assertThat(driver.getAccountProvisionedByUserId()).isEqualTo("manager-1");
        assertThat(driver.getAccountProvisionedByName()).isEqualTo("Nguyen Quan Ly");
        assertThat(driver.getAccountProvisionedByRole()).isEqualTo(Role.MANAGER);
        assertThat(driver.getAccountProvisionedAt()).isNotNull();
        assertThat(result.getUsername()).isEqualTo("taixe.nguyen");
    }

    @Test
    void provisionAccount_rejectsDriverWithExistingAccount() {
        Driver driver = Driver.builder().id("driver-1").userId("user-1").build();
        User account = User.builder().id("user-1").role(Role.DRIVER).build();
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> driverService.provisionAccount(
                "driver-1",
                DriverAccountProvisionRequest.builder()
                        .username("driver01")
                        .password("password123")
                        .build(),
                User.builder().role(Role.MANAGER).build()
        ))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DRIVER_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    void resetAccountPassword_updatesOnlyLinkedDriverPassword() {
        Driver driver = Driver.builder().id("driver-1").userId("user-1").build();
        User account = User.builder()
                .id("user-1")
                .username("driver01")
                .role(Role.DRIVER)
                .build();
        DriverDto mappedDriver = DriverDto.builder().id("driver-1").build();
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(account));
        when(driverMapper.toDto(driver)).thenReturn(mappedDriver);

        DriverDto result = driverService.resetAccountPassword(
                "driver-1",
                DriverPasswordResetRequest.builder().password("newPassword123").build(),
                User.builder().role(Role.ADMIN).build()
        );

        ArgumentCaptor<User> updateCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateUser(org.mockito.ArgumentMatchers.eq("user-1"), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getPassword()).isEqualTo("newPassword123");
        assertThat(updateCaptor.getValue().getMustChangePassword()).isTrue();
        assertThat(result.getUsername()).isEqualTo("driver01");
    }

    @Test
    void revokeAccount_deletesCredentialsAndKeepsDriverProfile() {
        Driver driver = Driver.builder().id("driver-1").userId("user-1").build();
        User account = User.builder()
                .id("user-1")
                .username("driver01")
                .role(Role.DRIVER)
                .build();
        DriverDto mappedDriver = DriverDto.builder().id("driver-1").build();
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(account));
        when(driverRepository.saveAndFlush(driver)).thenReturn(driver);
        when(driverMapper.toDto(driver)).thenReturn(mappedDriver);

        DriverDto result = driverService.revokeAccount(
                "driver-1",
                User.builder().role(Role.ADMIN).build()
        );

        assertThat(driver.getUserId()).isNull();
        assertThat(result.getId()).isEqualTo("driver-1");
        assertThat(result.getUsername()).isNull();
        verify(userRepository).delete(account);
    }

    @Test
    void managerCannotResetOrRevokeDriverAccount() {
        User manager = User.builder().role(Role.MANAGER).build();

        assertThatThrownBy(() -> driverService.resetAccountPassword(
                "driver-1",
                DriverPasswordResetRequest.builder().password("newPassword123").build(),
                manager
        ))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        assertThatThrownBy(() -> driverService.revokeAccount("driver-1", manager))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
