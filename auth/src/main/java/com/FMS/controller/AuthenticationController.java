package com.FMS.controller;

import com.FMS.dto.request.LoginRequest;
import com.FMS.dto.request.RegisterRequest;
import com.FMS.dto.request.PasswordChangeRequest;
import com.FMS.dto.response.AuthResponse;
import com.FMS.response.ApiResponse;
import com.FMS.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authenticationService.register(request))
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authenticationService.login(request))
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<AuthResponse> changePassword(
            @RequestBody @Valid PasswordChangeRequest request,
            Authentication authentication
    ) {
        return ApiResponse.<AuthResponse>builder()
                .result(authenticationService.changePassword(authentication.getName(), request))
                .build();
    }
}
