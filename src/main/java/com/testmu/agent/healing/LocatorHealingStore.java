package com.testmu.agent.healing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.testmu.agent.healing.model.HealedLocatorEntry;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LocatorHealingStore {

    private static final Logger LOG = LoggerFactory.getLogger(LocatorHealingStore.class);
    private static final String STORE_FILE = "healed-locators.json";

    private static final LocatorHealingStore INSTANCE = new LocatorHealingStore();

    private final FrameworkConfig config = ConfigManager.get();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Map<String, HealedLocatorEntry> cache = new ConcurrentHashMap<>();

    private LocatorHealingStore() {
        load();
    }

    public static LocatorHealingStore getInstance() {
        return INSTANCE;
    }

    public static String key(String pageClass, String elementName) {
        return pageClass + "." + elementName;
    }

    public void save(HealedLocatorEntry entry) {
        cache.put(key(entry.pageClass(), entry.elementName()), entry);
        persist();
        LOG.info("Healed locator stored: {}.{} -> {}:{}",
                entry.pageClass(), entry.elementName(), entry.locatorStrategy(), entry.locatorValue());
    }

    public Optional<HealedLocatorEntry> get(String pageClass, String elementName) {
        return Optional.ofNullable(cache.get(key(pageClass, elementName)));
    }

    public By resolve(String pageClass, String elementName, By defaultBy) {
        return get(pageClass, elementName)
                .map(entry -> LocatorParser.toBy(entry.locatorStrategy(), entry.locatorValue()))
                .orElse(defaultBy);
    }

    public Map<String, HealedLocatorEntry> getAll() {
        return Map.copyOf(cache);
    }

    private void load() {
        Path storePath = storePath();
        if (!Files.exists(storePath)) {
            return;
        }
        try {
            Map<String, HealedLocatorEntry> loaded = objectMapper.readValue(
                    storePath.toFile(), new TypeReference<LinkedHashMap<String, HealedLocatorEntry>>() {});
            cache.clear();
            cache.putAll(loaded);
            LOG.info("Loaded {} healed locator(s) from {}", cache.size(), storePath);
        } catch (IOException e) {
            LOG.warn("Failed to load healed locators from {}", storePath, e);
        }
    }

    private void persist() {
        try {
            Path storePath = storePath();
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), cache);
        } catch (IOException e) {
            LOG.warn("Failed to persist healed locators", e);
        }
    }

    private Path storePath() {
        return Path.of(config.aiAgentMemoryPath(), STORE_FILE);
    }

    public HealedLocatorEntry buildEntry(String pageClass, String elementName, String strategy,
                                          String value, By defaultBy, double confidence, String sourceTest) {
        return new HealedLocatorEntry(
                pageClass,
                elementName,
                strategy,
                value,
                defaultBy.toString(),
                LocalDateTime.now(),
                confidence,
                sourceTest
        );
    }
}
