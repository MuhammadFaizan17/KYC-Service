package com.coding.interview.ekyc.client;

import com.coding.interview.ekyc.client.dto.BiometricRequestDto;
import com.coding.interview.ekyc.client.dto.BiometricResponseDto;
import com.coding.interview.ekyc.model.Customer;
import com.coding.interview.ekyc.model.VerificationRequest;
import com.coding.interview.ekyc.model.VerificationResult;
import com.coding.interview.ekyc.model.VerificationStatus;
import com.coding.interview.ekyc.model.VerificationType;
import com.coding.interview.ekyc.ratelimit.RateLimiter;
import com.coding.interview.ekyc.retry.RetryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Client for Biometric Verification Service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BiometricVerificationClient {
    private static final String SERVICE_NAME = "BiometricService";
    private static final int TIMEOUT_SECONDS = 8;

    private final HttpClientWrapper httpClient;
    private final RateLimiter rateLimiter;
    private final RetryHandler retryHandler;

    @Value("${ekyc.biometric.service.url:http://localhost:8082/api/v1/face-match}")
    private String serviceUrl;

    public VerificationResult verify(Customer customer, VerificationRequest request, String correlationId) {
        log.info("[{}] Starting biometric verification for customer: {}", correlationId, customer.getCustomerId());

        try {
            // Respect rate limit
            rateLimiter.acquire(SERVICE_NAME);

            // Execute with retry
            BiometricResponseDto response = retryHandler.executeWithRetry(
                () -> {
                    try {
                        BiometricRequestDto requestDto = BiometricRequestDto.builder()
                            .customerId(customer.getCustomerId())
                            .selfieUrl(request.getSelfieUrl())
                            .idPhotoUrl(request.getIdPhotoUrl())
                            .build();

                        return httpClient.post(serviceUrl, requestDto,
                            BiometricResponseDto.class, TIMEOUT_SECONDS, correlationId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                SERVICE_NAME,
                correlationId
            );

            VerificationResult result = VerificationResult.builder()
                .verificationType(VerificationType.FACE_MATCH)
                .status(VerificationStatus.valueOf(response.getStatus()))
                .confidence(response.getConfidence())
                .similarityScore(response.getSimilarityScore())
                .reasons(new ArrayList<>())
                .timestamp(Instant.now())
                .build();

            log.info("[{}] Biometric verification completed: {} (similarity: {}%)",
                correlationId, result.getStatus(), response.getSimilarityScore());
            return result;

        } catch (Exception e) {
            log.error("[{}] Biometric verification failed", correlationId, e);
            return VerificationResult.builder()
                .verificationType(VerificationType.FACE_MATCH)
                .status(VerificationStatus.FAIL)
                .confidence(0)
                .reasons(new ArrayList<>() {{ add("Service unavailable: " + e.getMessage()); }})
                .timestamp(Instant.now())
                .build();
        }
    }
}

