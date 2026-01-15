package com.coding.interview.ekyc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


@Slf4j
@Component
public class HttpClientWrapper {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpClientWrapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }


    public <T, R> T post(
            String url,
            R requestBody,
            Class<T> responseType,
            int timeoutSeconds,
            String correlationId
    ) throws Exception {
        String requestJson = objectMapper.writeValueAsString(requestBody);

        log.debug("[{}] POST request to: {} with body: {}", correlationId, url, requestJson);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("X-Correlation-ID", correlationId)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("[{}] Response status: {}, body: {}",
            correlationId, response.statusCode(), response.body());

        if (response.statusCode() >= 400) {
            throw new Exception("HTTP " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), responseType);
    }
}

