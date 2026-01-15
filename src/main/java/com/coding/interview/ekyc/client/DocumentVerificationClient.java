package com.coding.interview.ekyc.client;

import com.coding.interview.ekyc.client.dto.DocumentVerificationRequestDto;
import com.coding.interview.ekyc.client.dto.DocumentVerificationResponseDto;
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


@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentVerificationClient {
    private static final String SERVICE_NAME = "DocumentVerificationService";
    private static final int TIMEOUT_SECONDS = 5;

    private final HttpClientWrapper httpClient;
    private final RateLimiter rateLimiter;
    private final RetryHandler retryHandler;

    @Value("${ekyc.document.service.url:http://localhost:8081/api/v1/verify-document}")
    private String serviceUrl;

    public VerificationResult verify(Customer customer, VerificationRequest request, String correlationId) {
        log.info("[{}] Starting document verification for customer: {}", correlationId, customer.getCustomerId());

        try {
            // Respect rate limit
            rateLimiter.acquire(SERVICE_NAME);

            // Execute with retry
            DocumentVerificationResponseDto response = retryHandler.executeWithRetry(
                () -> {
                    try {
                        DocumentVerificationRequestDto requestDto = DocumentVerificationRequestDto.builder()
                            .customerId(customer.getCustomerId())
                            .documentType(request.getDocumentType())
                            .documentNumber(request.getDocumentNumber())
                            .expiryDate(request.getExpiryDate())
                            .documentImageUrl(request.getDocumentImageUrl())
                            .build();

                        return httpClient.post(serviceUrl, requestDto,
                            DocumentVerificationResponseDto.class, TIMEOUT_SECONDS, correlationId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                SERVICE_NAME,
                correlationId
            );

            VerificationResult result = VerificationResult.builder()
                .verificationType(VerificationType.ID_DOCUMENT)
                .status(VerificationStatus.valueOf(response.getStatus()))
                .confidence(response.getConfidence())
                .reasons(response.getReasons() != null ? response.getReasons() : new ArrayList<>())
                .timestamp(Instant.now())
                .build();

            log.info("[{}] Document verification completed: {}", correlationId, result.getStatus());
            return result;

        } catch (Exception e) {
            log.error("[{}] Document verification failed", correlationId, e);
            return VerificationResult.builder()
                .verificationType(VerificationType.ID_DOCUMENT)
                .status(VerificationStatus.FAIL)
                .confidence(0)
                .reasons(new ArrayList<>() {{ add("Service unavailable: " + e.getMessage()); }})
                .timestamp(Instant.now())
                .build();
        }
    }
}

