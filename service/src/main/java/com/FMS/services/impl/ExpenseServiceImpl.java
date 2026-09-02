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
import com.FMS.services.ExpenseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {
    static final int MAX_RECEIPT_IMAGE_LENGTH = 2_200_000;

    ExpenseRepository expenseRepository;

    TripRepository tripRepository;

    DriverRepository driverRepository;

    ExpenseMapper expenseMapper;

    @Override
    @Transactional
    public ExpenseDto createExpense(ExpenseCreationRequest request, User actor) {
        validateExpense(request.getAmount(), request.getExpenseDate());
        Trip trip = tripRepository.findById(request.getTripId()).orElseThrow(() ->
                new AppException(ErrorCode.TRIP_NOT_FOUND));
        validateTripAccess(trip, actor);
        validateTripAllowsExpense(trip);
        validateExpenseDateWithinTrip(trip, request.getExpenseDate());
        List<ExpenseType> expenseTypes = resolveExpenseTypes(
                request.getExpenseTypes(),
                request.getExpenseType()
        );
        String description = normalizeDescription(request.getDescription());
        validateDescription(expenseTypes, description);

        Expense expense = Expense.builder()
                .trip(trip)
                .expenseType(expenseTypes.getFirst())
                .expenseTypes(expenseTypes)
                .amount(request.getAmount())
                .description(description)
                .receiptImageUrl(validateReceiptImage(request.getReceiptImageUrl()))
                .expenseDate(request.getExpenseDate())
                .status(ExpenseStatus.PENDING)
                .recordedByUserId(actor.getId())
                .recordedByName(actorDisplayName(actor, trip))
                .recordedByRole(actor.getRole())
                .build();

        return expenseMapper.toDto(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseDto updateExpense(String id, Expense request, User actor) {

        Expense expense = expenseRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.EXPENSE_NOT_FOUND));
        validateTripAccess(expense.getTrip(), actor);
        validatePendingExpense(expense);
        if (actor.getRole() == Role.DRIVER
                && !Objects.equals(expense.getRecordedByUserId(), actor.getId())) {
            throw new AppException(ErrorCode.EXPENSE_TRIP_ACCESS_DENIED);
        }

        validateExpense(request.getAmount(), request.getExpenseDate());
        validateTripAllowsExpense(expense.getTrip());
        validateExpenseDateWithinTrip(expense.getTrip(), request.getExpenseDate());

        List<ExpenseType> expenseTypes = resolveExpenseTypes(
                request.getExpenseTypes(),
                request.getExpenseType()
        );
        String description = normalizeDescription(request.getDescription());
        validateDescription(expenseTypes, description);
        expense.setExpenseType(expenseTypes.getFirst());
        expense.setExpenseTypes(expenseTypes);
        expense.setAmount(request.getAmount());
        expense.setDescription(description);
        expense.setReceiptImageUrl(validateReceiptImage(request.getReceiptImageUrl()));
        expense.setExpenseDate(request.getExpenseDate());

        return expenseMapper.toDto(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseDto reviewExpense(
            String id,
            ExpenseStatus status,
            ExpenseReviewRequest request,
            User actor
    ) {
        if (actor == null || (actor.getRole() != Role.ADMIN
                && actor.getRole() != Role.MANAGER
                && actor.getRole() != Role.ACCOUNTANT)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (status != ExpenseStatus.APPROVED && status != ExpenseStatus.REJECTED) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_INPUT);
        }

        Expense expense = expenseRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.EXPENSE_NOT_FOUND));
        validatePendingExpense(expense);
        String reviewNote = normalizeDescription(request == null ? null : request.getReviewNote());
        if (status == ExpenseStatus.REJECTED && reviewNote == null) {
            throw new AppException(ErrorCode.EXPENSE_REJECTION_NOTE_REQUIRED);
        }

        expense.setStatus(status);
        expense.setReviewNote(reviewNote);
        expense.setReviewedByUserId(actor.getId());
        expense.setReviewedByName(actorDisplayName(actor, expense.getTrip()));
        expense.setReviewedAt(LocalDateTime.now());
        return expenseMapper.toDto(expenseRepository.save(expense));
    }

    @Override
    public void delete(String id) {

        Expense expense = expenseRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.EXPENSE_NOT_FOUND));

        expenseRepository.delete(expense);
    }

    @Override
    public ExpenseDto getById(String id) {

        Expense expense = expenseRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.EXPENSE_NOT_FOUND));

        return expenseMapper.toDto(expense);
    }

    @Override
    public List<ExpenseDto> getAll() {

        return expenseRepository.findAll()
                .stream()
                .map(expenseMapper::toDto)
                .toList();
    }

    @Override
    public Double calculateTripExpense(String tripId, User actor) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() ->
                new AppException(ErrorCode.TRIP_NOT_FOUND));
        validateTripAccess(trip, actor);
        return expenseRepository.findByTripId(tripId)
                .stream()
                .filter(this::isApproved)
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    @Override
    public Double calculateTotalExpense() {

        return expenseRepository.findAll()
                .stream()
                .filter(this::isApproved)
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    @Override
    public List<ExpenseDto> findByTrip(String tripId, User actor) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() ->
                new AppException(ErrorCode.TRIP_NOT_FOUND));
        validateTripAccess(trip, actor);
        return expenseRepository.findByTripId(tripId)
                .stream()
                .sorted((left, right) -> {
                    LocalDateTime leftTime = left.getCreatedAt();
                    LocalDateTime rightTime = right.getCreatedAt();
                    if (leftTime == null && rightTime == null) return 0;
                    if (leftTime == null) return 1;
                    if (rightTime == null) return -1;
                    return rightTime.compareTo(leftTime);
                })
                .map(expenseMapper::toDto)
                .toList();
    }

    @Override
    public List<ExpenseDto> findByExpenseType(ExpenseType expenseType) {

        return expenseRepository.findAll()
                .stream()
                .filter(expense -> resolveExpenseTypes(
                        expense.getExpenseTypes(),
                        expense.getExpenseType()
                ).contains(expenseType))
                .map(expenseMapper::toDto)
                .toList();
    }

    private List<ExpenseType> resolveExpenseTypes(List<ExpenseType> values, ExpenseType legacyValue) {
        LinkedHashSet<ExpenseType> resolved = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null)
                    .forEach(resolved::add);
        }
        if (resolved.isEmpty() && legacyValue != null) {
            resolved.add(legacyValue);
        }
        if (resolved.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return List.copyOf(resolved);
    }

    private void validateExpense(Double amount, LocalDate expenseDate) {
        if (amount == null
                || amount <= 0
                || expenseDate == null
                || expenseDate.isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_INPUT);
        }
    }

    private void validateTripAccess(Trip trip, User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (actor.getRole() == Role.CUSTOMER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (actor.getRole() != Role.DRIVER) {
            return;
        }

        Driver driver = driverRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));
        if (trip == null
                || trip.getDriver() == null
                || !Objects.equals(driver.getId(), trip.getDriver().getId())) {
            throw new AppException(ErrorCode.EXPENSE_TRIP_ACCESS_DENIED);
        }
    }

    private void validateTripAllowsExpense(Trip trip) {
        if (trip == null || (trip.getStatus() != TripStatus.IN_PROGRESS
                && trip.getStatus() != TripStatus.COMPLETED)) {
            throw new AppException(ErrorCode.EXPENSE_TRIP_STATUS_INVALID);
        }
    }

    private void validateExpenseDateWithinTrip(Trip trip, LocalDate expenseDate) {
        LocalDate startDate = parseTripDate(trip.getStartTime());
        LocalDate endDate = parseTripDate(trip.getEndTime());
        if (startDate == null
                || endDate == null
                || expenseDate.isBefore(startDate)
                || expenseDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.EXPENSE_DATE_OUTSIDE_TRIP);
        }
    }

    private LocalDate parseTripDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private void validateDescription(List<ExpenseType> expenseTypes, String description) {
        if (expenseTypes.contains(ExpenseType.OTHER) && description == null) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_INPUT);
        }
    }

    private String validateReceiptImage(String receiptImageUrl) {
        if (receiptImageUrl == null || receiptImageUrl.isBlank()) {
            return null;
        }
        String normalized = receiptImageUrl.trim();
        boolean supported = normalized.startsWith("data:image/jpeg;base64,")
                || normalized.startsWith("data:image/png;base64,")
                || normalized.startsWith("data:image/webp;base64,");
        if (!supported || normalized.length() > MAX_RECEIPT_IMAGE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_RECEIPT);
        }
        return normalized;
    }

    private void validatePendingExpense(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new AppException(ErrorCode.EXPENSE_ALREADY_REVIEWED);
        }
    }

    private boolean isApproved(Expense expense) {
        return expense.getStatus() == null || expense.getStatus() == ExpenseStatus.APPROVED;
    }

    private String actorDisplayName(User actor, Trip trip) {
        if (actor.getRole() == Role.DRIVER
                && trip != null
                && trip.getDriver() != null
                && trip.getDriver().getName() != null
                && !trip.getDriver().getName().isBlank()) {
            return trip.getDriver().getName().trim();
        }
        if (actor.getFullName() != null && !actor.getFullName().isBlank()) {
            return actor.getFullName().trim();
        }
        return actor.getUsername();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
