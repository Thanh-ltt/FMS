package com.FMS.entity;

import com.FMS.enums.DepositStatus;
import com.FMS.enums.PaymentMethod;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "deposits")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Deposit extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, unique = true)
    String receiptNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    Contract contract;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    Trip trip;

    @Column(nullable = false)
    Double amount;

    @Builder.Default
    @Column(nullable = false)
    Double allocatedAmount = 0D;

    @Builder.Default
    @Column(nullable = false)
    Double refundedAmount = 0D;

    @Column(nullable = false)
    LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentMethod paymentMethod;

    String bankName;

    String accountHolder;

    String accountNumber;

    String referenceNumber;

    @Column(length = 1000)
    String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DepositStatus status;

    @Transient
    public Double getAvailableAmount() {
        double received = amount == null ? 0D : amount;
        double allocated = allocatedAmount == null ? 0D : allocatedAmount;
        double refunded = refundedAmount == null ? 0D : refundedAmount;
        return Math.max(received - allocated - refunded, 0D);
    }
}
