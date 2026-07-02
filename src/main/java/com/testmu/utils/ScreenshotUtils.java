package com.testmu.utils;

import com.testmu.config.ConfigManager;
import com.testmu.core.DriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {
    }

    public static String captureOnFailure(String testName) {
        if (!ConfigManager.get().screenshotOnFailure()) {
            return getOrReuse(null);
        }
        return capture(testName);
    }

    public static String captureOnPass(String testName) {
        if (!ConfigManager.get().screenshotOnPass()) {
            return null;
        }
        return capture(testName);
    }

    public static String capture(String testName) {
        if (!DriverManager.hasActiveDriver()) {
            return null;
        }

        WebDriver driver = DriverManager.getDriver();
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return null;
        }

        try {
            Path dir = Path.of(ConfigManager.get().reportsDir(), "screenshots");
            Files.createDirectories(dir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String safeName = testName.replaceAll("[^a-zA-Z0-9_-]", "_");
            File dest = dir.resolve(safeName + "_" + timestamp + ".png").toFile();
            File src = screenshotDriver.getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(src, dest);
            LOG.info("Screenshot saved: {}", dest.getAbsolutePath());
            ScreenshotHolder.set(dest.getAbsolutePath());
            return dest.getAbsolutePath();
        } catch (IOException e) {
            LOG.warn("Failed to capture screenshot for {}", testName, e);
            return null;
        }
    }

    public static String getOrReuse(String fallbackCapture) {
        String existing = ScreenshotHolder.get();
        return existing != null ? existing : fallbackCapture;
    }
}
