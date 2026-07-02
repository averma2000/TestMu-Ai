package com.testmu.agent.learning;

import com.testmu.agent.AgentMemoryStore;
import com.testmu.agent.flaky.FlakyTestClassifier;
import com.testmu.agent.flaky.model.TestStabilityProfile;
import com.testmu.agent.healing.WaitHealingStore;
import com.testmu.agent.model.FailureInsightRecord;
import com.testmu.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Applies learned fixes before a test runs based on prior failure history and flaky classification.
 */
public class LearningAdvisor {

    private static final Logger LOG = LoggerFactory.getLogger(LearningAdvisor.class);

    private final AgentMemoryStore memoryStore = new AgentMemoryStore();
    private final FlakyTestClassifier flakyClassifier = new FlakyTestClassifier();
    private final WaitHealingStore waitStore = WaitHealingStore.getInstance();

    public void applyPreRunHealing(String testClass, String testMethod) {
        if (!ConfigManager.get().aiAgentEnabled()) {
            return;
        }

        List<FailureInsightRecord> history = memoryStore.loadHistory(testClass, testMethod);
        if (history.isEmpty()) {
            return;
        }

        TestStabilityProfile profile = flakyClassifier.classify(testClass, testMethod);
        boolean flaky = profile != null && flakyClassifier.isFlaky(testClass, testMethod);

        long timingFailures = history.stream()
                .filter(r -> "timing".equals(r.analysis().getFailureCategory()))
                .count();

        if (timingFailures >= 1 || flaky) {
            String pageClass = inferPageClass(testClass);
            int currentWait = waitStore.getWaitSeconds(pageClass, 10);
            int boostedWait = Math.min(currentWait + 5, 30);
            if (boostedWait > currentWait) {
                waitStore.setWaitSeconds(pageClass, boostedWait);
                LOG.info("Pre-run learning: boosted wait for {} to {}s ({} prior timing failure(s), flaky={})",
                        pageClass, boostedWait, timingFailures, flaky);
            }
        }
    }

    public String buildHistorySummary(String testClass, String testMethod) {
        List<FailureInsightRecord> history = memoryStore.loadHistory(testClass, testMethod);
        if (history.isEmpty()) {
            return "";
        }

        TestStabilityProfile profile = flakyClassifier.classify(testClass, testMethod);
        StringBuilder sb = new StringBuilder();
        sb.append("## Prior Failure History (").append(history.size()).append(" run(s))\n");

        if (profile != null) {
            sb.append("- Stability: ").append(profile.getClassification())
                    .append(" (flaky score: ").append(profile.getFlakyScore()).append(")\n");
            sb.append("- Recommendation: ").append(profile.getRecommendation()).append("\n");
        }

        int limit = Math.min(history.size(), 5);
        for (int i = history.size() - limit; i < history.size(); i++) {
            FailureInsightRecord record = history.get(i);
            sb.append("- ").append(record.analyzedAt())
                    .append(" | category=").append(record.analysis().getFailureCategory())
                    .append(" | cause=").append(truncate(record.analysis().getRootCause(), 120))
                    .append("\n");
        }
        return sb.toString();
    }

    private String inferPageClass(String testClass) {
        if (testClass.contains("Login")) {
            return "LoginPage";
        }
        if (testClass.contains("Dashboard")) {
            return "DashboardPage";
        }
        if (testClass.endsWith("Test")) {
            return testClass.replace("Test", "Page");
        }
        return testClass;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "N/A";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
