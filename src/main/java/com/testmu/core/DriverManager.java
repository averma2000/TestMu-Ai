package com.testmu.core;

import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver getDriver() {
        if (DRIVER.get() == null) {
            DRIVER.set(createDriver());
        }
        return DRIVER.get();
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }

    public static boolean hasActiveDriver() {
        return DRIVER.get() != null;
    }

    private static WebDriver createDriver() {
        FrameworkConfig config = ConfigManager.get();
        String browser = config.browser().toLowerCase();

        WebDriver driver = switch (browser) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = new FirefoxOptions();
                if (config.headless()) {
                    options.addArguments("-headless");
                }
                yield new FirefoxDriver(options);
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions options = new EdgeOptions();
                if (config.headless()) {
                    options.addArguments("--headless=new");
                }
                yield new EdgeDriver(options);
            }
            default -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if (config.headless()) {
                    options.addArguments("--headless=new");
                }
                options.addArguments(
                        "--disable-gpu",
                        "--no-sandbox",
                        "--window-size=1920,1080",
                        "--disable-notifications",
                        "--disable-infobars"
                );
                Map<String, Object> chromePrefs = new HashMap<>();
                chromePrefs.put("profile.default_content_setting_values.notifications", 2);
                chromePrefs.put("profile.default_content_setting_values.popups", 2);
                options.setExperimentalOption("prefs", chromePrefs);
                yield new ChromeDriver(options);
            }
        };

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.implicitWaitSeconds()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.pageLoadTimeoutSeconds()));
        driver.manage().window().maximize();
        return driver;
    }
}
