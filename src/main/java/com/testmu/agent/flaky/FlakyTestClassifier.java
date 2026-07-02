package com.testmu.agent.flaky;

import com.testmu.agent.flaky.model.TestRunOutcome;
import com.testmu.agent.flaky.model.TestStabilityProfile;
import com.testmu.agent.model.FailureInsightRecord;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlakyTestClassifier {

    private static final Logger LOG = LoggerFactory.getLogger(FlakyTestClassifier.class);

    private final FrameworkConfig config = ConfigManager.get();
    private final TestRunRecorder recorder = TestRunRecorder.getInstance();

    public List<TestStabilityProfile> classifyAll() {
        Map<String, TestStabilityProfile> profiles = new HashMap<>();

        for (TestRunOutcome outcome : recorder.getAllOutcomes()) {
            String key = outcome.testKey();
            TestStabilityProfile profile = profiles.computeIfAbsent(key, k -> {
                TestStabilityProfile p = new TestStabilityProfile();
                p.setTestClass(outcome.getTestClass());
                p.setTestMethod(outcome.getTestMethod());
                return p;
            });

            profile.setTotalRuns(profile.getTotalRuns() + 1);
            switch (outcome.getStatus()) {
                case "PASS" -> {
                    if (outcome.isHealedRetry()) {
                        profile.setHealedPasses(profile.getHealedPasses() + 1);
                    } else {
                        profile.setPasses(profile.getPasses() + 1);
                    }
                }
                case "FAIL" -> profile.setFailures(profile.getFailures() + 1);
                default -> { }
            }

            if (outcome.getFailureCategory() != null && !outcome.getFailureCategory().isBlank()) {
                profile.setDominantFailureCategory(outcome.getFailureCategory());
            }
        }

        for (TestStabilityProfile profile : profiles.values()) {
            computeScore(profile);
        }

        List<TestStabilityProfile> sorted = new ArrayList<>(profiles.values());
        sorted.sort(Comparator.comparingDouble(TestStabilityProfile::getFlakyScore).reversed());
        return sorted;
    }

    public TestStabilityProfile classify(String testClass, String testMethod) {
        return classifyAll().stream()
                .filter(p -> p.getTestClass().equals(testClass) && p.getTestMethod().equals(testMethod))
                .findFirst()
                .orElse(null);
    }

    public boolean isFlaky(String testClass, String testMethod) {
        TestStabilityProfile profile = classify(testClass, testMethod);
        return profile != null && profile.getFlakyScore() >= config.aiFlakyThreshold();
    }

    public void enrichFromFailureHistory(String testClass, String testMethod,
                                         List<FailureInsightRecord> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        String dominantCategory = history.stream()
                .map(r -> r.analysis().getFailureCategory())
                .filter(c -> c != null && !c.isBlank())
                .reduce((a, b) -> b)
                .orElse(null);

        for (TestRunOutcome outcome : recorder.getOutcomesForTest(testClass, testMethod)) {
            if (outcome.getFailureCategory() == null && dominantCategory != null) {
                outcome.setFailureCategory(dominantCategory);
            }
        }
    }

    private void computeScore(TestStabilityProfile profile) {
        int total = profile.getTotalRuns();
        if (total == 0) {
            profile.setFlakyScore(0);
            profile.setClassification("unknown");
            profile.setRecommendation("No run history yet");
            return;
        }

        int failures = profile.getFailures();
        int healed = profile.getHealedPasses();
        double failRate = (double) failures / total;
        double healRate = (double) healed / total;

        // Flaky: alternates between pass and fail, or passes only after healing
        double score = failRate * 0.6 + healRate * 0.4;
        if (failures > 0 && profile.getPasses() > 0) {
            score = Math.min(1.0, score + 0.2);
        }

        profile.setFlakyScore(Math.round(score * 100.0) / 100.0);

        if (score >= config.aiFlakyThreshold()) {
            profile.setClassification("flaky");
            profile.setRecommendation(buildFlakyRecommendation(profile));
        } else if (failRate > 0.5) {
            profile.setClassification("failing");
            profile.setRecommendation("Consistently failing — review test logic and application state");
        } else if (healRate > 0.3) {
            profile.setClassification("healing-dependent");
            profile.setRecommendation("Often passes only after self-healing — consider permanent fix");
        } else {
            profile.setClassification("stable");
            profile.setRecommendation("Test appears stable across runs");
        }

        LOG.debug("Classified {}.{} as {} (score={})",
                profile.getTestClass(), profile.getTestMethod(),
                profile.getClassification(), profile.getFlakyScore());
    }

    private String buildFlakyRecommendation(TestStabilityProfile profile) {
        String category = profile.getDominantFailureCategory();
        if ("timing".equals(category)) {
            return "Flaky due to timing — increase explicit waits or apply pre-healing";
        }
        if ("locator".equals(category)) {
            return "Flaky due to locators — review HealingLocator overrides";
        }
        return "Flaky test — investigate environment, data, and synchronization";
    }
}
