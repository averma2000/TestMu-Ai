package com.testmu.core;

import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import com.testmu.agent.healing.HealingAnnotationTransformer;
import com.testmu.reporting.ExtentReportListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

@Listeners({TestListener.class, ExtentReportListener.class, HealingAnnotationTransformer.class})
public abstract class BaseUITest {

    protected FrameworkConfig config;
    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUpUI() {
        config = ConfigManager.get();
        driver = DriverManager.getDriver();
        driver.get(config.baseUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownUI() {
        DriverManager.quitDriver();
    }
}
