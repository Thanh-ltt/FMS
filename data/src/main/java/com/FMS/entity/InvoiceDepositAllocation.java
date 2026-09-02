package com.FMS.entity;

import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "invoice_deposit_allocations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"deposit_id", "invoice_id"})
)
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDepositAllocation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deposit_id", nullable = false)
    Deposit deposit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    Invoice invoice;

    @Column(nullable = false)
    Double amount;
}
