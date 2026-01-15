package com.coding.interview.ekyc.client;

import com.coding.interview.ekyc.client.dto.SanctionsRequestDto;
import com.coding.interview.ekyc.client.dto.SanctionsResponseDto;
import com.coding.interview.ekyc.model.Customer;
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
public class SanctionsScreeningClient {
    private static final String SERVICE_NAME = "SanctionsScreeningService";
    private static final int TIMEOUT_SECONDS = 3;

    private final HttpClientWrapper httpClient;
    private final RateLimiter rateLimiter;
    private final RetryHandler retryHandler;

    @Value("${ekyc.sanctions.service.url:http://localhost:8084/api/v1/check-sanctions}")
    private String serviceUrl;

    public VerificationResult verify(Customer customer, String correlationId) throws Exception {
        log.info("[{}] Starting sanctions screening for customer: {} (CRITICAL)",
            correlationId, customer.getCustomerId());


        rateLimiter.acquire(SERVICE_NAME);


        SanctionsResponseDto response = retryHandler.executeWithRetry(
            () -> {
                try {
                    SanctionsRequestDto requestDto = SanctionsRequestDto.builder()
                        .customerId(customer.getCustomerId())
                        .fullName(customer.getFullName())
                        .dateOfBirth(customer.getDateOfBirth())
                        .nationality(customer.getNationality())
                        .build();

                    return httpClient.post(serviceUrl, requestDto,
                        SanctionsResponseDto.class, TIMEOUT_SECONDS, correlationId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            SERVICE_NAME,
            correlationId
        );

        VerificationResult result = VerificationResult.builder()
            .verificationType(VerificationType.SANCTIONS)
            .status(VerificationStatus.valueOf(response.getStatus()))
            .confidence(100)
            .matchCount(response.getMatchCount())
            .reasons(response.getMatches() != null ? response.getMatches() : new ArrayList<>())
            .timestamp(Instant.now())
            .build();

        if ("HIT".equals(response.getStatus())) {
            log.warn("[{}] SANCTIONS HIT detected for customer: {} - {} matches found",
                correlationId, customer.getCustomerId(), response.getMatchCount());
        } else {
            log.info("[{}] Sanctions screening completed: CLEAR", correlationId);
        }

        return result;
    }
}

