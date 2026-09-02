package com.FMS.config;

import com.FMS.entity.User;
import com.FMS.enums.Role;
import com.FMS.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${FMS_ADMIN_USERNAME:}")
    private String adminUsername;

    @Value("${FMS_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.length() < 8) {
            log.warn("No ADMIN account exists. Set FMS_ADMIN_USERNAME and FMS_ADMIN_PASSWORD (minimum 8 characters) to create the initial admin.");
            return;
        }

        String username = adminUsername.trim();
        if (userRepository.existsByUsername(username)) {
            log.warn("Cannot bootstrap ADMIN because username '{}' is already in use.", username);
            return;
        }

        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .fullName("Quản trị hệ thống")
                .position("Quản trị viên")
                .active(true)
                .build());
        log.info("Initial ADMIN account '{}' was created.", username);
    }
}
