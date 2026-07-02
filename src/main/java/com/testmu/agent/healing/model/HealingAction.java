package com.testmu.agent.healing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HealingAction {

    private String actionType;
    private String targetClass;
    private String elementName;
    private String locatorStrategy;
    private String locatorValue;
    private String apiEndpoint;
    private Integer expectedStatusCode;
    private Integer waitSeconds;
    private double confidence;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getLocatorStrategy() {
        return locatorStrategy;
    }

    public void setLocatorStrategy(String locatorStrategy) {
        this.locatorStrategy = locatorStrategy;
    }

    public String getLocatorValue() {
        return locatorValue;
    }

    public void setLocatorValue(String locatorValue) {
        this.locatorValue = locatorValue;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public Integer getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(Integer expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public Integer getWaitSeconds() {
        return waitSeconds;
    }

    public void setWaitSeconds(Integer waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
