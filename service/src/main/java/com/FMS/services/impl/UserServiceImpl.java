package com.FMS.services.impl;

import com.FMS.dto.ChangePasswordDto;
import com.FMS.dto.UserDto;
import com.FMS.dto.request.EmployeeCreationRequest;
import com.FMS.dto.request.EmployeeUpdateRequest;
import com.FMS.entity.User;
import com.FMS.enums.Role;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.UserMapper;
import com.FMS.repositories.UserRepository;
import com.FMS.services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Comparator;
import java.util.Set;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class UserServiceImpl implements UserService {

    static final Set<Role> EMPLOYEE_ROLES = Set.of(Role.MANAGER, Role.ACCOUNTANT);
    static final int MAX_AVATAR_LENGTH = 2_200_000;

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserDto createUser(User request) {
        validateUsername(request == null ? null : request.getUsername());
        validatePassword(request == null ? null : request.getPassword());
        if (request.getRole() == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        request.setUsername(username);

        if (request.getPassword() != null) {
            request.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getActive() == null) {
            request.setActive(true);
        }
        if (request.getMustChangePassword() == null) {
            request.setMustChangePassword(false);
        }

        User user = userRepository.save(request);

        return userMapper.toDto(user);
    }


    public UserDto updateUser(String id, User request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getUsername() != null) {
            validateUsername(request.getUsername());
            String username = request.getUsername().trim();
            if (userRepository.existsByUsernameAndIdNot(username, id)) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }
            user.setUsername(username);
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getPassword() != null) {
            validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getMustChangePassword() != null) {
            user.setMustChangePassword(request.getMustChangePassword());
        }

        User updatedUser = userRepository.save(user);

        return userMapper.toDto(updatedUser);
    }


    public UserDto getById(String id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toDto(user);
    }

    public List<UserDto> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }


    public void deleteUser(String id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
    }


    public void changePassword(String id, ChangePasswordDto request) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }

    @Override
    public UserDto createEmployee(EmployeeCreationRequest request) {
        validateEmployeeRole(request.getRole());
        validateEmployeeDates(request.getDob(), request.getHireDate());
        validateUniqueEmployee(request.getUsername(), request.getEmployeeCode(), null);

        User employee = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .employeeCode(normalizeEmployeeCode(request.getEmployeeCode()))
                .fullName(request.getFullName().trim())
                .phone(normalizePhone(request.getPhone()))
                .email(normalizeNullable(request.getEmail()))
                .address(normalizeNullable(request.getAddress()))
                .idNumber(normalizeNullable(request.getIdNumber()))
                .dob(request.getDob())
                .gender(request.getGender())
                .position(resolvePosition(request.getPosition(), request.getRole()))
                .hireDate(request.getHireDate())
                .avatarUrl(validateAvatar(request.getAvatarUrl()))
                .active(true)
                .build();

        return userMapper.toDto(userRepository.save(employee));
    }

    @Override
    public UserDto updateEmployee(String id, EmployeeUpdateRequest request) {
        User employee = findEmployee(id);
        validateEmployeeRole(request.getRole());
        validateEmployeeDates(request.getDob(), request.getHireDate());
        validateUniqueEmployee(request.getUsername(), request.getEmployeeCode(), id);

        employee.setUsername(request.getUsername().trim());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        employee.setRole(request.getRole());
        employee.setEmployeeCode(normalizeEmployeeCode(request.getEmployeeCode()));
        employee.setFullName(request.getFullName().trim());
        employee.setPhone(normalizePhone(request.getPhone()));
        employee.setEmail(normalizeNullable(request.getEmail()));
        employee.setAddress(normalizeNullable(request.getAddress()));
        employee.setIdNumber(normalizeNullable(request.getIdNumber()));
        employee.setDob(request.getDob());
        employee.setGender(request.getGender());
        employee.setPosition(resolvePosition(request.getPosition(), request.getRole()));
        employee.setHireDate(request.getHireDate());
        employee.setAvatarUrl(validateAvatar(request.getAvatarUrl()));

        return userMapper.toDto(userRepository.save(employee));
    }

    @Override
    public UserDto setEmployeeActive(String id, boolean active) {
        User employee = findEmployee(id);
        employee.setActive(active);
        return userMapper.toDto(userRepository.save(employee));
    }

    @Override
    public void deleteEmployee(String id) {
        User employee = findEmployee(id);
        userRepository.delete(employee);
    }

    @Override
    public UserDto getEmployeeById(String id) {
        return userMapper.toDto(findEmployee(id));
    }

    @Override
    public List<UserDto> getEmployees() {
        return userRepository.findByRoleIn(List.copyOf(EMPLOYEE_ROLES))
                .stream()
                .sorted(Comparator.comparing(
                        user -> user.getFullName() == null ? "" : user.getFullName(),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(userMapper::toDto)
                .toList();
    }

    private User findEmployee(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!EMPLOYEE_ROLES.contains(user.getRole())) {
            throw new AppException(ErrorCode.INVALID_EMPLOYEE_ROLE);
        }
        return user;
    }

    private void validateEmployeeRole(Role role) {
        if (role == null || !EMPLOYEE_ROLES.contains(role)) {
            throw new AppException(ErrorCode.INVALID_EMPLOYEE_ROLE);
        }
    }

    private void validateUniqueEmployee(String username, String employeeCode, String ignoredId) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmployeeCode = normalizeEmployeeCode(employeeCode);

        boolean usernameExists = ignoredId == null
                ? userRepository.existsByUsername(normalizedUsername)
                : userRepository.existsByUsernameAndIdNot(normalizedUsername, ignoredId);
        if (usernameExists) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        boolean employeeCodeExists = ignoredId == null
                ? userRepository.existsByEmployeeCode(normalizedEmployeeCode)
                : userRepository.existsByEmployeeCodeAndIdNot(normalizedEmployeeCode, ignoredId);
        if (employeeCodeExists) {
            throw new AppException(ErrorCode.EMPLOYEE_CODE_ALREADY_EXISTS);
        }
    }

    private String validateAvatar(String avatarUrl) {
        String normalized = normalizeNullable(avatarUrl);
        if (normalized == null) {
            return null;
        }
        boolean supportedSource = normalized.startsWith("data:image/")
                || normalized.startsWith("https://")
                || normalized.startsWith("http://");
        if (!supportedSource || normalized.length() > MAX_AVATAR_LENGTH) {
            throw new AppException(ErrorCode.INVALID_AVATAR);
        }
        return normalized;
    }

    private String normalizeEmployeeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizePhone(String value) {
        return value == null ? null : value.trim().replaceAll("[ .-]", "");
    }

    private void validateEmployeeDates(LocalDate dob, LocalDate hireDate) {
        LocalDate today = LocalDate.now();
        if (dob == null
                || Period.between(dob, today).getYears() < 18
                || hireDate == null
                || hireDate.isAfter(today)) {
            throw new AppException(ErrorCode.INVALID_EMPLOYEE_DATE);
        }
    }

    private String resolvePosition(String position, Role role) {
        String normalized = normalizeNullable(position);
        if (normalized != null) {
            return normalized;
        }
        return role == Role.ACCOUNTANT ? "Kế toán" : "Quản lý vận hành";
    }

    private void validateUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.length() < 6 || normalized.length() > 50) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
    }

}
