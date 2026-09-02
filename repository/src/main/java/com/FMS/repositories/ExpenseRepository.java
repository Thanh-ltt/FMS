package com.FMS.repositories;

import com.FMS.entity.Expense;
import com.FMS.enums.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense,String> {
    List<Expense> findByTripId(String tripId);

    boolean existsByTripId(String tripId);

    List<Expense> findByExpenseType(ExpenseType expenseType);
}
