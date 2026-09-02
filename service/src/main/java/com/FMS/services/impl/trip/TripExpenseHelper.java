package com.FMS.services.impl.trip;

import com.FMS.dto.ExpenseDto;
import com.FMS.dto.ExpenseSummaryDto;
import com.FMS.dto.InvoiceDto;
import com.FMS.dto.TripReportDto;
import com.FMS.entity.Expense;
import com.FMS.entity.Invoice;
import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.InvoiceStatus;
import com.FMS.mapper.ExpenseMapper;
import com.FMS.mapper.InvoiceMapper;
import com.FMS.repositories.ExpenseRepository;
import com.FMS.repositories.InvoiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripExpenseHelper {

    InvoiceRepository invoiceRepository;
    ExpenseRepository expenseRepository;
    InvoiceMapper invoiceMapper;
    ExpenseMapper expenseMapper;
    TripFormatter tripFormatter;

    public TripReportDto generateReport(String tripId) {
        List<Invoice> invoices = invoiceRepository.findByTrip_Id(tripId);
        List<Expense> expenses = expenseRepository.findByTripId(tripId);
        List<Expense> approvedExpenses = expenses.stream()
                .filter(this::isApprovedExpense)
                .toList();

        List<Invoice> activeInvoices = invoices.stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.CANCELLED)
                .toList();

        double totalRevenue = activeInvoices.stream()
                .mapToDouble(Invoice::getTotalAmount)
                .sum();
        double totalExpense = approvedExpenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        List<InvoiceDto> invoiceDtos = activeInvoices.stream()
                .map(invoiceMapper::toDto)
                .toList();
        List<ExpenseDto> expenseDtos = approvedExpenses.stream()
                .map(expenseMapper::toDto)
                .toList();

        return new TripReportDto(totalRevenue, totalExpense, invoiceDtos, expenseDtos);
    }

    public ExpenseSummaryDto expenseSummary(String tripId) {
        List<Expense> expenses = expenseRepository.findByTripId(tripId);
        double approvedAmount = expenses.stream()
                .filter(this::isApprovedExpense)
                .mapToDouble(expense -> tripFormatter.valueOrZero(expense.getAmount()))
                .sum();
        double pendingAmount = expenses.stream()
                .filter(expense -> expense.getStatus() == ExpenseStatus.PENDING)
                .mapToDouble(expense -> tripFormatter.valueOrZero(expense.getAmount()))
                .sum();
        int approvedCount = (int) expenses.stream().filter(this::isApprovedExpense).count();
        int pendingCount = (int) expenses.stream()
                .filter(expense -> expense.getStatus() == ExpenseStatus.PENDING)
                .count();
        int rejectedCount = (int) expenses.stream()
                .filter(expense -> expense.getStatus() == ExpenseStatus.REJECTED)
                .count();

        return ExpenseSummaryDto.builder()
                .approvedAmount(approvedAmount)
                .pendingAmount(pendingAmount)
                .approvedCount(approvedCount)
                .pendingCount(pendingCount)
                .rejectedCount(rejectedCount)
                .totalCount(expenses.size())
                .build();
    }

    public boolean isApprovedExpense(Expense expense) {
        return expense.getStatus() == null || expense.getStatus() == ExpenseStatus.APPROVED;
    }
}
