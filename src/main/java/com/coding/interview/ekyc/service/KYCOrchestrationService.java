package com.coding.interview.ekyc.service;

import com.coding.interview.ekyc.client.AddressVerificationClient;
import com.coding.interview.ekyc.client.BiometricVerificationClient;
import com.coding.interview.ekyc.client.DocumentVerificationClient;
import com.coding.interview.ekyc.client.SanctionsScreeningClient;
import com.coding.interview.ekyc.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class KYCOrchestrationService {

    private final DocumentVerificationClient documentClient;
    private final BiometricVerificationClient biometricClient;
    private final AddressVerificationClient addressClient;
    private final SanctionsScreeningClient sanctionsClient;
    private final DecisionEngineService decisionEngine;


    public KYCDecisionResult performVerification(Customer customer, VerificationRequest request) {
        // Generate correlation ID for request tracking
        String correlationId = request.getRequestId() != null ?
            request.getRequestId() : UUID.randomUUID().toString();

        log.info("[{}] ========== Starting KYC verification for customer: {} ==========",
            correlationId, customer.getCustomerId());
        log.info("[{}] Verification types requested: {}", correlationId, request.getVerificationTypes());

        List<VerificationResult> results = new ArrayList<>();
        Instant startTime = Instant.now();

        try {
            if (request.getVerificationTypes().contains(VerificationType.SANCTIONS)) {
                try {
                    log.info("[{}] Performing CRITICAL sanctions screening", correlationId);
                    VerificationResult sanctionsResult = sanctionsClient.verify(customer, correlationId);
                    results.add(sanctionsResult);

                    if (sanctionsResult.getStatus() == VerificationStatus.HIT) {
                        log.warn("[{}] SANCTIONS HIT - Stopping verification immediately", correlationId);
                        return buildDecisionResult(customer, request, results, correlationId);
                    }
                } catch (Exception e) {
                    log.error("[{}] CRITICAL: Sanctions screening failed - cannot proceed", correlationId, e);
                    results.add(VerificationResult.builder()
                        .verificationType(VerificationType.SANCTIONS)
                        .status(VerificationStatus.FAIL)
                        .confidence(0)
                        .reasons(List.of("Sanctions service unavailable"))
                        .timestamp(Instant.now())
                        .build());
                    return buildDecisionResult(customer, request, results, correlationId);
                }
            }

            // Perform document verification
            if (request.getVerificationTypes().contains(VerificationType.ID_DOCUMENT)) {
                log.info("[{}] Performing document verification", correlationId);
                VerificationResult docResult = documentClient.verify(customer, request, correlationId);
                results.add(docResult);
            }

            // Perform biometric verification
            if (request.getVerificationTypes().contains(VerificationType.FACE_MATCH)) {
                log.info("[{}] Performing biometric verification", correlationId);
                VerificationResult biometricResult = biometricClient.verify(customer, request, correlationId);
                results.add(biometricResult);
            }

            // Perform address verification
            if (request.getVerificationTypes().contains(VerificationType.ADDRESS)) {
                log.info("[{}] Performing address verification", correlationId);
                VerificationResult addressResult = addressClient.verify(customer, request, correlationId);
                results.add(addressResult);
            }

            // Build and return decision
            return buildDecisionResult(customer, request, results, correlationId);

        } catch (Exception e) {
            log.error("[{}] Unexpected error during KYC verification", correlationId, e);
            return buildDecisionResult(customer, request, results, correlationId);
        } finally {
            long durationMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            log.info("[{}] ========== KYC verification completed in {}ms ==========",
                correlationId, durationMs);
        }
    }

    private KYCDecisionResult buildDecisionResult(
            Customer customer,
            VerificationRequest request,
            List<VerificationResult> results,
            String correlationId
    ) {
        // Make final decision
        KYCDecision decision = decisionEngine.makeDecision(results, correlationId);

        KYCDecisionResult decisionResult = KYCDecisionResult.builder()
            .decision(decision)
            .verificationResults(results)
            .timestamp(Instant.now())
            .requestId(request.getRequestId())
            .customerId(customer.getCustomerId())
            .build();

        log.info("[{}] Final KYC Decision: {} for customer: {}",
            correlationId, decision, customer.getCustomerId());
        logVerificationSummary(results, correlationId);

        return decisionResult;
    }

    private void logVerificationSummary(List<VerificationResult> results, String correlationId) {
        log.info("[{}] Verification Summary:", correlationId);
        for (VerificationResult result : results) {
            log.info("[{}]   - {}: {} (confidence: {}%)",
                correlationId,
                result.getVerificationType(),
                result.getStatus(),
                result.getConfidence() != null ? result.getConfidence() : "N/A");
        }
    }
}

