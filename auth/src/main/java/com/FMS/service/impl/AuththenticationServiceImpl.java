package com.FMS.service.impl;

import com.FMS.dto.request.LoginRequest;
import com.FMS.dto.request.RefreshTokenRequest;
import com.FMS.dto.request.RegisterRequest;
import com.FMS.dto.request.PasswordChangeRequest;
import com.FMS.dto.response.AuthResponse;
import com.FMS.entity.User;
import com.FMS.entity.Customer;
import com.FMS.enums.Role;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.jwt.JwtService;
import com.FMS.repositories.UserRepository;
import com.FMS.repositories.CustomerRepository;
import com.FMS.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class AuththenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    CustomerRepository customerRepository;
    PasswordEncoder  passwordEncoder;
    JwtService  jwtService;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.USER_DISABLED);
        }

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .username(user.getUsername())
                .role(user.getRole())
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if(userRepository.existsByUsername(username)){
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        userRepository.save(user);

        Customer customer = Customer.builder()
                .name(username)
                .user(user)
                .build();
        customerRepository.save(customer);

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .username(user.getUsername())
                .role(Role.CUSTOMER)
                .mustChangePassword(false)
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        var username = jwtService.extractUsername(request.getToken());

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!jwtService.isValidToken(request.getToken(), user)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        var token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse changePassword(String username, PasswordChangeRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_MUST_DIFFER);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .username(user.getUsername())
                .role(user.getRole())
                .mustChangePassword(false)
                .build();
    }

}
