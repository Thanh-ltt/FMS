package com.FMS.entity;

import com.FMS.enums.InvoiceStatus;
import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true)
    String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    Trip trip;

    Double totalAmount;

    @Builder.Default
    Double depositAppliedAmount = 0D;

    @Builder.Default
    Double paidAmount = 0D;

    LocalDate issueDate;

    LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    InvoiceStatus status;

    @Transient
    public Double getAmountDue() {
        if (status == InvoiceStatus.PAID || status == InvoiceStatus.CANCELLED) {
            return 0D;
        }
        double total = totalAmount == null ? 0D : totalAmount;
        double paid = paidAmount == null ? 0D : paidAmount;
        return Math.max(total - paid, 0D);
    }

    @Transient
    public InvoiceStatus getEffectiveStatus() {
        if (status == InvoiceStatus.PENDING
                && dueDate != null
                && dueDate.isBefore(LocalDate.now())
                && getAmountDue() > 0) {
            return InvoiceStatus.OVERDUE;
        }
        return status;
    }
}
