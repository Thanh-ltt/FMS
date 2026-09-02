package com.FMS.controllers;

import com.FMS.dto.ExpenseDto;
import com.FMS.dto.TripDto;
import com.FMS.dto.TripReadinessDto;
import com.FMS.dto.request.TripCreationRequest;
import com.FMS.entity.Trip;
import com.FMS.entity.User;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.response.ApiResponse;
import com.FMS.services.impl.TripServiceImpl;
import com.FMS.dto.TripReportDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripController {

    TripServiceImpl tripServiceImpl;

    @PostMapping
    ApiResponse<TripDto> create(@RequestBody @Valid TripCreationRequest request) {
        return ApiResponse.<TripDto>builder()
                .result(tripServiceImpl.createTrip(request))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<TripDto> update(@PathVariable String id, @RequestBody @Valid Trip request) {
        return ApiResponse.<TripDto>builder()
                .result(tripServiceImpl.updateTrip(id, request))
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<TripDto> getById(@PathVariable String id) {
        return ApiResponse.<TripDto>builder()
                .result(tripServiceImpl.getTripById(id))
                .build();
    }

    @GetMapping
    ApiResponse<List<TripDto>> getAll() {
        return ApiResponse.<List<TripDto>>builder()
                .result(tripServiceImpl.getAllTrips())
                .build();
    }

    @GetMapping("/my")
    ApiResponse<List<TripDto>> getMyTrips(Authentication authentication) {
        return ApiResponse.<List<TripDto>>builder()
                .result(tripServiceImpl.getMyTrips(currentUser(authentication)))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> delete(@PathVariable String id) {
        tripServiceImpl.deleteTrip(id);
        return ApiResponse.<String>builder()
                .result("Trip deleted successfully")
                .build();
    }

    @GetMapping("/status/{status}")
    ApiResponse<List<TripDto>> getByStatus(@PathVariable TripStatus status) {
        return ApiResponse.<List<TripDto>>builder()
                .result(tripServiceImpl.getByStatus(status))
                .build();
    }

    @GetMapping("/driver/{driverId}")
    ApiResponse<List<TripDto>> getByDriver(@PathVariable String driverId) {
        return ApiResponse.<List<TripDto>>builder()
                .result(tripServiceImpl.getByDriver(driverId))
                .build();
    }

    @GetMapping("/vehicle/{vehicleId}")
    ApiResponse<List<TripDto>> getByVehicle(@PathVariable String vehicleId) {
        return ApiResponse.<List<TripDto>>builder()
                .result(tripServiceImpl.getByVehicle(vehicleId))
                .build();
    }

    @GetMapping("/customer/{customerId}")
    ApiResponse<List<TripDto>> getByCustomer(@PathVariable String customerId) {
        return ApiResponse.<List<TripDto>>builder()
                .result(tripServiceImpl.getByCustomer(customerId))
                .build();
    }

    @PatchMapping("/{id}/start")
    ApiResponse<String> startTrip(@PathVariable String id) {
        tripServiceImpl.startTrip(id);
        return ApiResponse.<String>builder()
                .result("Trip started successfully")
                .build();
    }

    @GetMapping("/{id}/readiness")
    ApiResponse<TripReadinessDto> getStartReadiness(@PathVariable String id) {
        return ApiResponse.<TripReadinessDto>builder()
                .result(tripServiceImpl.getStartReadiness(id))
                .build();
    }

    @PatchMapping("/{id}/complete")
    ApiResponse<String> completeTrip(@PathVariable String id) {
        tripServiceImpl.completeTrip(id);
        return ApiResponse.<String>builder()
                .result("Trip completed successfully")
                .build();
    }

    @PatchMapping("/{id}/cancel")
    ApiResponse<String> cancelTrip(@PathVariable String id) {
        tripServiceImpl.cancelTrip(id);
        return ApiResponse.<String>builder()
                .result("Trip cancelled successfully")
                .build();
    }

    @GetMapping("/{id}/report")
    public void exportReport(@PathVariable String id, HttpServletResponse response) 
            throws IOException {
        TripDto trip = tripServiceImpl.getTripById(id);
        TripReportDto report = tripServiceImpl.generateReport(id);

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Bao cao chuyen");
        int rowIdx = 0;

        Row title = sheet.createRow(rowIdx++);
        title.createCell(0).setCellValue("Báo cáo chuyến đi");
        rowIdx++;

        rowIdx = writePair(sheet, rowIdx, "Mã chuyến", trip.getId());
        rowIdx = writePair(sheet, rowIdx, "Trạng thái", valueOf(trip.getStatus()));
        rowIdx = writePair(sheet, rowIdx, "Xe", trip.getVehiclePlate());
        rowIdx = writePair(sheet, rowIdx, "Tài xế", trip.getDriverName());
        rowIdx = writePair(sheet, rowIdx, "Khách hàng", trip.getCustomerName());
        rowIdx = writePair(sheet, rowIdx, "Hợp đồng", trip.getContractCode());
        rowIdx = writePair(sheet, rowIdx, "Điểm đi", trip.getStartLocation());
        rowIdx = writePair(sheet, rowIdx, "Điểm đến", trip.getEndLocation());
        rowIdx = writePair(sheet, rowIdx, "Quãng đường (km)", trip.getDistanceKm());
        rowIdx = writePair(sheet, rowIdx, "Trọng lượng hàng (tấn)", trip.getCargoWeightTon());
        rowIdx = writePair(sheet, rowIdx, "Đơn giá (VNĐ/tấn/km)", trip.getFreightRatePerTonKm());
        rowIdx = writePair(sheet, rowIdx, "Cước vận chuyển dự kiến", trip.getFreightAmount());
        rowIdx = writePair(sheet, rowIdx, "Thời gian bắt đầu", trip.getStartTime());
        rowIdx = writePair(sheet, rowIdx, "Thời gian kết thúc", trip.getEndTime());
        rowIdx++;

        rowIdx = writePair(sheet, rowIdx, "Tổng doanh thu", report.getTotalRevenue());
        rowIdx = writePair(sheet, rowIdx, "Tổng chi phí", report.getTotalExpense());
        rowIdx = writePair(sheet, rowIdx, "Lợi nhuận", report.getTotalRevenue() - report.getTotalExpense());
        rowIdx++;

        Row invoiceHeader = sheet.createRow(rowIdx++);
        invoiceHeader.createCell(0).setCellValue("Hóa đơn");
        Row invoiceColumns = sheet.createRow(rowIdx++);
        invoiceColumns.createCell(0).setCellValue("Số hóa đơn");
        invoiceColumns.createCell(1).setCellValue("Trạng thái");
        invoiceColumns.createCell(2).setCellValue("Ngày lập");
        invoiceColumns.createCell(3).setCellValue("Hạn thanh toán");
        invoiceColumns.createCell(4).setCellValue("Số tiền");

        for (var invoice : report.getInvoices()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(valueOf(invoice.getInvoiceNumber()));
            row.createCell(1).setCellValue(valueOf(invoice.getStatus()));
            row.createCell(2).setCellValue(valueOf(invoice.getIssueDate()));
            row.createCell(3).setCellValue(valueOf(invoice.getDueDate()));
            row.createCell(4).setCellValue(numberOf(invoice.getTotalAmount()));
        }
        rowIdx++;

        Row expenseHeader = sheet.createRow(rowIdx++);
        expenseHeader.createCell(0).setCellValue("Chi phí");
        Row expenseColumns = sheet.createRow(rowIdx++);
        expenseColumns.createCell(0).setCellValue("Loại chi phí");
        expenseColumns.createCell(1).setCellValue("Ngày chi");
        expenseColumns.createCell(2).setCellValue("Số tiền");
        expenseColumns.createCell(3).setCellValue("Ghi chú");

        for (var expense : report.getExpenses()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(expenseTypesOf(expense));
            row.createCell(1).setCellValue(valueOf(expense.getExpenseDate()));
            row.createCell(2).setCellValue(numberOf(expense.getAmount()));
            row.createCell(3).setCellValue(valueOf(expense.getDescription()));
        }

        for (int i = 0; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=trip_" + id + "_report.xlsx");
        wb.write(response.getOutputStream());
        wb.close();
    }

    private int writePair(Sheet sheet, int rowIdx, String label, Object value) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(label);
        if (value instanceof Number number) {
            row.createCell(1).setCellValue(number.doubleValue());
        } else {
            row.createCell(1).setCellValue(valueOf(value));
        }
        return rowIdx;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    private String valueOf(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String expenseTypesOf(ExpenseDto expense) {
        if (expense.getExpenseTypes() != null && !expense.getExpenseTypes().isEmpty()) {
            return String.join(", ", expense.getExpenseTypes().stream().map(Enum::name).toList());
        }
        return valueOf(expense.getExpenseType());
    }

    private double numberOf(Number value) {
        return value == null ? 0 : value.doubleValue();
    }

}
