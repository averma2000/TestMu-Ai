package com.testmu.agent.healing;

import com.testmu.agent.healing.model.HealingResult;
import org.testng.ITestResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HealingSession {

    private static final Map<String, HealingResult> HEALING_RESULTS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> RETRY_APPROVED = new ConcurrentHashMap<>();

    private HealingSession() {
    }

    public static String testKey(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "."
                + result.getMethod().getMethodName();
    }

    public static void recordHealing(ITestResult result, HealingResult healingResult) {
        String key = testKey(result);
        HEALING_RESULTS.put(key, healingResult);
        RETRY_APPROVED.put(key, healingResult.isRetryRecommended());
    }

    public static boolean isRetryApproved(ITestResult result) {
        return Boolean.TRUE.equals(RETRY_APPROVED.get(testKey(result)));
    }

    public static HealingResult getHealingResult(ITestResult result) {
        return HEALING_RESULTS.get(testKey(result));
    }

    public static void clear(ITestResult result) {
        String key = testKey(result);
        HEALING_RESULTS.remove(key);
        RETRY_APPROVED.remove(key);
        HealingRetryAnalyzer.resetRetryCount(key);
    }
}
