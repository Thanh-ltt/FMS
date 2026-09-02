package com.FMS.services.impl.trip;

import com.FMS.dto.DepositSummaryDto;
import com.FMS.dto.TripReadinessCheckDto;
import com.FMS.dto.TripReadinessDto;
import com.FMS.entity.Contract;
import com.FMS.entity.Customer;
import com.FMS.entity.Driver;
import com.FMS.entity.Trip;
import com.FMS.entity.Vehicle;
import com.FMS.enums.ContractStatus;
import com.FMS.enums.TripReadinessCheckStatus;
import com.FMS.enums.TripStatus;
import com.FMS.enums.VehicleStatus;
import com.FMS.exception.ErrorCode;
import com.FMS.services.DepositService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripReadinessAssessor {
    static final double EPSILON = 0.000001D;
    static final int START_EARLY_TOLERANCE_MINUTES = 30;

    DepositService depositService;
    TripValidator tripValidator;
    TripFormatter tripFormatter;

    public record AssessedCheck(TripReadinessCheckDto check, ErrorCode blocker) {
    }

    public record ReadinessAssessment(TripReadinessDto result, ErrorCode firstBlocker) {
    }

    public ReadinessAssessment assessStartReadiness(Trip trip) {
        List<AssessedCheck> assessedChecks = new ArrayList<>();
        LocalDateTime checkedAt = LocalDateTime.now();
        LocalDate today = checkedAt.toLocalDate();

        boolean validTripStatus = trip.getStatus() == TripStatus.CREATED || trip.getStatus() == TripStatus.ASSIGNED;
        assessedChecks.add(validTripStatus
                ? passed("TRIP_STATUS", "TRIP", "Trạng thái chuyến",
                        "Chuyến đang ở trạng thái " + tripFormatter.tripStatusLabel(trip.getStatus()) + " và được phép khởi hành.")
                : blocked("TRIP_STATUS", "TRIP", "Trạng thái chuyến",
                        "Chuyến đang ở trạng thái " + tripFormatter.tripStatusLabel(trip.getStatus()) + ".",
                        "Chỉ chuyến Mới tạo hoặc Đã phân công mới có thể bắt đầu.",
                        ErrorCode.TRIP_CANNOT_START));

        TripValidator.TimeWindow tripWindow = tripValidator.getTimeWindow(trip.getStartTime(), trip.getEndTime(), false);
        boolean validTripWindow = tripWindow.isComplete();
        assessedChecks.add(validTripWindow
                ? passed("TRIP_SCHEDULE", "TRIP", "Lịch trình",
                        "Từ " + tripFormatter.formatDateTime(tripWindow.start()) + " đến " + tripFormatter.formatDateTime(tripWindow.end()) + ".")
                : blocked("TRIP_SCHEDULE", "TRIP", "Lịch trình",
                        "Ngày giờ bắt đầu hoặc kết thúc chưa hợp lệ.",
                        "Sửa chuyến và chọn thời gian kết thúc sau thời gian bắt đầu.",
                        ErrorCode.INVALID_TRIP_TIME));

        assessStartTimeCheck(assessedChecks, tripWindow, validTripWindow, checkedAt);

        boolean validRoute = tripFormatter.hasText(trip.getStartLocation()) && tripFormatter.hasText(trip.getEndLocation());
        assessedChecks.add(validRoute
                ? passed("ROUTE", "TRIP", "Tuyến vận chuyển",
                        trip.getStartLocation().trim() + " → " + trip.getEndLocation().trim())
                : blocked("ROUTE", "TRIP", "Tuyến vận chuyển",
                        "Điểm đi hoặc điểm đến đang để trống.",
                        "Sửa chuyến và nhập đầy đủ điểm đi, điểm đến.",
                        ErrorCode.INVALID_ROUTE_ADDRESS));

        boolean validFreight = tripFormatter.positive(trip.getDistanceKm())
                && tripFormatter.positive(trip.getCargoWeightTon())
                && tripFormatter.positive(trip.getFreightRatePerTonKm())
                && tripFormatter.positive(trip.getFreightAmount());
        assessedChecks.add(validFreight
                ? passed("FREIGHT", "TRIP", "Dữ liệu tính cước",
                        tripFormatter.formatNumber(trip.getDistanceKm()) + " km × "
                                + tripFormatter.formatNumber(trip.getCargoWeightTon()) + " tấn × "
                                + tripFormatter.formatAmount(trip.getFreightRatePerTonKm()) + " đ/tấn/km = "
                                + tripFormatter.formatAmount(trip.getFreightAmount()) + " đ.")
                : blocked("FREIGHT", "TRIP", "Dữ liệu tính cước",
                        "Quãng đường, trọng lượng, đơn giá hoặc thành tiền chưa hợp lệ.",
                        "Sửa chuyến và nhập các giá trị lớn hơn 0.",
                        ErrorCode.INVALID_FREIGHT_INPUT));

        Customer customer = trip.getCustomer();
        assessedChecks.add(customer != null
                ? passed("CUSTOMER", "TRIP", "Khách hàng",
                        "Chuyến đã liên kết khách hàng " + tripFormatter.customerDisplayName(customer) + ".")
                : blocked("CUSTOMER", "TRIP", "Khách hàng",
                        "Chuyến chưa liên kết khách hàng.",
                        "Sửa chuyến và chọn khách hàng vận chuyển.",
                        ErrorCode.CUSTOMER_NOT_FOUND));

        assessContractChecks(assessedChecks, trip, tripWindow, validTripWindow, today);
        assessVehicleChecks(assessedChecks, trip, tripWindow, validTripWindow, validFreight);
        assessDriverChecks(assessedChecks, trip, tripWindow, validTripWindow);

        DepositSummaryDto depositSummary = depositService.getSummaryForTrip(trip.getId());
        if (depositSummary == null) {
            depositSummary = DepositSummaryDto.builder().required(false).build();
        }
        boolean depositRequired = Boolean.TRUE.equals(depositSummary.getRequired());
        double depositShortfall = tripFormatter.valueOrZero(depositSummary.getShortfallAmount());
        if (!depositRequired) {
            assessedChecks.add(passed("DEPOSIT", "FINANCE", "Tiền cọc",
                    "Chuyến không yêu cầu tiền cọc trước khi khởi hành."));
        } else if (depositShortfall > EPSILON) {
            assessedChecks.add(blocked("DEPOSIT", "FINANCE", "Tiền cọc",
                    "Đã nhận " + tripFormatter.formatAmount(depositSummary.getReceivedAmount()) + " đ / yêu cầu "
                            + tripFormatter.formatAmount(depositSummary.getRequiredAmount()) + " đ; còn thiếu "
                            + tripFormatter.formatAmount(depositShortfall) + " đ.",
                    "Ghi nhận thêm " + tripFormatter.formatAmount(depositShortfall)
                            + " đ tiền cọc đúng hợp đồng hoặc chuyến này.",
                    ErrorCode.REQUIRED_DEPOSIT_NOT_RECEIVED));
        } else {
            assessedChecks.add(passed("DEPOSIT", "FINANCE", "Tiền cọc",
                    "Đã đáp ứng mức cọc " + tripFormatter.formatAmount(depositSummary.getRequiredAmount()) + " đ."));
        }

        int passedCount = countStatus(assessedChecks, TripReadinessCheckStatus.PASSED);
        int blockedCount = countStatus(assessedChecks, TripReadinessCheckStatus.BLOCKED);
        int waitingCount = countStatus(assessedChecks, TripReadinessCheckStatus.WAITING);
        int notApplicableCount = countStatus(assessedChecks, TripReadinessCheckStatus.NOT_APPLICABLE);
        AssessedCheck primaryBlocker = assessedChecks.stream()
                .filter(check -> check.check().getStatus() == TripReadinessCheckStatus.BLOCKED)
                .findFirst()
                .orElse(null);

        TripReadinessDto result = TripReadinessDto.builder()
                .tripId(trip.getId())
                .ready(blockedCount == 0 && waitingCount == 0)
                .checkedAt(checkedAt)
                .passedCount(passedCount)
                .blockedCount(blockedCount)
                .waitingCount(waitingCount)
                .notApplicableCount(notApplicableCount)
                .primaryBlockerCode(primaryBlocker == null || primaryBlocker.blocker() == null
                        ? null : primaryBlocker.blocker().name())
                .primaryBlockerMessage(primaryBlocker == null ? null : primaryBlocker.check().getMessage())
                .primaryResolution(primaryBlocker == null ? null : primaryBlocker.check().getResolution())
                .depositSummary(depositSummary)
                .checks(assessedChecks.stream().map(AssessedCheck::check).toList())
                .build();

        return new ReadinessAssessment(result, primaryBlocker == null ? null : primaryBlocker.blocker());
    }

    private void assessStartTimeCheck(
            List<AssessedCheck> checks,
            TripValidator.TimeWindow tripWindow,
            boolean validTripWindow,
            LocalDateTime checkedAt
    ) {
        if (!validTripWindow) {
            checks.add(waiting("TRIP_START_TIME", "TRIP", "Thời điểm khởi hành",
                    "Chưa thể kiểm tra do lịch trình chuyến chưa hợp lệ."));
            return;
        }

        LocalDateTime earliestStart = tripWindow.start().minusMinutes(START_EARLY_TOLERANCE_MINUTES);
        if (checkedAt.isBefore(earliestStart)) {
            checks.add(blocked("TRIP_START_TIME", "TRIP", "Thời điểm khởi hành",
                    "Chuyến dự kiến bắt đầu lúc " + tripFormatter.formatDateTime(tripWindow.start())
                            + "; được phép bắt đầu sớm nhất lúc " + tripFormatter.formatDateTime(earliestStart) + ".",
                    "Đợi đến " + tripFormatter.formatDateTime(earliestStart) + " hoặc sửa lại lịch chuyến nếu kế hoạch đã thay đổi.",
                    ErrorCode.TRIP_START_TOO_EARLY));
            return;
        }

        if (checkedAt.isAfter(tripWindow.end())) {
            checks.add(blocked("TRIP_START_TIME", "TRIP", "Thời điểm khởi hành",
                    "Chuyến đã quá giờ kết thúc dự kiến " + tripFormatter.formatDateTime(tripWindow.end()) + ".",
                    "Điều chỉnh lại lịch chuyến trước khi khởi hành.",
                    ErrorCode.TRIP_START_WINDOW_EXPIRED));
            return;
        }

        checks.add(passed("TRIP_START_TIME", "TRIP", "Thời điểm khởi hành",
                "Đang trong khung được phép bắt đầu chuyến (sớm tối đa "
                        + START_EARLY_TOLERANCE_MINUTES + " phút)."));
    }

    private void assessContractChecks(
            List<AssessedCheck> checks,
            Trip trip,
            TripValidator.TimeWindow tripWindow,
            boolean validTripWindow,
            LocalDate today
    ) {
        Contract contract = trip.getContract();
        if (contract == null) {
            checks.add(notApplicable("CONTRACT_STATUS", "CONTRACT", "Trạng thái hợp đồng",
                    "Chuyến không gắn hợp đồng."));
            checks.add(notApplicable("CONTRACT_CUSTOMER", "CONTRACT", "Khách hàng hợp đồng",
                    "Không áp dụng vì chuyến không gắn hợp đồng."));
            checks.add(notApplicable("CONTRACT_SCHEDULE", "CONTRACT", "Lịch trong thời hạn hợp đồng",
                    "Không áp dụng vì chuyến không gắn hợp đồng."));
            checks.add(notApplicable("CONTRACT_CURRENT_DATE", "CONTRACT", "Hiệu lực tại ngày khởi hành",
                    "Không áp dụng vì chuyến không gắn hợp đồng."));
            return;
        }

        boolean active = contract.getStatus() == ContractStatus.ACTIVE;
        checks.add(active
                ? passed("CONTRACT_STATUS", "CONTRACT", "Trạng thái hợp đồng",
                        "Hợp đồng " + tripFormatter.contractDisplayCode(contract) + " đang hiệu lực.")
                : blocked("CONTRACT_STATUS", "CONTRACT", "Trạng thái hợp đồng",
                        "Hợp đồng " + tripFormatter.contractDisplayCode(contract) + " đang ở trạng thái "
                                + contract.getStatus() + ".",
                        "Kích hoạt hợp đồng trước khi bắt đầu chuyến.",
                        ErrorCode.CONTRACT_NOT_ACTIVE));

        boolean customerMatches = contract.getCustomer() != null
                && trip.getCustomer() != null
                && Objects.equals(contract.getCustomer().getId(), trip.getCustomer().getId());
        checks.add(customerMatches
                ? passed("CONTRACT_CUSTOMER", "CONTRACT", "Khách hàng hợp đồng",
                        "Khách hàng của chuyến khớp với hợp đồng.")
                : blocked("CONTRACT_CUSTOMER", "CONTRACT", "Khách hàng hợp đồng",
                        "Khách hàng của chuyến không khớp với hợp đồng.",
                        "Sửa hoặc tạo lại chuyến với đúng khách hàng của hợp đồng.",
                        ErrorCode.CONTRACT_CUSTOMER_MISMATCH));

        boolean contractDatesAvailable = contract.getStartDate() != null && contract.getEndDate() != null;
        if (!validTripWindow) {
            checks.add(waiting("CONTRACT_SCHEDULE", "CONTRACT", "Lịch trong thời hạn hợp đồng",
                    "Chưa thể kiểm tra do lịch trình chuyến chưa hợp lệ."));
        } else {
            boolean scheduleWithinContract = contractDatesAvailable
                    && !tripWindow.start().toLocalDate().isBefore(contract.getStartDate())
                    && !tripWindow.end().toLocalDate().isAfter(contract.getEndDate());
            checks.add(scheduleWithinContract
                    ? passed("CONTRACT_SCHEDULE", "CONTRACT", "Lịch trong thời hạn hợp đồng",
                            "Toàn bộ chuyến nằm trong thời hạn " + tripFormatter.formatDate(contract.getStartDate())
                                    + " – " + tripFormatter.formatDate(contract.getEndDate()) + ".")
                    : blocked("CONTRACT_SCHEDULE", "CONTRACT", "Lịch trong thời hạn hợp đồng",
                            "Lịch chuyến nằm ngoài thời hạn của hợp đồng.",
                            "Đổi lịch chuyến hoặc sử dụng hợp đồng có thời hạn phù hợp.",
                            ErrorCode.CONTRACT_OUTSIDE_VALIDITY));
        }

        boolean currentDateWithinContract = contractDatesAvailable
                && !today.isBefore(contract.getStartDate())
                && !today.isAfter(contract.getEndDate());
        if (currentDateWithinContract) {
            checks.add(passed("CONTRACT_CURRENT_DATE", "CONTRACT", "Hiệu lực tại ngày khởi hành",
                    "Hôm nay " + tripFormatter.formatDate(today) + " nằm trong thời hạn hợp đồng."));
        } else {
            String resolution = contract.getStartDate() != null && today.isBefore(contract.getStartDate())
                    ? "Đợi đến ngày " + tripFormatter.formatDate(contract.getStartDate()) + " hoặc dùng hợp đồng đã có hiệu lực."
                    : "Sử dụng hợp đồng khác còn hiệu lực tại ngày khởi hành.";
            checks.add(blocked("CONTRACT_CURRENT_DATE", "CONTRACT", "Hiệu lực tại ngày khởi hành",
                    "Hôm nay " + tripFormatter.formatDate(today) + " không nằm trong thời hạn "
                            + tripFormatter.formatDate(contract.getStartDate()) + " – " + tripFormatter.formatDate(contract.getEndDate()) + ".",
                    resolution,
                    ErrorCode.CONTRACT_OUTSIDE_VALIDITY));
        }
    }

    private void assessVehicleChecks(
            List<AssessedCheck> checks,
            Trip trip,
            TripValidator.TimeWindow tripWindow,
            boolean validTripWindow,
            boolean validFreight
    ) {
        Vehicle vehicle = trip.getVehicle();
        boolean vehicleAvailable = vehicle != null && vehicle.getStatus() == VehicleStatus.AVAILABLE;
        checks.add(vehicleAvailable
                ? passed("VEHICLE_STATUS", "VEHICLE", "Trạng thái phương tiện",
                        "Xe " + tripFormatter.vehicleDisplayName(vehicle) + " đang Sẵn sàng.")
                : blocked("VEHICLE_STATUS", "VEHICLE", "Trạng thái phương tiện",
                        vehicle == null
                                ? "Chuyến chưa được gán phương tiện."
                                : "Xe " + tripFormatter.vehicleDisplayName(vehicle) + " đang ở trạng thái "
                                        + tripFormatter.vehicleStatusLabel(vehicle.getStatus()) + ".",
                        "Chọn một xe đang ở trạng thái Sẵn sàng.",
                        ErrorCode.VEHICLE_NOT_AVAILABLE));

        if (vehicle == null || !validFreight) {
            checks.add(waiting("VEHICLE_CAPACITY", "VEHICLE", "Tải trọng phương tiện",
                    "Chưa thể kiểm tra do thiếu xe hoặc trọng lượng hàng hợp lệ."));
        } else {
            boolean validCapacity = tripFormatter.positive(vehicle.getCapacity());
            boolean withinCapacity = validCapacity && trip.getCargoWeightTon() <= vehicle.getCapacity() + EPSILON;
            if (!validCapacity) {
                checks.add(blocked("VEHICLE_CAPACITY", "VEHICLE", "Tải trọng phương tiện",
                        "Xe chưa có tải trọng hợp lệ.",
                        "Cập nhật tải trọng xe trước khi khởi hành.",
                        ErrorCode.INVALID_VEHICLE_INPUT));
            } else if (!withinCapacity) {
                checks.add(blocked("VEHICLE_CAPACITY", "VEHICLE", "Tải trọng phương tiện",
                        "Hàng nặng " + tripFormatter.formatNumber(trip.getCargoWeightTon()) + " tấn, vượt tải trọng "
                                + tripFormatter.formatNumber(vehicle.getCapacity()) + " tấn của xe.",
                        "Giảm tải hoặc chọn xe có tải trọng phù hợp.",
                        ErrorCode.VEHICLE_CAPACITY_EXCEEDED));
            } else {
                checks.add(passed("VEHICLE_CAPACITY", "VEHICLE", "Tải trọng phương tiện",
                        "Hàng " + tripFormatter.formatNumber(trip.getCargoWeightTon()) + " tấn / xe "
                                + tripFormatter.formatNumber(vehicle.getCapacity()) + " tấn."));
            }
        }

        if (vehicle == null || !validTripWindow) {
            checks.add(waiting("VEHICLE_SCHEDULE", "VEHICLE", "Lịch sử dụng phương tiện",
                    "Chưa thể kiểm tra do thiếu xe hoặc lịch trình hợp lệ."));
        } else {
            boolean conflict = tripValidator.hasVehicleScheduleConflict(vehicle.getId(), tripWindow, trip.getId());
            checks.add(conflict
                    ? blocked("VEHICLE_SCHEDULE", "VEHICLE", "Lịch sử dụng phương tiện",
                            "Xe có chuyến khác trùng khoảng thời gian này.",
                            "Đổi xe hoặc điều chỉnh lịch để không bị chồng thời gian.",
                            ErrorCode.VEHICLE_HAS_ACTIVE_TRIP)
                    : passed("VEHICLE_SCHEDULE", "VEHICLE", "Lịch sử dụng phương tiện",
                            "Không có chuyến khác trùng lịch xe."));
        }
    }

    private void assessDriverChecks(
            List<AssessedCheck> checks,
            Trip trip,
            TripValidator.TimeWindow tripWindow,
            boolean validTripWindow
    ) {
        Driver driver = trip.getDriver();
        checks.add(driver != null
                ? passed("DRIVER_ASSIGNED", "DRIVER", "Phân công tài xế",
                        "Đã phân công " + tripFormatter.driverDisplayName(driver) + ".")
                : blocked("DRIVER_ASSIGNED", "DRIVER", "Phân công tài xế",
                        "Chuyến chưa được phân công tài xế.",
                        "Sửa chuyến và chọn tài xế.",
                        ErrorCode.DRIVER_NOT_FOUND));

        if (driver == null || !validTripWindow) {
            checks.add(waiting("DRIVER_LICENSE", "DRIVER", "Giấy phép lái xe",
                    "Chưa thể kiểm tra do thiếu tài xế hoặc lịch trình hợp lệ."));
        } else {
            LocalDate requiredThrough = tripWindow.end().toLocalDate();
            boolean licenseValid = driver.getLicenseExpiration() != null
                    && !driver.getLicenseExpiration().isBefore(requiredThrough);
            checks.add(licenseValid
                    ? passed("DRIVER_LICENSE", "DRIVER", "Giấy phép lái xe",
                            "Giấy phép còn hạn đến " + tripFormatter.formatDate(driver.getLicenseExpiration()) + ".")
                    : blocked("DRIVER_LICENSE", "DRIVER", "Giấy phép lái xe",
                            "Giấy phép không còn hiệu lực đến hết ngày " + tripFormatter.formatDate(requiredThrough) + ".",
                            "Gia hạn giấy phép hoặc phân công tài xế có giấy phép còn hiệu lực suốt chuyến.",
                            ErrorCode.DRIVER_LICENSE_EXPIRED));
        }

        if (driver == null || !validTripWindow) {
            checks.add(waiting("DRIVER_SCHEDULE", "DRIVER", "Lịch làm việc tài xế",
                    "Chưa thể kiểm tra do thiếu tài xế hoặc lịch trình hợp lệ."));
        } else {
            boolean conflict = tripValidator.hasDriverScheduleConflict(driver.getId(), tripWindow, trip.getId());
            checks.add(conflict
                    ? blocked("DRIVER_SCHEDULE", "DRIVER", "Lịch làm việc tài xế",
                            "Tài xế có chuyến khác trùng khoảng thời gian này.",
                            "Đổi tài xế hoặc điều chỉnh lịch để không bị chồng thời gian.",
                            ErrorCode.DRIVER_HAS_ACTIVE_TRIP)
                    : passed("DRIVER_SCHEDULE", "DRIVER", "Lịch làm việc tài xế",
                            "Không có chuyến khác trùng lịch tài xế."));
        }
    }

    private AssessedCheck passed(String key, String group, String label, String message) {
        return assessedCheck(key, group, label, TripReadinessCheckStatus.PASSED, message, null, null);
    }

    private AssessedCheck blocked(
            String key,
            String group,
            String label,
            String message,
            String resolution,
            ErrorCode blocker
    ) {
        return assessedCheck(key, group, label, TripReadinessCheckStatus.BLOCKED, message, resolution, blocker);
    }

    private AssessedCheck waiting(String key, String group, String label, String message) {
        return assessedCheck(key, group, label, TripReadinessCheckStatus.WAITING, message, null, null);
    }

    private AssessedCheck notApplicable(String key, String group, String label, String message) {
        return assessedCheck(key, group, label, TripReadinessCheckStatus.NOT_APPLICABLE, message, null, null);
    }

    private AssessedCheck assessedCheck(
            String key,
            String group,
            String label,
            TripReadinessCheckStatus status,
            String message,
            String resolution,
            ErrorCode blocker
    ) {
        return new AssessedCheck(TripReadinessCheckDto.builder()
                .key(key)
                .group(group)
                .label(label)
                .status(status)
                .message(message)
                .resolution(resolution)
                .build(), blocker);
    }

    private int countStatus(List<AssessedCheck> checks, TripReadinessCheckStatus status) {
        return (int) checks.stream().filter(check -> check.check().getStatus() == status).count();
    }
}
