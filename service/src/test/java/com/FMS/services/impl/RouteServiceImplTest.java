package com.FMS.services.impl;

import com.FMS.dto.RouteDistanceDto;
import com.FMS.dto.request.RouteDistanceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RouteServiceImplTest {
    private final RouteServiceImpl routeService = new RouteServiceImpl(new ObjectMapper());

    @Test
    void calculateDistance_estimatesSameStreetByHouseNumbers() {
        RouteDistanceDto result = routeService.calculateDistance(RouteDistanceRequest.builder()
                .startLocation("4418 Nguyễn Cửu Phú, Tân Tạo, Hồ Chí Minh, Việt Nam")
                .endLocation("4369 Nguyễn Cửu Phú, Tân Tạo, Hồ Chí Minh, Việt Nam")
                .build());

        assertThat(result.getSource()).isEqualTo("SAME_STREET_ESTIMATE");
        assertThat(result.getDistanceKm()).isCloseTo(0.54, within(0.001));
    }

    @Test
    void calculateDistance_estimatesGenericSameStreetAddress() {
        RouteDistanceDto result = routeService.calculateDistance(RouteDistanceRequest.builder()
                .startLocation("100 Đường Lê Lợi, Phường Bến Nghé, Quận 1, Thành phố Hồ Chí Minh")
                .endLocation("120 Đường Lê Lợi, Phường Bến Nghé, Quận 1, Thành phố Hồ Chí Minh")
                .build());

        assertThat(result.getSource()).isEqualTo("SAME_STREET_ESTIMATE");
        assertThat(result.getDistanceKm()).isCloseTo(0.2, within(0.001));
    }
}
