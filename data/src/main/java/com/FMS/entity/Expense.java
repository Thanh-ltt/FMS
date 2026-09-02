package com.FMS.entity;

import com.FMS.converter.ExpenseTypeListConverter;
import com.FMS.enums.ExpenseStatus;
import com.FMS.enums.ExpenseType;
import com.FMS.enums.Role;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "expenses")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Expense extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Enumerated(EnumType.STRING)
    ExpenseType expenseType;

    @Convert(converter = ExpenseTypeListConverter.class)
    @Column(name = "expense_types", length = 1000)
    List<ExpenseType> expenseTypes;

    @NotNull(message = "INVALID_EXPENSE_INPUT")
    @Positive(message = "INVALID_EXPENSE_INPUT")
    Double amount;

    @Size(max = 500, message = "INVALID_EXPENSE_INPUT")
    String description;

    @Column(columnDefinition = "TEXT")
    @Size(max = 2_200_000, message = "INVALID_EXPENSE_RECEIPT")
    String receiptImageUrl;

    @NotNull(message = "INVALID_EXPENSE_INPUT")
    @PastOrPresent(message = "INVALID_EXPENSE_INPUT")
    LocalDate expenseDate;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    Trip trip;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ExpenseStatus status = ExpenseStatus.PENDING;

    String recordedByUserId;

    String recordedByName;

    @Enumerated(EnumType.STRING)
    Role recordedByRole;

    String reviewedByUserId;

    String reviewedByName;

    LocalDateTime reviewedAt;

    @Size(max = 500, message = "INVALID_EXPENSE_INPUT")
    String reviewNote;
}
