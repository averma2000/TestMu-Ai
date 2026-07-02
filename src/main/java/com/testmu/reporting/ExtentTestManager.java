package com.testmu.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> EXTENT_TEST = new ThreadLocal<>();

    private ExtentTestManager() {
    }

    public static ExtentTest getTest() {
        return EXTENT_TEST.get();
    }

    public static void setTest(ExtentTest test) {
        EXTENT_TEST.set(test);
    }

    public static void removeTest() {
        EXTENT_TEST.remove();
    }

    public static void logInfo(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.log(Status.INFO, message);
        }
    }

    public static void attachScreenshot(String screenshotPath, String title) {
        if (screenshotPath == null || screenshotPath.isBlank()) {
            return;
        }
        ExtentTest test = getTest();
        if (test != null) {
            try {
                byte[] bytes = Files.readAllBytes(Path.of(screenshotPath));
                String base64 = Base64.getEncoder().encodeToString(bytes);
                test.addScreenCaptureFromBase64String(base64, title);
            } catch (Exception e) {
                test.info(title + " (screenshot path: " + screenshotPath + ")");
            }
        }
    }
}
