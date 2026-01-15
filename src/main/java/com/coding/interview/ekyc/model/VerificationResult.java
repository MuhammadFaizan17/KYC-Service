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
public class VerificationResult {
    private VerificationType verificationType;
    private VerificationStatus status;
    private Integer confidence;
    private List<String> reasons;
    private Instant timestamp;
    private Double similarityScore; // For biometric
    private Integer matchCount;     // For sanctions
}

