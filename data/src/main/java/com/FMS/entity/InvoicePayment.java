package com.FMS.entity;

import com.FMS.enums.PaymentMethod;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "invoice_payments")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoicePayment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    Invoice invoice;

    @Column(nullable = false)
    Double amount;

    @Column(nullable = false)
    LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentMethod paymentMethod;

    String bankName;

    String accountHolder;

    String accountNumber;

    String transactionReference;

    @Column(length = 1000)
    String note;
}
