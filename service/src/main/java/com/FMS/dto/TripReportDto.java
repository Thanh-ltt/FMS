package com.FMS.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Data transfer object representing a revenue and expense report for a trip.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripReportDto {
    /** Total revenue from invoices */
    private Double totalRevenue;
    /** Total expense amount */
    private Double totalExpense;
    /** List of invoice details (optional) */
    private List<InvoiceDto> invoices;
    /** List of expense details (optional) */
    private List<ExpenseDto> expenses;
}
