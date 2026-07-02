package com.testmu.api;

import com.fasterxml.jackson.databind.JsonNode;

public record ApiResponse(int statusCode, String rawBody, JsonNode body) {

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
