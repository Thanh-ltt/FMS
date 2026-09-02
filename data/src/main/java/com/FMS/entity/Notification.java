package com.FMS.entity;

import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "notifications")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String targetRole; // e.g. MANAGER, ACCOUNTANT, ADMIN

    @Column(nullable = false)
    String title;

    @Column(length = 1000, nullable = false)
    String message;

    String type; // OVERDUE_INVOICE, MAINTENANCE_DUE, DEPOSIT_WARNING, SYSTEM

    @Builder.Default
    Boolean isRead = false;
}

