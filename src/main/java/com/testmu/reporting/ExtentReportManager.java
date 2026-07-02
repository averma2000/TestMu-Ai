package com.testmu.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ExtentReportManager {

    private static final Logger LOG = LoggerFactory.getLogger(ExtentReportManager.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static ExtentReports extentReports;
    private static String reportFilePath;
    private static String latestReportPath;
    private static boolean shutdownHookRegistered;

    private ExtentReportManager() {
    }

    public static synchronized void initReport() {
        FrameworkConfig config = ConfigManager.get();
        if (!config.extentReportEnabled()) {
            return;
        }
        if (extentReports != null) {
            return;
        }

        try {
            Path reportDir = Path.of(config.extentReportDir());
            Files.createDirectories(reportDir);

            String timestamp = LocalDateTime.now().format(FILE_TS);
            String fileName = config.extentReportName() + "_" + timestamp + ".html";
            reportFilePath = reportDir.resolve(fileName).toAbsolutePath().toString();
            latestReportPath = reportDir.resolve(config.extentReportName() + "_latest.html").toAbsolutePath().toString();

            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportFilePath);
            sparkReporter.config().setDocumentTitle("TestMu AI Automation Report");
            sparkReporter.config().setReportName("TestMu AI — Test Execution Report");
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            extentReports.setSystemInfo("Project", "TestMu AI Framework");
            extentReports.setSystemInfo("Browser", config.browser());
            extentReports.setSystemInfo("Base URL", config.baseUrl());
            extentReports.setSystemInfo("API Base URL", config.apiBaseUrl());
            extentReports.setSystemInfo("AI Agent", String.valueOf(config.aiAgentEnabled()));
            extentReports.setSystemInfo("Self-Healing", String.valueOf(config.aiSelfHealingEnabled()));
            extentReports.setSystemInfo("OS", System.getProperty("os.name"));
            extentReports.setSystemInfo("Java", System.getProperty("java.version"));

            registerShutdownHook();
            LOG.info("Extent report initialized: {}", reportFilePath);
        } catch (Exception e) {
            LOG.error("Failed to initialize Extent report", e);
        }
    }

    public static ExtentTest createTest(String name, String description) {
        if (extentReports == null) {
            initReport();
        }
        if (extentReports == null) {
            return null;
        }
        return extentReports.createTest(name, description);
    }

    public static synchronized void flushReport() {
        if (extentReports == null) {
            return;
        }
        try {
            extentReports.flush();
            copyToLatestReport();
            LOG.info("Extent report saved: {}", reportFilePath);
            LOG.info("Latest Extent report: {}", latestReportPath);
        } catch (Exception e) {
            LOG.error("Failed to flush Extent report", e);
        } finally {
            extentReports = null;
        }
    }

    private static void copyToLatestReport() {
        if (reportFilePath == null || latestReportPath == null) {
            return;
        }
        try {
            Path source = Path.of(reportFilePath);
            if (Files.exists(source)) {
                Files.copy(source, Path.of(latestReportPath), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOG.warn("Failed to copy latest Extent report", e);
        }
    }

    private static void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (extentReports != null) {
                LOG.info("Flushing Extent report via shutdown hook");
                flushReport();
            }
        }, "extent-report-flush"));
        shutdownHookRegistered = true;
    }

    public static String getReportFilePath() {
        return reportFilePath;
    }

    public static String getLatestReportPath() {
        return latestReportPath;
    }
}
