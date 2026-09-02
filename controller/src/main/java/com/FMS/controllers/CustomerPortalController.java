package com.FMS.controllers;

import com.FMS.dto.CustomerPortalDto;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.CustomerServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/customer-portal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerPortalController {

    CustomerServiceImpl customerServiceImpl;

    @GetMapping("/me")
    ApiResponse<CustomerPortalDto> getMyPortal(Authentication authentication) {
        return ApiResponse.<CustomerPortalDto>builder()
                .result(customerServiceImpl.getPortalByUsername(authentication.getName()))
                .build();
    }
}
