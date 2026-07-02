package com.testmu.agent.flaky;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.testmu.agent.flaky.model.TestRunOutcome;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestRunRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(TestRunRecorder.class);
    private static final TestRunRecorder INSTANCE = new TestRunRecorder();
    private static final String STORE_FILE = "test-run-history.json";

    private final FrameworkConfig config = ConfigManager.get();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final List<TestRunOutcome> outcomes = new ArrayList<>();

    private TestRunRecorder() {
        load();
    }

    public static TestRunRecorder getInstance() {
        return INSTANCE;
    }

    public void record(String testClass, String testMethod, String status,
                       boolean healedRetry, String failureCategory) {
        if (!config.aiFlakyClassifierEnabled()) {
            return;
        }
        outcomes.add(new TestRunOutcome(
                testClass, testMethod, status, healedRetry, failureCategory, LocalDateTime.now()));
        persist();
        LOG.debug("Recorded {} outcome for {}.{}", status, testClass, testMethod);
    }

    public List<TestRunOutcome> getAllOutcomes() {
        return List.copyOf(outcomes);
    }

    public List<TestRunOutcome> getOutcomesForTest(String testClass, String testMethod) {
        return outcomes.stream()
                .filter(o -> o.getTestClass().equals(testClass) && o.getTestMethod().equals(testMethod))
                .toList();
    }

    private void load() {
        Path path = storePath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            List<TestRunOutcome> loaded = objectMapper.readValue(path.toFile(), new TypeReference<>() {});
            outcomes.clear();
            outcomes.addAll(loaded);
        } catch (IOException e) {
            LOG.warn("Failed to load test run history", e);
        }
    }

    private void persist() {
        try {
            Path path = storePath();
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), outcomes);
        } catch (IOException e) {
            LOG.warn("Failed to persist test run history", e);
        }
    }

    private Path storePath() {
        return Path.of(config.aiAgentMemoryPath(), STORE_FILE);
    }
}
