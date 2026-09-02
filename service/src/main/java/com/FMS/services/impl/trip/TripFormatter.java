package com.FMS.services.impl.trip;

import com.FMS.entity.Contract;
import com.FMS.entity.Customer;
import com.FMS.entity.Driver;
import com.FMS.entity.Vehicle;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class TripFormatter {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public boolean positive(Double value) {
        return value != null && Double.isFinite(value) && value > 0;
    }

    public double valueOrZero(Double value) {
        return value == null ? 0D : value;
    }

    public String formatDate(LocalDate value) {
        return value == null ? "-" : value.format(DATE_FORMAT);
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMAT);
    }

    public String formatAmount(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "0";
        }
        return decimalFormat("#,##0").format(value);
    }

    public String formatNumber(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "-";
        }
        return decimalFormat("#,##0.##").format(value);
    }

    private DecimalFormat decimalFormat(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
    }

    public String customerDisplayName(Customer customer) {
        if (customer == null) {
            return "-";
        }
        return hasText(customer.getName()) ? customer.getName().trim() : customer.getId();
    }

    public String contractDisplayCode(Contract contract) {
        if (contract == null) {
            return "-";
        }
        return hasText(contract.getContractCode()) ? contract.getContractCode().trim() : contract.getId();
    }

    public String vehicleDisplayName(Vehicle vehicle) {
        if (vehicle == null) {
            return "-";
        }
        return hasText(vehicle.getLicensePlate()) ? vehicle.getLicensePlate().trim() : vehicle.getId();
    }

    public String driverDisplayName(Driver driver) {
        if (driver == null) {
            return "-";
        }
        return hasText(driver.getName()) ? driver.getName().trim() : driver.getId();
    }

    public String tripStatusLabel(TripStatus status) {
        if (status == null) {
            return "Chưa xác định";
        }
        return switch (status) {
            case CREATED -> "Mới tạo";
            case ASSIGNED -> "Đã phân công";
            case IN_PROGRESS -> "Đang vận chuyển";
            case COMPLETED -> "Hoàn tất";
            case CANCELLED -> "Đã hủy";
        };
    }

    public String vehicleStatusLabel(VehicleStatus status) {
        if (status == null) {
            return "Chưa xác định";
        }
        return switch (status) {
            case AVAILABLE -> "Sẵn sàng";
            case IN_TRIP -> "Đang chạy";
            case MAINTENANCE -> "Bảo dưỡng";
            case INACTIVE -> "Ngừng hoạt động";
        };
    }
}
