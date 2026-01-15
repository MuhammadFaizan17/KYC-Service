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
public class KYCDecisionResult {
    private KYCDecision decision;
    private List<VerificationResult> verificationResults;
    private Instant timestamp;
    private String requestId;
    private String customerId;
}

