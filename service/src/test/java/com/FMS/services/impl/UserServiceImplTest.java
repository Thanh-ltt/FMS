package com.FMS.services.impl;

import com.FMS.dto.request.EmployeeCreationRequest;
import com.FMS.entity.User;
import com.FMS.enums.Gender;
import com.FMS.enums.Role;
import com.FMS.exception.AppException;
import com.FMS.mapper.UserMapper;
import com.FMS.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
    }

    @Test
    void createEmployee_encodesPasswordAndCreatesActiveInternalUser() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createEmployee(EmployeeCreationRequest.builder()
                .username("manager01")
                .password("password123")
                .employeeCode(" nv001 ")
                .fullName("Nguyen Van An")
                .phone("0901234567")
                .email("an.nguyen@example.com")
                .address("123 Nguyen Trai, TP.HCM")
                .idNumber("079123456789")
                .dob(LocalDate.now().minusYears(30))
                .gender(Gender.MALE)
                .hireDate(LocalDate.now().minusDays(1))
                .role(Role.MANAGER)
                .build());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmployeeCode()).isEqualTo("NV001");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(Role.MANAGER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void createEmployee_rejectsCustomerRole() {
        EmployeeCreationRequest request = EmployeeCreationRequest.builder()
                .username("customer01")
                .password("password123")
                .employeeCode("NV002")
                .fullName("Customer")
                .phone("0900000000")
                .role(Role.CUSTOMER)
                .build();

        assertThatThrownBy(() -> userService.createEmployee(request))
                .isInstanceOf(AppException.class)
                .hasMessage("Employee role must be MANAGER or ACCOUNTANT");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createEmployee_rejectsEmployeeYoungerThanEighteen() {
        EmployeeCreationRequest request = EmployeeCreationRequest.builder()
                .username("manager02")
                .password("password123")
                .employeeCode("NV002")
                .fullName("Nhan Vien Tre")
                .phone("0900000000")
                .dob(LocalDate.now().minusYears(17))
                .hireDate(LocalDate.now())
                .role(Role.MANAGER)
                .build();

        assertThatThrownBy(() -> userService.createEmployee(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(com.FMS.exception.ErrorCode.INVALID_EMPLOYEE_DATE);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteEmployee_deletesInternalEmployee() {
        User employee = User.builder()
                .id("employee-1")
                .username("manager01")
                .role(Role.MANAGER)
                .build();
        when(userRepository.findById("employee-1")).thenReturn(Optional.of(employee));

        userService.deleteEmployee("employee-1");

        verify(userRepository).delete(employee);
    }

    @Test
    void deleteEmployee_rejectsNonEmployeeAccount() {
        User customer = User.builder()
                .id("customer-1")
                .username("customer01")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findById("customer-1")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> userService.deleteEmployee("customer-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(com.FMS.exception.ErrorCode.INVALID_EMPLOYEE_ROLE);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void updateUser_marksResetPasswordAsTemporary() {
        User driver = User.builder()
                .id("driver-user-1")
                .username("driver01")
                .password("old-encoded")
                .role(Role.DRIVER)
                .mustChangePassword(false)
                .build();
        when(userRepository.findById("driver-user-1")).thenReturn(Optional.of(driver));
        when(passwordEncoder.encode("temporary123")).thenReturn("temporary-encoded");
        when(userRepository.save(driver)).thenReturn(driver);

        userService.updateUser("driver-user-1", User.builder()
                .password("temporary123")
                .mustChangePassword(true)
                .build());

        assertThat(driver.getPassword()).isEqualTo("temporary-encoded");
        assertThat(driver.getMustChangePassword()).isTrue();
    }

}
