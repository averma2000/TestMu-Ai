package com.testmu.agent.flaky;

import com.testmu.agent.flaky.model.TestStabilityProfile;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class FlakyReportGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(FlakyReportGenerator.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FrameworkConfig config = ConfigManager.get();

    public Path generate(List<TestStabilityProfile> profiles) throws IOException {
        Path reportDir = Path.of(config.reportsDir(), "flaky");
        Files.createDirectories(reportDir);

        Path htmlPath = reportDir.resolve("flaky-report.html");
        Files.writeString(htmlPath, buildHtml(profiles), StandardCharsets.UTF_8);

        LOG.info("Flaky test report: {}", htmlPath.toAbsolutePath());
        return htmlPath;
    }

    private String buildHtml(List<TestStabilityProfile> profiles) {
        long flakyCount = profiles.stream()
                .filter(p -> "flaky".equals(p.getClassification()))
                .count();

        String rows = profiles.stream()
                .map(this::buildRow)
                .collect(Collectors.joining());

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <title>Flaky Test Classifier Report</title>
                  <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; background: #f4f6f9; }
                    .header { background: #1a1a2e; color: #fff; padding: 24px 32px; }
                    .container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
                    table { width: 100%%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; }
                    th, td { padding: 10px 12px; border-bottom: 1px solid #e5e7eb; text-align: left; }
                    th { background: #16213e; color: #fff; }
                    .flaky { color: #dc2626; font-weight: bold; }
                    .stable { color: #16a34a; }
                    .healing { color: #d97706; }
                    .score { font-family: monospace; }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <h1>Flaky Test Classifier</h1>
                    <p>Generated %s | %d test(s) tracked | %d flaky</p>
                  </div>
                  <div class="container">
                    <table>
                      <thead>
                        <tr>
                          <th>Test</th>
                          <th>Runs</th>
                          <th>Pass</th>
                          <th>Fail</th>
                          <th>Healed</th>
                          <th>Score</th>
                          <th>Classification</th>
                          <th>Recommendation</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                    </table>
                  </div>
                </body>
                </html>
                """.formatted(LocalDateTime.now().format(TS), profiles.size(), flakyCount, rows);
    }

    private String buildRow(TestStabilityProfile p) {
        String cssClass = switch (p.getClassification()) {
            case "flaky", "failing" -> "flaky";
            case "healing-dependent" -> "healing";
            default -> "stable";
        };
        return """
                <tr>
                  <td>%s.%s</td>
                  <td>%d</td>
                  <td>%d</td>
                  <td>%d</td>
                  <td>%d</td>
                  <td class="score">%.2f</td>
                  <td class="%s">%s</td>
                  <td>%s</td>
                </tr>
                """.formatted(
                escape(p.getTestClass()), escape(p.getTestMethod()),
                p.getTotalRuns(), p.getPasses(), p.getFailures(), p.getHealedPasses(),
                p.getFlakyScore(), cssClass, escape(p.getClassification()),
                escape(p.getRecommendation())
        );
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
