package com.testmu.agent;

import com.testmu.agent.model.FailureContext;
import com.testmu.api.ApiContextHolder;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import com.testmu.core.DriverManager;
import com.testmu.utils.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class FailureContextCollector {

    private final FrameworkConfig config = ConfigManager.get();

    public FailureContext collect(ITestResult result) {
        Throwable throwable = result.getThrowable();
        String testClass = result.getTestClass().getRealClass().getSimpleName();
        String testMethod = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        List<String> groups = Arrays.asList(result.getMethod().getGroups());

        String exceptionType = throwable != null ? throwable.getClass().getName() : "Unknown";
        String exceptionMessage = throwable != null ? throwable.getMessage() : "No exception message";
        String stackTrace = toStackTrace(throwable);

        boolean uiTest = DriverManager.hasActiveDriver();
        String screenshotPath = null;
        String currentUrl = null;
        String htmlSource = null;

        if (uiTest) {
            WebDriver driver = DriverManager.getDriver();
            screenshotPath = ScreenshotUtils.getOrReuse(
                    ScreenshotUtils.captureOnFailure(testMethod));
            currentUrl = safeCurrentUrl(driver);
            htmlSource = truncateHtml(driver.getPageSource());
        }

        String apiRequestUrl = ApiContextHolder.getLastRequestUrl();
        Integer apiStatusCode = ApiContextHolder.getLastStatusCode();
        String apiResponseBody = ApiContextHolder.getLastResponseBody();

        return new FailureContext(
                testClass,
                testMethod,
                description != null ? description : "",
                groups,
                LocalDateTime.now(),
                exceptionType,
                exceptionMessage,
                stackTrace,
                currentUrl,
                htmlSource,
                screenshotPath,
                apiRequestUrl,
                apiStatusCode,
                apiResponseBody,
                uiTest
        );
    }

    private String safeCurrentUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return "unavailable";
        }
    }

    private String truncateHtml(String html) {
        if (html == null) {
            return "";
        }
        int maxChars = config.aiHtmlMaxChars();
        if (html.length() <= maxChars) {
            return html;
        }
        return html.substring(0, maxChars) + "\n<!-- truncated for LLM analysis -->";
    }

    private String toStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
