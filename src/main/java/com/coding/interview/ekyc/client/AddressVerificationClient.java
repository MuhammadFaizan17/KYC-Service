package com.coding.interview.ekyc.client;

import com.coding.interview.ekyc.client.dto.AddressVerificationRequestDto;
import com.coding.interview.ekyc.client.dto.AddressVerificationResponseDto;
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
public class AddressVerificationClient {
    private static final String SERVICE_NAME = "AddressVerificationService";
    private static final int TIMEOUT_SECONDS = 5;

    private final HttpClientWrapper httpClient;
    private final RateLimiter rateLimiter;
    private final RetryHandler retryHandler;

    @Value("${ekyc.address.service.url:http://localhost:8083/api/v1/verify-address}")
    private String serviceUrl;

    public VerificationResult verify(Customer customer, VerificationRequest request, String correlationId) {
        log.info("[{}] Starting address verification for customer: {}", correlationId, customer.getCustomerId());

        try {
            rateLimiter.acquire(SERVICE_NAME);

            AddressVerificationResponseDto response = retryHandler.executeWithRetry(
                () -> {
                    try {
                        AddressVerificationRequestDto requestDto = AddressVerificationRequestDto.builder()
                            .customerId(customer.getCustomerId())
                            .address(customer.getAddress())
                            .proofType(request.getProofType())
                            .proofDate(request.getProofDate())
                            .proofUrl(request.getProofUrl())
                            .build();

                        return httpClient.post(serviceUrl, requestDto,
                            AddressVerificationResponseDto.class, TIMEOUT_SECONDS, correlationId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                SERVICE_NAME,
                correlationId
            );

            VerificationResult result = VerificationResult.builder()
                .verificationType(VerificationType.ADDRESS)
                .status(VerificationStatus.valueOf(response.getStatus()))
                .confidence(response.getConfidence())
                .reasons(response.getReasons() != null ? response.getReasons() : new ArrayList<>())
                .timestamp(Instant.now())
                .build();

            log.info("[{}] Address verification completed: {}", correlationId, result.getStatus());
            return result;

        } catch (Exception e) {
            log.error("[{}] Address verification failed", correlationId, e);
            return VerificationResult.builder()
                .verificationType(VerificationType.ADDRESS)
                .status(VerificationStatus.FAIL)
                .confidence(0)
                .reasons(new ArrayList<>() {{ add("Service unavailable: " + e.getMessage()); }})
                .timestamp(Instant.now())
                .build();
        }
    }
}

