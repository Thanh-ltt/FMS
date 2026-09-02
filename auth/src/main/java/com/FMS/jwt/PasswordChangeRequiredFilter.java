package com.FMS.jwt;

import com.FMS.entity.User;
import com.FMS.exception.ErrorCode;
import com.FMS.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/change-password"
    );

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean passwordChangeRequired = authentication != null
                && authentication.getPrincipal() instanceof User user
                && Boolean.TRUE.equals(user.getMustChangePassword());
        boolean allowedRequest = "OPTIONS".equalsIgnoreCase(request.getMethod())
                || ALLOWED_PATHS.contains(request.getRequestURI());

        if (!passwordChangeRequired || allowedRequest) {
            filterChain.doFilter(request, response);
            return;
        }

        ErrorCode errorCode = ErrorCode.PASSWORD_CHANGE_REQUIRED;
        response.setStatus(errorCode.getStatusCode().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build());
    }
}
