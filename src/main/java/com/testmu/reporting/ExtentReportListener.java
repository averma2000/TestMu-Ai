package com.testmu.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.testmu.agent.healing.HealingSession;
import com.testmu.api.ApiContextHolder;
import com.testmu.config.ConfigManager;
import com.testmu.core.DriverManager;
import com.testmu.utils.ScreenshotHolder;
import com.testmu.utils.ScreenshotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IExecutionListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

public class ExtentReportListener implements ITestListener, ISuiteListener, IExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(ExtentReportListener.class);

    @Override
    public void onExecutionStart() {
        ensureReportInitialized();
    }

    @Override
    public void onExecutionFinish() {
        flushReportSafely();
    }

    @Override
    public void onStart(ISuite suite) {
        ensureReportInitialized();
    }

    @Override
    public void onFinish(ISuite suite) {
        flushReportSafely();
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (!ConfigManager.get().extentReportEnabled()) {
            return;
        }

        ensureReportInitialized();
        ScreenshotHolder.clear();

        String methodName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        if (description == null || description.isBlank()) {
            description = methodName;
        }

        ExtentTest extentTest = ExtentReportManager.createTest(methodName, description);
        if (extentTest == null) {
            LOG.warn("ExtentTest not created for {}", methodName);
            return;
        }

        extentTest.assignCategory(result.getMethod().getGroups());
        extentTest.assignAuthor(result.getTestClass().getRealClass().getSimpleName());
        extentTest.info("Test started: <b>" + methodName + "</b>");
        extentTest.info("Groups: " + Arrays.toString(result.getMethod().getGroups()));

        if (DriverManager.hasActiveDriver()) {
            extentTest.info("Test type: UI (Selenium)");
        } else {
            extentTest.info("Test type: API");
        }

        ExtentTestManager.setTest(extentTest);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (!ConfigManager.get().extentReportEnabled()) {
            return;
        }

        ExtentTest extentTest = ExtentTestManager.getTest();
        if (extentTest == null) {
            return;
        }

        String methodName = result.getMethod().getMethodName();
        logApiContext(extentTest);

        if (DriverManager.hasActiveDriver()) {
            String screenshot = ScreenshotUtils.captureOnPass(methodName);
            ExtentTestManager.attachScreenshot(screenshot, "Pass screenshot");
            extentTest.info("URL at pass: " + safeUrl());
        }

        if (HealingSession.getHealingResult(result) != null) {
            extentTest.pass("Test <b>PASSED</b> after self-healing retry");
        } else {
            extentTest.pass("Test <b>PASSED</b>");
        }

        cleanup();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (!ConfigManager.get().extentReportEnabled()) {
            return;
        }

        ExtentTest extentTest = ExtentTestManager.getTest();
        if (extentTest == null) {
            return;
        }

        String methodName = result.getMethod().getMethodName();
        Throwable throwable = result.getThrowable();
        logApiContext(extentTest);

        if (DriverManager.hasActiveDriver()) {
            String screenshot = ScreenshotUtils.captureOnFailure(methodName);
            ExtentTestManager.attachScreenshot(screenshot, "Failure screenshot");
            extentTest.info("URL at failure: " + safeUrl());
        }

        if (throwable != null) {
            extentTest.fail(throwable);
        } else {
            extentTest.fail("Test <b>FAILED</b>");
        }

        extentTest.log(Status.WARNING, "AI failure analysis will run if enabled");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (!ConfigManager.get().extentReportEnabled()) {
            return;
        }

        ExtentTest extentTest = ExtentTestManager.getTest();
        if (extentTest != null) {
            Throwable throwable = result.getThrowable();
            if (throwable != null) {
                extentTest.skip(throwable);
            } else {
                extentTest.skip("Test <b>SKIPPED</b>");
            }
        }
        cleanup();
    }

    private void ensureReportInitialized() {
        if (ConfigManager.get().extentReportEnabled()) {
            ExtentReportManager.initReport();
        }
    }

    private void flushReportSafely() {
        if (ConfigManager.get().extentReportEnabled()) {
            ExtentReportManager.flushReport();
        }
    }

    private void logApiContext(ExtentTest extentTest) {
        String url = ApiContextHolder.getLastRequestUrl();
        if (url == null) {
            return;
        }
        extentTest.info("API Request: " + url);
        Integer status = ApiContextHolder.getLastStatusCode();
        if (status != null) {
            extentTest.info("API Status: " + status);
        }
        String body = ApiContextHolder.getLastResponseBody();
        if (body != null && !body.isBlank()) {
            extentTest.info("API Response:<br/><pre>" + escapeHtml(body) + "</pre>");
        }
    }

    private String safeUrl() {
        try {
            return DriverManager.getDriver().getCurrentUrl();
        } catch (Exception e) {
            return "unavailable";
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void cleanup() {
        ExtentTestManager.removeTest();
        ScreenshotHolder.clear();
    }
}
