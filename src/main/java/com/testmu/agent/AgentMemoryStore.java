package com.testmu.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.testmu.agent.model.FailureInsightRecord;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AgentMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(AgentMemoryStore.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final FrameworkConfig config = ConfigManager.get();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public void save(FailureInsightRecord record) {
        try {
            Path memoryDir = Path.of(config.aiAgentMemoryPath(), "history",
                    record.context().testClass());
            Files.createDirectories(memoryDir);

            String fileName = record.context().testMethod() + "_"
                    + record.analyzedAt().format(FILE_TS) + ".json";
            Path filePath = memoryDir.resolve(fileName);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), record);

            updateLearningIndex(record);
            LOG.info("Agent memory saved: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            LOG.warn("Failed to save agent memory", e);
        }
    }

    public List<FailureInsightRecord> loadHistory(String testClass, String testMethod) {
        Path memoryDir = Path.of(config.aiAgentMemoryPath(), "history", testClass);
        if (!Files.exists(memoryDir)) {
            return List.of();
        }

        List<FailureInsightRecord> history = new ArrayList<>();
        try {
            Files.list(memoryDir)
                    .filter(p -> p.getFileName().toString().startsWith(testMethod))
                    .sorted()
                    .forEach(path -> {
                        try {
                            history.add(objectMapper.readValue(path.toFile(), FailureInsightRecord.class));
                        } catch (IOException e) {
                            LOG.debug("Skipping unreadable memory file: {}", path);
                        }
                    });
        } catch (IOException e) {
            LOG.warn("Failed to load agent memory for {}.{}", testClass, testMethod, e);
        }
        return history;
    }

    private void updateLearningIndex(FailureInsightRecord record) throws IOException {
        Path indexPath = Path.of(config.aiAgentMemoryPath(), "learning-index.txt");
        Files.createDirectories(indexPath.getParent());
        String line = "%s | %s.%s | category=%s | confidence=%.0f%% | report=%s%n".formatted(
                record.analyzedAt(),
                record.context().testClass(),
                record.context().testMethod(),
                record.analysis().getFailureCategory(),
                record.analysis().getConfidence() * 100,
                record.htmlReportPath()
        );
        Files.writeString(indexPath, line, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }
}
