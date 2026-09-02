package com.FMS.entity;

import com.FMS.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_proofs")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripProof extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    Trip trip;

    String recipientName;

    String recipientPhone;

    @Column(columnDefinition = "TEXT")
    String signatureBase64;

    @Column(columnDefinition = "TEXT")
    String photoUrls;

    @Column(length = 1000)
    String notes;

    @Column(nullable = false)
    LocalDateTime signedAt;
}
