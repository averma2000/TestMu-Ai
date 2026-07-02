package com.testmu.core;

import com.testmu.agent.FailureAnalysisOrchestrator;
import com.testmu.agent.flaky.FlakyTestClassifier;
import com.testmu.agent.flaky.TestRunRecorder;
import com.testmu.agent.healing.HealingSession;
import com.testmu.agent.healing.model.HealingResult;
import com.testmu.agent.learning.LearningAdvisor;
import com.testmu.reporting.ExtentTestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(TestListener.class);
    private final FailureAnalysisOrchestrator failureOrchestrator = new FailureAnalysisOrchestrator();
    private final TestRunRecorder runRecorder = TestRunRecorder.getInstance();
    private final LearningAdvisor learningAdvisor = new LearningAdvisor();
    private final FlakyTestClassifier flakyClassifier = new FlakyTestClassifier();

    @Override
    public void onTestStart(ITestResult result) {
        String testClass = result.getTestClass().getRealClass().getSimpleName();
        String testMethod = result.getMethod().getMethodName();
        TestContextHolder.setTestKey(testClass + "." + testMethod);

        learningAdvisor.applyPreRunHealing(testClass, testMethod);

        if (flakyClassifier.isFlaky(testClass, testMethod)) {
            LOG.warn("Flaky test detected — pre-healing applied: {}.{}", testClass, testMethod);
        }

        LOG.info("Starting: {} [{}]", testMethod, result.getMethod().getGroups());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testClass = result.getTestClass().getRealClass().getSimpleName();
        String testMethod = result.getMethod().getMethodName();
        boolean healedRetry = HealingSession.getHealingResult(result) != null;

        runRecorder.record(testClass, testMethod, "PASS", healedRetry, null);

        if (healedRetry) {
            LOG.info("Passed on healing retry: {}", testMethod);
        } else {
            LOG.info("Passed: {} — continuing", testMethod);
        }
        HealingSession.clear(result);
        TestContextHolder.clear();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.error("Failed: {} - {}", result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "unknown");
        HealingResult healingResult = failureOrchestrator.handleFailure(result);
        if (healingResult.getAppliedCount() > 0) {
            ExtentTestManager.logInfo("Self-healing applied: " + String.join(", ", healingResult.getAppliedActions()));
            if (healingResult.isRetryRecommended()) {
                ExtentTestManager.logInfo("Retry scheduled with healed configuration");
            }
        }
        if (healingResult.isRetryRecommended()) {
            LOG.info("Self-healing applied — test will be retried: {}", result.getMethod().getMethodName());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testClass = result.getTestClass().getRealClass().getSimpleName();
        String testMethod = result.getMethod().getMethodName();
        runRecorder.record(testClass, testMethod, "SKIP", false, null);
        LOG.warn("Skipped: {}", testMethod);
        HealingSession.clear(result);
        TestContextHolder.clear();
    }
}
