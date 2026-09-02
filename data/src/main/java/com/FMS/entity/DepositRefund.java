package com.FMS.entity;

import com.FMS.enums.PaymentMethod;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "deposit_refunds")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositRefund extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deposit_id", nullable = false)
    Deposit deposit;

    @Column(nullable = false)
    Double amount;

    @Column(nullable = false)
    LocalDate refundDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentMethod paymentMethod;

    String bankName;

    String accountHolder;

    String accountNumber;

    String referenceNumber;

    @Column(length = 1000)
    String note;
}
