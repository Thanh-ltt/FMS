package com.FMS.config;

import com.FMS.jwt.JwtAuthenticationFilter;
import com.FMS.jwt.PasswordChangeRequiredFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login", "/auth/register"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.DELETE,
                        "/users/**",
                        "/vehicles/**",
                        "/drivers/**",
                        "/customers/**",
                        "/contracts/**",
                        "/trips/**",
                        "/invoices/**",
                        "/deposits/**",
                        "/expenses/**",
                        "/maintenances/**"
                ).hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("ADMIN")
                .requestMatchers("/vehicles/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/drivers/my-profile").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/drivers/*/account").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/drivers/*/account/password").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/drivers/*/account").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/drivers/**").hasRole("ADMIN")
                .requestMatchers("/drivers/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.GET, "/contracts/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers("/contracts/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/customer-portal/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/maintenances/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers("/maintenances/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.GET, "/trips/my").hasRole("DRIVER")
                .requestMatchers(HttpMethod.GET, "/trips/**").hasAnyRole("ADMIN", "MANAGER", "DRIVER", "ACCOUNTANT")
                .requestMatchers("/trips/**").hasAnyRole("ADMIN", "MANAGER", "DRIVER")
                .requestMatchers("/customers/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers("/invoices/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers("/deposits/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers(HttpMethod.POST, "/expenses").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT", "DRIVER")
                .requestMatchers(HttpMethod.GET, "/expenses/trip/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT", "DRIVER")
                .requestMatchers(HttpMethod.PATCH, "/expenses/*/approve", "/expenses/*/reject").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers("/expenses/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                .requestMatchers("/gps/**").hasAnyRole("ADMIN", "MANAGER", "DRIVER", "ACCOUNTANT", "CUSTOMER")
                .requestMatchers("/driver-portal/**").hasAnyRole("ADMIN", "MANAGER", "DRIVER")
                .requestMatchers("/dispatch/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/payment/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT", "CUSTOMER")
                .requestMatchers("/notifications/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(passwordChangeRequiredFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
