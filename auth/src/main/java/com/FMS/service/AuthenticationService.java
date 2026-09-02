package com.FMS.service;

import com.FMS.dto.request.LoginRequest;
import com.FMS.dto.request.RefreshTokenRequest;
import com.FMS.dto.request.RegisterRequest;
import com.FMS.dto.request.PasswordChangeRequest;
import com.FMS.dto.response.AuthResponse;

public interface AuthenticationService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse changePassword(String username, PasswordChangeRequest request);
}
