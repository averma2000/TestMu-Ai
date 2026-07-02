package com.testmu.agent.healing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testmu.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ApiHealingStore {

    private static final Logger LOG = LoggerFactory.getLogger(ApiHealingStore.class);
    private static final ApiHealingStore INSTANCE = new ApiHealingStore();
    private static final String STORE_FILE = "healed-api.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ApiHealEntry> cache = new ConcurrentHashMap<>();

    private ApiHealingStore() {
        load();
    }

    public static ApiHealingStore getInstance() {
        return INSTANCE;
    }

    public record ApiHealEntry(String endpoint, int expectedStatusCode) {
    }

    public void save(String testKey, String endpoint, int expectedStatusCode) {
        cache.put(testKey, new ApiHealEntry(endpoint, expectedStatusCode));
        persist();
        LOG.info("Healed API expectation stored for {}: {} -> {}", testKey, endpoint, expectedStatusCode);
    }

    public Optional<ApiHealEntry> get(String testKey) {
        return Optional.ofNullable(cache.get(testKey));
    }

    private void load() {
        Path path = storePath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            Map<String, ApiHealEntry> loaded = objectMapper.readValue(path.toFile(), new TypeReference<>() {});
            cache.clear();
            cache.putAll(loaded);
        } catch (IOException e) {
            LOG.warn("Failed to load API healing store", e);
        }
    }

    private void persist() {
        try {
            Path path = storePath();
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), cache);
        } catch (IOException e) {
            LOG.warn("Failed to persist API healing store", e);
        }
    }

    private Path storePath() {
        return Path.of(ConfigManager.get().aiAgentMemoryPath(), STORE_FILE);
    }
}
