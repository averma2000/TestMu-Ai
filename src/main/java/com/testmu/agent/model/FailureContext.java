package com.testmu.agent.model;

import java.time.LocalDateTime;
import java.util.List;

public record FailureContext(
        String testClass,
        String testMethod,
        String testDescription,
        List<String> testGroups,
        LocalDateTime failedAt,
        String exceptionType,
        String exceptionMessage,
        String stackTrace,
        String currentUrl,
        String htmlSource,
        String screenshotPath,
        String apiRequestUrl,
        Integer apiStatusCode,
        String apiResponseBody,
        boolean uiTest
) {
}
