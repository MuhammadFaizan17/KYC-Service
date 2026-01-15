package com.coding.interview.ekyc.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


@Slf4j
@Component
public class RateLimiter {
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(1);

    private final Map<String, Queue<Instant>> requestTimestamps = new ConcurrentHashMap<>();


    public boolean tryAcquire(String serviceName) {
        Queue<Instant> timestamps = requestTimestamps.computeIfAbsent(
            serviceName,
            k -> new ConcurrentLinkedQueue<>()
        );

        Instant now = Instant.now();
        Instant windowStart = now.minus(WINDOW_SIZE);

        timestamps.removeIf(timestamp -> timestamp.isBefore(windowStart));

        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for service: {} (current: {}, max: {})",
                serviceName, timestamps.size(), MAX_REQUESTS_PER_MINUTE);
            return false;
        }

        timestamps.offer(now);
        log.debug("Request allowed for service: {} (current: {}/{}, window: {})",
            serviceName, timestamps.size(), MAX_REQUESTS_PER_MINUTE, WINDOW_SIZE);
        return true;
    }


    public void acquire(String serviceName) throws InterruptedException {
        while (!tryAcquire(serviceName)) {
            log.info("Rate limit reached for {}, waiting 1 second...", serviceName);
            Thread.sleep(1000);
        }
    }


    public void reset(String serviceName) {
        requestTimestamps.remove(serviceName);
    }


    public void resetAll() {
        requestTimestamps.clear();
    }
}

