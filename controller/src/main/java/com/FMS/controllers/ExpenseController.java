package com.FMS.controllers;

import com.FMS.dto.ExpenseDto;
import com.FMS.dto.request.ExpenseCreationRequest;
import com.FMS.dto.request.ExpenseReviewRequest;
import com.FMS.entity.Expense;
import com.FMS.entity.User;
import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.ExpenseType;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.ExpenseServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseController {

    ExpenseServiceImpl expenseServiceImpl;

    @PostMapping
    ApiResponse<ExpenseDto> create(
            @RequestBody @Valid ExpenseCreationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.<ExpenseDto>builder()
                .result(expenseServiceImpl.createExpense(request, currentUser(authentication)))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<ExpenseDto> update(
            @PathVariable String id,
            @RequestBody @Valid Expense request,
            Authentication authentication
    ) {
        return ApiResponse.<ExpenseDto>builder()
                .result(expenseServiceImpl.updateExpense(id, request, currentUser(authentication)))
                .build();
    }

    @PatchMapping("/{id}/approve")
    ApiResponse<ExpenseDto> approve(
            @PathVariable String id,
            @RequestBody(required = false) @Valid ExpenseReviewRequest request,
            Authentication authentication
    ) {
        return ApiResponse.<ExpenseDto>builder()
                .result(expenseServiceImpl.reviewExpense(
                        id,
                        ExpenseStatus.APPROVED,
                        request,
                        currentUser(authentication)
                ))
                .build();
    }

    @PatchMapping("/{id}/reject")
    ApiResponse<ExpenseDto> reject(
            @PathVariable String id,
            @RequestBody @Valid ExpenseReviewRequest request,
            Authentication authentication
    ) {
        return ApiResponse.<ExpenseDto>builder()
                .result(expenseServiceImpl.reviewExpense(
                        id,
                        ExpenseStatus.REJECTED,
                        request,
                        currentUser(authentication)
                ))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        expenseServiceImpl.delete(id);
        return ApiResponse.<String>builder()
                .result("Expense deleted successfully")
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<ExpenseDto> getById(@PathVariable String id) {
        return ApiResponse.<ExpenseDto>builder()
                .result(expenseServiceImpl.getById(id))
                .build();
    }

    @GetMapping
    ApiResponse<List<ExpenseDto>> getAll() {
        return ApiResponse.<List<ExpenseDto>>builder()
                .result(expenseServiceImpl.getAll())
                .build();
    }

    @GetMapping("/trip/{tripId}")
    ApiResponse<List<ExpenseDto>> getByTrip(
            @PathVariable String tripId,
            Authentication authentication
    ) {
        return ApiResponse.<List<ExpenseDto>>builder()
                .result(expenseServiceImpl.findByTrip(tripId, currentUser(authentication)))
                .build();
    }

    @GetMapping("/type/{expenseType}")
    ApiResponse<List<ExpenseDto>> getByType(@PathVariable ExpenseType expenseType) {
        return ApiResponse.<List<ExpenseDto>>builder()
                .result(expenseServiceImpl.findByExpenseType(expenseType))
                .build();
    }

    @GetMapping("/trip/{tripId}/total")
    ApiResponse<Double> calculateTripExpense(
            @PathVariable String tripId,
            Authentication authentication
    ) {
        return ApiResponse.<Double>builder()
                .result(expenseServiceImpl.calculateTripExpense(tripId, currentUser(authentication)))
                .build();
    }

    @GetMapping("/total")
    ApiResponse<Double> calculateTotalExpense() {
        return ApiResponse.<Double>builder()
                .result(expenseServiceImpl.calculateTotalExpense())
                .build();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }
}
