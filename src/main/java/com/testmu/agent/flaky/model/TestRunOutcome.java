package com.testmu.agent.flaky.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRunOutcome {

    private String testClass;
    private String testMethod;
    private String status;
    private boolean healedRetry;
    private String failureCategory;
    private LocalDateTime runAt;

    public TestRunOutcome() {
    }

    public TestRunOutcome(String testClass, String testMethod, String status,
                          boolean healedRetry, String failureCategory, LocalDateTime runAt) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.status = status;
        this.healedRetry = healedRetry;
        this.failureCategory = failureCategory;
        this.runAt = runAt;
    }

    public String getTestClass() {
        return testClass;
    }

    public void setTestClass(String testClass) {
        this.testClass = testClass;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHealedRetry() {
        return healedRetry;
    }

    public void setHealedRetry(boolean healedRetry) {
        this.healedRetry = healedRetry;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(String failureCategory) {
        this.failureCategory = failureCategory;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }

    public String testKey() {
        return testClass + "." + testMethod;
    }
}
