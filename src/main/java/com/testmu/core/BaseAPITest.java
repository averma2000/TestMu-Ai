package com.testmu.core;

import com.testmu.api.ApiContextHolder;
import com.testmu.api.RestApiClient;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import com.testmu.agent.healing.HealingAnnotationTransformer;
import com.testmu.reporting.ExtentReportListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

@Listeners({TestListener.class, ExtentReportListener.class, HealingAnnotationTransformer.class})
public abstract class BaseAPITest {

    protected FrameworkConfig config;
    protected RestApiClient apiClient;

    @BeforeMethod(alwaysRun = true)
    public void setUpAPI() {
        config = ConfigManager.get();
        apiClient = new RestApiClient(config.apiBaseUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownAPI() {
        ApiContextHolder.clear();
    }
}
