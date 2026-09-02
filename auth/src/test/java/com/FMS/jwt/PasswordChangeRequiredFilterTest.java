package com.FMS.jwt;

import com.FMS.entity.User;
import com.FMS.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordChangeRequiredFilterTest {
    private final PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter(new ObjectMapper());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksOperationalRequestsUntilPasswordIsChanged() throws Exception {
        authenticateTemporaryDriver();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/trips/my");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":1106");
    }

    @Test
    void allowsPasswordChangeEndpoint() throws Exception {
        authenticateTemporaryDriver();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/change-password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    private void authenticateTemporaryDriver() {
        User driver = User.builder()
                .username("driver01")
                .role(Role.DRIVER)
                .mustChangePassword(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(driver, null, driver.getAuthorities())
        );
    }
}
