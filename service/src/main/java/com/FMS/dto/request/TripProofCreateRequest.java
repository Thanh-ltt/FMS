package com.FMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripProofCreateRequest {

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    private String recipientPhone;

    @NotBlank(message = "Signature is required")
    private String signatureBase64;

    private String photoUrls;
    private String notes;
}
