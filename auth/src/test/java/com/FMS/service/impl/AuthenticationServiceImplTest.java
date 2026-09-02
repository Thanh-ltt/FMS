package com.FMS.service.impl;

import com.FMS.dto.request.RegisterRequest;
import com.FMS.dto.request.LoginRequest;
import com.FMS.dto.request.PasswordChangeRequest;
import com.FMS.dto.response.AuthResponse;
import com.FMS.entity.Customer;
import com.FMS.entity.User;
import com.FMS.enums.Role;
import com.FMS.jwt.JwtService;
import com.FMS.repositories.CustomerRepository;
import com.FMS.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuththenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuththenticationServiceImpl(
                userRepository,
                customerRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void register_alwaysCreatesCustomerAccountAndLinkedProfile() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        AuthResponse result = authenticationService.register(RegisterRequest.builder()
                .username("minhlong")
                .password("password123")
                .build());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(userRepository).save(userCaptor.capture());
        verify(customerRepository).save(customerCaptor.capture());

        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(userCaptor.getValue().isEnabled()).isTrue();
        assertThat(customerCaptor.getValue().getUser()).isSameAs(userCaptor.getValue());
        assertThat(customerCaptor.getValue().getName()).isEqualTo("minhlong");
        assertThat(customerCaptor.getValue().getPhone()).isNull();
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void login_returnsPasswordChangeRequirement() {
        User driver = User.builder()
                .id("driver-user-1")
                .username("driver01")
                .password("encoded-temporary")
                .role(Role.DRIVER)
                .active(true)
                .mustChangePassword(true)
                .build();
        when(userRepository.findByUsername("driver01")).thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("temporary123", "encoded-temporary")).thenReturn(true);
        when(jwtService.generateToken(driver)).thenReturn("token");

        AuthResponse result = authenticationService.login(LoginRequest.builder()
                .username("driver01")
                .password("temporary123")
                .build());

        assertThat(result.getMustChangePassword()).isTrue();
        assertThat(result.getRole()).isEqualTo(Role.DRIVER);
    }

    @Test
    void changePassword_verifiesCurrentPasswordAndClearsRequirement() {
        User driver = User.builder()
                .id("driver-user-1")
                .username("driver01")
                .password("encoded-temporary")
                .role(Role.DRIVER)
                .mustChangePassword(true)
                .build();
        when(userRepository.findByUsername("driver01")).thenReturn(Optional.of(driver));
        when(passwordEncoder.matches("temporary123", "encoded-temporary")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", "encoded-temporary")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
        when(jwtService.generateToken(driver)).thenReturn("new-token");

        AuthResponse result = authenticationService.changePassword(
                "driver01",
                PasswordChangeRequest.builder()
                        .currentPassword("temporary123")
                        .newPassword("newPassword123")
                        .build()
        );

        assertThat(driver.getPassword()).isEqualTo("encoded-new-password");
        assertThat(driver.getMustChangePassword()).isFalse();
        assertThat(result.getMustChangePassword()).isFalse();
        assertThat(result.getToken()).isEqualTo("new-token");
        verify(userRepository).save(driver);
    }
}
