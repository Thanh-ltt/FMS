package com.FMS.services.impl;

import com.FMS.dto.ExpenseDto;
import com.FMS.dto.request.ExpenseCreationRequest;
import com.FMS.dto.request.ExpenseReviewRequest;
import com.FMS.entity.Driver;
import com.FMS.entity.Expense;
import com.FMS.entity.Trip;
import com.FMS.entity.User;
import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.ExpenseType;
import com.FMS.enums.Role;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.ExpenseMapper;
import com.FMS.repositories.DriverRepository;
import com.FMS.repositories.ExpenseRepository;
import com.FMS.repositories.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {
    @Mock private ExpenseRepository expenseRepository;
    @Mock private TripRepository tripRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private ExpenseMapper expenseMapper;

    private ExpenseServiceImpl expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseServiceImpl(
                expenseRepository,
                tripRepository,
                driverRepository,
                expenseMapper
        );
    }

    @Test
    void createExpense_savesPendingExpenseWithAuditInformation() {
        Trip trip = activeTrip();
        User actor = managerUser();
        ExpenseCreationRequest request = requestFor(trip);
        request.setExpenseTypes(List.of(ExpenseType.FUEL, ExpenseType.TOLL));
        request.setReceiptImageUrl("data:image/png;base64,AAAA");

        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDto(any(Expense.class))).thenReturn(ExpenseDto.builder().build());

        expenseService.createExpense(request, actor);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();
        assertThat(saved.getExpenseTypes()).containsExactly(ExpenseType.FUEL, ExpenseType.TOLL);
        assertThat(saved.getExpenseType()).isEqualTo(ExpenseType.FUEL);
        assertThat(saved.getAmount()).isEqualTo(500_000D);
        assertThat(saved.getStatus()).isEqualTo(ExpenseStatus.PENDING);
        assertThat(saved.getRecordedByUserId()).isEqualTo("manager-user");
        assertThat(saved.getRecordedByName()).isEqualTo("Quản lý vận hành");
        assertThat(saved.getRecordedByRole()).isEqualTo(Role.MANAGER);
        assertThat(saved.getReceiptImageUrl()).startsWith("data:image/png;base64,");
    }

    @Test
    void createExpense_allowsAssignedDriver() {
        Trip trip = activeTrip();
        User actor = driverUser();
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(driverRepository.findByUserId("driver-user")).thenReturn(Optional.of(trip.getDriver()));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDto(any(Expense.class))).thenReturn(ExpenseDto.builder().build());

        expenseService.createExpense(requestFor(trip), actor);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getRecordedByName()).isEqualTo("Tài xế Minh");
        assertThat(captor.getValue().getRecordedByRole()).isEqualTo(Role.DRIVER);
    }

    @Test
    void createExpense_rejectsDriverAssignedToAnotherTrip() {
        Trip trip = activeTrip();
        User actor = driverUser();
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(driverRepository.findByUserId("driver-user")).thenReturn(Optional.of(
                Driver.builder().id("driver-2").build()
        ));

        assertThatThrownBy(() -> expenseService.createExpense(requestFor(trip), actor))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPENSE_TRIP_ACCESS_DENIED);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_rejectsTripThatHasNotStarted() {
        Trip trip = activeTrip();
        trip.setStatus(TripStatus.ASSIGNED);
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> expenseService.createExpense(requestFor(trip), managerUser()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPENSE_TRIP_STATUS_INVALID);
    }

    @Test
    void createExpense_rejectsDateOutsideTripSchedule() {
        Trip trip = activeTrip();
        ExpenseCreationRequest request = requestFor(trip);
        request.setExpenseDate(LocalDate.now().minusDays(10));
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> expenseService.createExpense(request, managerUser()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPENSE_DATE_OUTSIDE_TRIP);
    }

    @Test
    void reviewExpense_approvesPendingExpenseAndStoresReviewer() {
        Expense expense = Expense.builder()
                .id("expense-1")
                .trip(activeTrip())
                .status(ExpenseStatus.PENDING)
                .build();
        User reviewer = User.builder()
                .id("accountant-user")
                .username("accountant01")
                .fullName("Kế toán Lan")
                .role(Role.ACCOUNTANT)
                .build();
        when(expenseRepository.findById("expense-1")).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);
        when(expenseMapper.toDto(expense)).thenReturn(ExpenseDto.builder().build());

        expenseService.reviewExpense(
                "expense-1",
                ExpenseStatus.APPROVED,
                ExpenseReviewRequest.builder().reviewNote("Chứng từ hợp lệ").build(),
                reviewer
        );

        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
        assertThat(expense.getReviewedByName()).isEqualTo("Kế toán Lan");
        assertThat(expense.getReviewedAt()).isNotNull();
        assertThat(expense.getReviewNote()).isEqualTo("Chứng từ hợp lệ");
    }

    @Test
    void reviewExpense_requiresReasonWhenRejected() {
        Expense expense = Expense.builder()
                .id("expense-1")
                .trip(activeTrip())
                .status(ExpenseStatus.PENDING)
                .build();
        when(expenseRepository.findById("expense-1")).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.reviewExpense(
                "expense-1",
                ExpenseStatus.REJECTED,
                ExpenseReviewRequest.builder().build(),
                managerUser()
        ))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPENSE_REJECTION_NOTE_REQUIRED);
    }

    @Test
    void calculateTripExpense_countsOnlyApprovedAndLegacyExpenses() {
        Trip trip = activeTrip();
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(expenseRepository.findByTripId("trip-1")).thenReturn(List.of(
                Expense.builder().amount(100_000D).status(ExpenseStatus.APPROVED).build(),
                Expense.builder().amount(200_000D).status(ExpenseStatus.PENDING).build(),
                Expense.builder().amount(300_000D).status(ExpenseStatus.REJECTED).build(),
                Expense.builder().amount(50_000D).status(null).build()
        ));

        Double total = expenseService.calculateTripExpense("trip-1", managerUser());

        assertThat(total).isEqualTo(150_000D);
    }

    private Trip activeTrip() {
        Driver driver = Driver.builder()
                .id("driver-1")
                .userId("driver-user")
                .name("Tài xế Minh")
                .build();
        return Trip.builder()
                .id("trip-1")
                .driver(driver)
                .status(TripStatus.IN_PROGRESS)
                .startTime(LocalDate.now().minusDays(2) + "T08:00")
                .endTime(LocalDate.now().plusDays(1) + "T18:00")
                .build();
    }

    private ExpenseCreationRequest requestFor(Trip trip) {
        return ExpenseCreationRequest.builder()
                .tripId(trip.getId())
                .expenseTypes(List.of(ExpenseType.FUEL))
                .amount(500_000D)
                .description("Đổ dầu dọc đường")
                .expenseDate(LocalDate.now().minusDays(1))
                .build();
    }

    private User managerUser() {
        return User.builder()
                .id("manager-user")
                .username("manager01")
                .fullName("Quản lý vận hành")
                .role(Role.MANAGER)
                .build();
    }

    private User driverUser() {
        return User.builder()
                .id("driver-user")
                .username("driver01")
                .role(Role.DRIVER)
                .build();
    }
}
