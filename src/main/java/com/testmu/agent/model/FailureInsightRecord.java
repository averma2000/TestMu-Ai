package com.testmu.agent.model;

import java.time.LocalDateTime;

public record FailureInsightRecord(
        LocalDateTime analyzedAt,
        FailureContext context,
        FailureAnalysis analysis,
        String htmlReportPath,
        String jsonReportPath
) {
}
