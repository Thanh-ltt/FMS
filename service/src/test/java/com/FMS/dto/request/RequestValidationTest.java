package com.FMS.dto.request;

import com.FMS.entity.Vehicle;
import com.FMS.enums.ExpenseType;
import com.FMS.enums.PaymentMethod;
import com.FMS.enums.VehicleStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void customerProfile_requiresDateOfBirthAndValidContactDetails() {
        CustomerAccountCreationRequest request = CustomerAccountCreationRequest.builder()
                .name("Nguyen Van An")
                .phone("khong-phai-so-dien-thoai")
                .idNumber("123")
                .dob(null)
                .address("123 Nguyen Trai, TP.HCM")
                .username("customer01")
                .password("password123")
                .build();

        assertThat(messagesOf(request))
                .contains("INVALID_PHONE", "INVALID_ID_NUMBER", "INVALID_DATE_OF_BIRTH");
    }

    @Test
    void customerProfile_acceptsFormattedVietnamesePhone() {
        CustomerAccountCreationRequest request = CustomerAccountCreationRequest.builder()
                .name("Nguyen Van An")
                .phone("090 123 4567")
                .idNumber("079123456789")
                .dob(LocalDate.now().minusYears(30))
                .address("123 Nguyen Trai, TP.HCM")
                .username("customer01")
                .password("password123")
                .build();

        assertThat(messagesOf(request)).isEmpty();
    }

    @Test
    void expense_rejectsZeroAmountAndFutureDate() {
        ExpenseCreationRequest request = ExpenseCreationRequest.builder()
                .tripId("trip-1")
                .expenseTypes(List.of(ExpenseType.FUEL))
                .amount(0D)
                .expenseDate(LocalDate.now().plusDays(1))
                .build();

        assertThat(messagesOf(request)).contains("INVALID_EXPENSE_INPUT");
    }

    @Test
    void maintenance_requiresMaintenanceDate() {
        MaintenanceCreationRequest request = MaintenanceCreationRequest.builder()
                .vehicleId("vehicle-1")
                .maintenanceTypes(List.of("PERIODIC"))
                .cost(0D)
                .build();

        assertThat(messagesOf(request)).contains("INVALID_MAINTENANCE_DATE");
    }

    @Test
    void vehicle_rejectsInvalidPlateAndNonPositiveCapacity() {
        Vehicle vehicle = Vehicle.builder()
                .licensePlate("ABC")
                .vehicleType("CARGO_TRUCK")
                .capacity(0D)
                .status(VehicleStatus.AVAILABLE)
                .build();

        assertThat(messagesOf(vehicle)).contains("INVALID_VEHICLE_INPUT");
    }

    @Test
    void trip_requiresLocationsTimesAndPositiveFreightValues() {
        TripCreationRequest request = TripCreationRequest.builder()
                .vehicleId("vehicle-1")
                .driverId("driver-1")
                .customerId("customer-1")
                .startLocation(" ")
                .endLocation(" ")
                .startTime("")
                .endTime("")
                .distanceKm(0D)
                .cargoWeightTon(-1D)
                .freightRatePerTonKm(0D)
                .build();

        assertThat(messagesOf(request))
                .contains("INVALID_ROUTE_ADDRESS", "INVALID_TRIP_TIME", "INVALID_FREIGHT_INPUT");
    }

    @Test
    void contract_rejectsFutureSignatureAndZeroFreightRate() {
        ContractCreationRequest request = ContractCreationRequest.builder()
                .contractCode("HD-001")
                .customerId("customer-1")
                .signedDate(LocalDate.now().plusDays(1))
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(10))
                .cargoDescription("Hang kho")
                .cargoType("GENERAL")
                .freightRatePerTonKm(0D)
                .build();

        assertThat(messagesOf(request))
                .contains("INVALID_CONTRACT_DATE", "INVALID_FREIGHT_INPUT");
    }

    @Test
    void invoice_rejectsFutureIssueDateAndZeroAmount() {
        InvoiceCreationRequest request = InvoiceCreationRequest.builder()
                .tripId("trip-1")
                .totalAmount(0D)
                .issueDate(LocalDate.now().plusDays(1))
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        assertThat(messagesOf(request))
                .contains("INVALID_INVOICE_DATE", "INVALID_INVOICE_AMOUNT");
    }

    @Test
    void deposit_rejectsFutureReceivedDate() {
        DepositCreationRequest request = DepositCreationRequest.builder()
                .customerId("customer-1")
                .amount(100_000D)
                .receivedDate(LocalDate.now().plusDays(1))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThat(messagesOf(request)).contains("INVALID_DEPOSIT_DATE");
    }

    @Test
    void driverAccountProvision_requiresValidUsernameAndPassword() {
        DriverAccountProvisionRequest request = DriverAccountProvisionRequest.builder()
                .username("abc")
                .password("123")
                .build();

        assertThat(messagesOf(request)).contains("INVALID_USERNAME", "INVALID_PASSWORD");
    }

    private static Set<String> messagesOf(Object value) {
        return validator.validate(value).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
