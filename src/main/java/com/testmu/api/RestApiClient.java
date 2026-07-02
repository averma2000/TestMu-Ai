package com.testmu.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class RestApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RestApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ApiResponse get(String path) {
        return get(path, Map.of());
    }

    public ApiResponse get(String path, Map<String, String> queryParams) {
        String url = buildUrl(path, queryParams);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        return execute(request);
    }

    public ApiResponse post(String path, String jsonBody) {
        String url = buildUrl(path, Map.of());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();
        return execute(request);
    }

    public ApiResponse postWithoutBody(String path) {
        String url = buildUrl(path, Map.of());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();
        return execute(request);
    }

    private String buildUrl(String path, Map<String, String> queryParams) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        StringBuilder url = new StringBuilder(baseUrl).append(normalizedPath);
        if (!queryParams.isEmpty()) {
            url.append("?");
            queryParams.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
            url.setLength(url.length() - 1);
        }
        return url.toString();
    }

    private ApiResponse execute(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = response.body() == null || response.body().isBlank()
                    ? null
                    : objectMapper.readTree(response.body());
            ApiResponse apiResponse = new ApiResponse(response.statusCode(), response.body(), body);
            ApiContextHolder.record(request.uri().toString(), response.statusCode(), response.body());
            return apiResponse;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            ApiContextHolder.record(request.uri().toString(), -1, e.getMessage());
            throw new ApiException("API call failed: " + request.uri(), e);
        }
    }
}
