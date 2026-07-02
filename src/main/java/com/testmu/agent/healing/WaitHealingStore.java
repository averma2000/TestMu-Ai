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
import java.util.concurrent.ConcurrentHashMap;

public final class WaitHealingStore {

    private static final Logger LOG = LoggerFactory.getLogger(WaitHealingStore.class);
    private static final WaitHealingStore INSTANCE = new WaitHealingStore();
    private static final String STORE_FILE = "healed-waits.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Integer> waitOverrides = new ConcurrentHashMap<>();

    private WaitHealingStore() {
        load();
    }

    public static WaitHealingStore getInstance() {
        return INSTANCE;
    }

    public void setWaitSeconds(String pageClass, int seconds) {
        waitOverrides.put(pageClass, seconds);
        persist();
        LOG.info("Healed wait stored for {}: {}s", pageClass, seconds);
    }

    public int getWaitSeconds(String pageClass, int defaultSeconds) {
        return waitOverrides.getOrDefault(pageClass, defaultSeconds);
    }

    private void load() {
        Path path = storePath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            Map<String, Integer> loaded = objectMapper.readValue(path.toFile(), new TypeReference<>() {});
            waitOverrides.clear();
            waitOverrides.putAll(loaded);
        } catch (IOException e) {
            LOG.warn("Failed to load wait healing store", e);
        }
    }

    private void persist() {
        try {
            Path path = storePath();
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), waitOverrides);
        } catch (IOException e) {
            LOG.warn("Failed to persist wait healing store", e);
        }
    }

    private Path storePath() {
        return Path.of(ConfigManager.get().aiAgentMemoryPath(), STORE_FILE);
    }
}
