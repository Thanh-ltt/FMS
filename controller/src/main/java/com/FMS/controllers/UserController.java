package com.FMS.controllers;

import com.FMS.dto.UserDto;
import com.FMS.entity.User;
import com.FMS.dto.request.EmployeeCreationRequest;
import com.FMS.dto.request.EmployeeStatusRequest;
import com.FMS.dto.request.EmployeeUpdateRequest;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserServiceImpl userServiceImpl;

    @PostMapping
    ApiResponse<UserDto> createUser(@RequestBody @Valid User user) {
        return ApiResponse.<UserDto>builder()
                .result(userServiceImpl.createUser(user))
                .build();
    }

    @GetMapping
    ApiResponse<List<UserDto>> getAllUsers() {
        return ApiResponse.<List<UserDto>>builder()
                .result(userServiceImpl.getAllUsers())
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<UserDto> updateUser(@PathVariable String id, @RequestBody User user) {
        return ApiResponse.<UserDto>builder()
                .result(userServiceImpl.updateUser(id, user))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteUserById(@PathVariable String id) {
        if (!StringUtils.hasText(id)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        userServiceImpl.deleteUser(id);
        return ApiResponse.<String>builder()
                .result("User deleted successfully")
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<UserDto> getUserById(@PathVariable String id) {
        return ApiResponse.<UserDto>builder()
                .result(userServiceImpl.getById(id))
                .build();
    }

    @PostMapping("/employees")
    ApiResponse<UserDto> createEmployee(@RequestBody @Valid EmployeeCreationRequest request) {
        return ApiResponse.<UserDto>builder()
                .result(userServiceImpl.createEmployee(request))
                .build();
    }

    @GetMapping("/employees")
    ApiResponse<List<UserDto>> getEmployees() {
        return ApiResponse.<List<UserDto>>builder()
                .result(userServiceImpl.getEmployees())
                .build();
    }

    @GetMapping("/employees/{id}")
    ApiResponse<UserDto> getEmployee(@PathVariable String id) {
        return ApiResponse.<UserDto>builder()
                .result(userServiceImpl.getEmployeeById(id))
                .build();
    }

    @PutMapping("/employees/{id}")
    ApiResponse<UserDto> updateEmployee(
            @PathVariable String id,
            @RequestBody @Valid EmployeeUpdateRequest request
    ) {
        return ApiResponse.<UserDto>builder()
                .result(userServiceImpl.updateEmployee(id, request))
                .build();
    }

    @PatchMapping("/employees/{id}/status")
    ApiResponse<UserDto> updateEmployeeStatus(
            @PathVariable String id,
            @RequestBody @Valid EmployeeStatusRequest request
    ) {
        return ApiResponse.<UserDto>builder()
                .result(userServiceImpl.setEmployeeActive(id, request.getActive()))
                .build();
    }

    @DeleteMapping("/employees/{id}")
    ApiResponse<String> deleteEmployee(@PathVariable String id) {
        userServiceImpl.deleteEmployee(id);
        return ApiResponse.<String>builder()
                .result("Employee deleted successfully")
                .build();
    }
}
