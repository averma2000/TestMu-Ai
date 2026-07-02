package com.testmu.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.testmu.agent.healing.model.HealingResult;
import com.testmu.agent.model.FailureAnalysis;
import com.testmu.agent.model.FailureContext;
import com.testmu.agent.model.FailureInsightRecord;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class FailureInsightReportGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(FailureInsightReportGenerator.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final FrameworkConfig config = ConfigManager.get();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public FailureInsightRecord generate(FailureContext context, FailureAnalysis analysis,
                                         HealingResult healingResult) throws IOException {
        Path insightsDir = Path.of(config.aiAgentInsightsDir());
        Files.createDirectories(insightsDir);

        String safeName = context.testClass() + "_" + context.testMethod();
        String timestamp = context.failedAt().format(FILE_TS);
        String baseName = safeName + "_" + timestamp;

        Path htmlPath = insightsDir.resolve(baseName + ".html");
        Path jsonPath = insightsDir.resolve(baseName + ".json");

        String html = buildHtmlReport(context, analysis, healingResult, baseName);
        Files.writeString(htmlPath, html, StandardCharsets.UTF_8);

        FailureInsightRecord record = new FailureInsightRecord(
                context.failedAt(),
                context,
                analysis,
                htmlPath.toAbsolutePath().toString(),
                jsonPath.toAbsolutePath().toString()
        );
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), record);
        appendToIndex(insightsDir, record, baseName);

        LOG.info("Failure insight report: {}", htmlPath.toAbsolutePath());
        return record;
    }

    private void appendToIndex(Path insightsDir, FailureInsightRecord record, String baseName) throws IOException {
        Path indexPath = insightsDir.resolve("index.html");
        String row = """
                  <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td><a href="%s.html">View Report</a></td>
                  </tr>
                """.formatted(
                record.analyzedAt(),
                record.context().testClass(),
                record.context().testMethod(),
                escape(record.analysis().getFailureCategory()),
                baseName
        );

        if (!Files.exists(indexPath)) {
            Files.writeString(indexPath, """
                    <!DOCTYPE html>
                    <html><head><title>Failure Insights Index</title>
                    <style>
                      body { font-family: Arial, sans-serif; margin: 24px; }
                      table { border-collapse: collapse; width: 100%%; }
                      th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                      th { background: #1a1a2e; color: #fff; }
                    </style></head><body>
                    <h1>TestMu AI — Failure Insights</h1>
                    <table>
                      <thead><tr><th>Time</th><th>Class</th><th>Method</th><th>Category</th><th>Report</th></tr></thead>
                      <tbody>
                    </tbody></table></body></html>
                    """, StandardCharsets.UTF_8);
        }

        String existing = Files.readString(indexPath, StandardCharsets.UTF_8);
        String updated = existing.replace("</tbody>", row + "</tbody>");
        Files.writeString(indexPath, updated, StandardCharsets.UTF_8);
    }

    private String buildHtmlReport(FailureContext context, FailureAnalysis analysis,
                                   HealingResult healingResult, String baseName) {
        String screenshotSection = "";
        if (context.screenshotPath() != null && Files.exists(Path.of(context.screenshotPath()))) {
            try {
                Path screenshot = Path.of(context.screenshotPath());
                Path insightsDir = Path.of(config.aiAgentInsightsDir());
                Path dest = insightsDir.resolve(baseName + "_screenshot.png");
                FileUtils.copyFile(screenshot.toFile(), dest.toFile());
                screenshotSection = "<h2>Screenshot</h2><img src=\"" + dest.getFileName() + "\" style=\"max-width:100%;border:1px solid #ccc;\"/>";
            } catch (IOException e) {
                screenshotSection = "<p>Screenshot unavailable</p>";
            }
        }

        String fixes = analysis.getSuggestedFixes().stream()
                .map(fix -> "<li>" + escape(fix) + "</li>")
                .collect(Collectors.joining());
        String steps = analysis.getImplementationSteps().stream()
                .map(step -> "<li>" + escape(step) + "</li>")
                .collect(Collectors.joining());

        String healingApplied = healingResult.getAppliedActions().stream()
                .map(action -> "<li>" + escape(action) + "</li>")
                .collect(Collectors.joining());
        String healingSection = healingResult.getAppliedCount() > 0
                ? "<div class=\"card\"><h2>Self-Healing Applied</h2><p><strong>"
                + healingResult.getAppliedCount() + " fix(es) applied — retry "
                + (healingResult.isRetryRecommended() ? "scheduled" : "not scheduled") + "</strong></p><ul>"
                + healingApplied + "</ul></div>"
                : "<div class=\"card\"><h2>Self-Healing</h2><p>No automatic fixes applied.</p></div>";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <title>Failure Insight — %s.%s</title>
                  <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; background: #f4f6f9; color: #222; }
                    .header { background: #1a1a2e; color: #fff; padding: 24px 32px; }
                    .container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
                    .card { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 6px rgba(0,0,0,.08); }
                    h2 { color: #1a1a2e; border-bottom: 2px solid #e94560; padding-bottom: 6px; }
                    pre { background: #1e1e2e; color: #cdd6f4; padding: 16px; border-radius: 6px; overflow-x: auto; font-size: 13px; }
                    .badge { display: inline-block; background: #e94560; color: #fff; padding: 4px 10px; border-radius: 12px; font-size: 12px; }
                    .confidence { color: #16a34a; font-weight: bold; }
                    code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <h1>Failure Insight Report</h1>
                    <p>%s &rsaquo; %s &nbsp; <span class="badge">%s</span></p>
                  </div>
                  <div class="container">
                    <div class="card">
                      <h2>AI Summary</h2>
                      <p>%s</p>
                      <p><strong>Root Cause:</strong> %s</p>
                      <p><strong>Confidence:</strong> <span class="confidence">%.0f%%</span></p>
                    </div>
                    <div class="card">
                      <h2>Exception</h2>
                      <p><strong>Type:</strong> <code>%s</code></p>
                      <p><strong>Message:</strong> %s</p>
                      <pre>%s</pre>
                    </div>
                    <div class="card">
                      <h2>Page Context</h2>
                      <p><strong>URL:</strong> %s</p>
                      %s
                    </div>
                    <div class="card">
                      <h2>Suggested Fixes</h2>
                      <ul>%s</ul>
                    </div>
                    <div class="card">
                      <h2>Implementation Steps</h2>
                      <ol>%s</ol>
                    </div>
                    <div class="card">
                      <h2>Corrected Code Snippet</h2>
                      <pre>%s</pre>
                    </div>
                    <div class="card">
                      <h2>Self-Healing Recommendation</h2>
                      <p>%s</p>
                    </div>
                    %s
                    <div class="card">
                      <h2>HTML Source (DOM)</h2>
                      <pre>%s</pre>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                context.testClass(), context.testMethod(),
                context.testClass(), context.testMethod(), escape(analysis.getFailureCategory()),
                escape(analysis.getSummary()),
                escape(analysis.getRootCause()),
                analysis.getConfidence() * 100,
                escape(context.exceptionType()),
                escape(context.exceptionMessage()),
                escape(context.stackTrace()),
                escape(nullToEmpty(context.currentUrl())),
                screenshotSection,
                fixes,
                steps,
                escape(nullToEmpty(analysis.getCorrectedCodeSnippet())),
                escape(nullToEmpty(analysis.getHealingRecommendation())),
                healingSection,
                escape(nullToEmpty(context.htmlSource()))
        );
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "N/A";
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
