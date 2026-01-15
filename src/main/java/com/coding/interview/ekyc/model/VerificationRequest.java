package com.coding.interview.ekyc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    private String requestId;
    private String customerId;
    private List<VerificationType> verificationTypes;
    private Instant timestamp;

    // Document verification fields
    private String documentType;
    private String documentNumber;
    private String expiryDate;
    private String documentImageUrl;

    // Biometric fields
    private String selfieUrl;
    private String idPhotoUrl;

    // Address verification fields
    private String proofType;
    private String proofDate;
    private String proofUrl;
}

