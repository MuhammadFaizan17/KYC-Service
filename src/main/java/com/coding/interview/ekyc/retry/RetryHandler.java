package com.coding.interview.ekyc.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;


@Slf4j
@Component
public class RetryHandler {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 100;
    private static final double BACKOFF_MULTIPLIER = 2.0;


    public <T> T executeWithRetry(
            Supplier<T> operation,
            String serviceName,
            String correlationId
    ) throws Exception {
        Exception lastException = null;
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("[{}] Attempt {}/{} for service: {}",
                    correlationId, attempt, MAX_RETRIES, serviceName);
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                if (attempt == MAX_RETRIES) {
                    log.error("[{}] All {} attempts failed for service: {}",
                        correlationId, MAX_RETRIES, serviceName, e);
                    break;
                }

                log.warn("[{}] Attempt {}/{} failed for service: {}, retrying in {}ms - Error: {}",
                    correlationId, attempt, MAX_RETRIES, serviceName, backoffMs, e.getMessage());

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Retry interrupted", ie);
                }

                // Exponential backoff
                backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
            }
        }

        throw lastException != null ? lastException : new Exception("Operation failed");
    }


    public <T> T executeWithRetry(
            Supplier<T> operation,
            String serviceName,
            String correlationId,
            int maxRetries
    ) throws Exception {
        Exception lastException = null;
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("[{}] Attempt {}/{} for service: {}",
                    correlationId, attempt, maxRetries, serviceName);
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                if (attempt == maxRetries) {
                    log.error("[{}] All {} attempts failed for service: {}",
                        correlationId, maxRetries, serviceName, e);
                    break;
                }

                log.warn("[{}] Attempt {}/{} failed for service: {}, retrying in {}ms - Error: {}",
                    correlationId, attempt, maxRetries, serviceName, backoffMs, e.getMessage());

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Retry interrupted", ie);
                }

                backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
            }
        }

        throw lastException != null ? lastException : new Exception("Operation failed");
    }
}

