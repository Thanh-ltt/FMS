package com.FMS.services;

import com.FMS.dto.ChangePasswordDto;
import com.FMS.dto.UserDto;
import com.FMS.entity.User;
import com.FMS.dto.request.EmployeeCreationRequest;
import com.FMS.dto.request.EmployeeUpdateRequest;

import java.util.List;

public interface UserService {
    UserDto createUser(User request);

    UserDto updateUser(String id, User request);

    UserDto getById(String id);

    List<UserDto> getAllUsers();

    void deleteUser(String id);

    void changePassword(String userId, ChangePasswordDto request);

    UserDto createEmployee(EmployeeCreationRequest request);

    UserDto updateEmployee(String id, EmployeeUpdateRequest request);

    UserDto setEmployeeActive(String id, boolean active);

    void deleteEmployee(String id);

    UserDto getEmployeeById(String id);

    List<UserDto> getEmployees();

}
