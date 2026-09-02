package com.FMS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripProofDto {
    private String id;
    private String tripId;
    private String recipientName;
    private String recipientPhone;
    private String signatureBase64;
    private String photoUrls;
    private String notes;
    private LocalDateTime signedAt;
}
