package com.testmu.agent.healing;

import com.testmu.config.ConfigManager;
import com.testmu.core.DriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HealingRetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(HealingRetryAnalyzer.class);
    private static final Map<String, Integer> RETRY_COUNTS = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        if (!ConfigManager.get().aiSelfHealingEnabled()) {
            return false;
        }

        String key = HealingSession.testKey(result);
        int maxRetries = ConfigManager.get().aiHealMaxRetries();
        int current = RETRY_COUNTS.getOrDefault(key, 0);

        if (current >= maxRetries) {
            LOG.info("Max healing retries ({}) reached for {}", maxRetries, key);
            HealingSession.clear(result);
            return false;
        }

        if (!HealingSession.isRetryApproved(result)) {
            LOG.info("No healing applied — skipping retry for {}", key);
            HealingSession.clear(result);
            return false;
        }

        RETRY_COUNTS.put(key, current + 1);
        LOG.info("Self-healing retry {}/{} for {}", current + 1, maxRetries, key);

        if (DriverManager.hasActiveDriver()) {
            DriverManager.quitDriver();
        }
        return true;
    }

    public static void resetRetryCount(String testKey) {
        RETRY_COUNTS.remove(testKey);
    }
}
