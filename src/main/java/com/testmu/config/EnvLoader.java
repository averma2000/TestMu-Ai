package com.testmu.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads secrets from local files before Owner config initializes.
 * Priority at runtime: OS env var &gt; system property &gt; local files.
 */
public final class EnvLoader {

    private static final Logger LOG = LoggerFactory.getLogger(EnvLoader.class);

    private EnvLoader() {
    }

    public static void load() {
        loadPropertiesFile(Path.of("src/test/resources/config.local.properties"));
        loadPropertiesFile(Path.of("config.local.properties"));
        loadDotEnvFile(Path.of(".env"));
    }

    private static void loadPropertiesFile(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            properties.forEach((key, value) -> setPropertyIfAbsent(key.toString(), value.toString()));
            LOG.info("Loaded local config from {}", path.toAbsolutePath());
        } catch (IOException e) {
            LOG.warn("Failed to load {}", path, e);
        }
    }

    private static void loadDotEnvFile(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                setPropertyIfAbsent(key, value);
            }
            LOG.info("Loaded environment variables from {}", path.toAbsolutePath());
        } catch (IOException e) {
            LOG.warn("Failed to load {}", path, e);
        }
    }

    private static void setPropertyIfAbsent(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (System.getenv(key) != null) {
            return;
        }
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
