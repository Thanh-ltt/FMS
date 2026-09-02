package com.FMS.services;

import com.FMS.dto.ExpenseDto;
import com.FMS.dto.request.ExpenseCreationRequest;
import com.FMS.dto.request.ExpenseReviewRequest;
import com.FMS.entity.Expense;
import com.FMS.entity.User;
import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.ExpenseType;

import java.util.List;

public interface ExpenseService {
    ExpenseDto createExpense(ExpenseCreationRequest request, User actor);

    ExpenseDto updateExpense(String id, Expense request, User actor);

    ExpenseDto reviewExpense(String id, ExpenseStatus status, ExpenseReviewRequest request, User actor);

    void delete(String id);

    ExpenseDto getById(String id);

    List<ExpenseDto> getAll();

    Double calculateTripExpense(String tripId, User actor);

    Double calculateTotalExpense();

    List<ExpenseDto> findByTrip(String tripId, User actor);

    List<ExpenseDto> findByExpenseType(ExpenseType expenseType);
}
