package com.testmu.agent.flaky.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestStabilityProfile {

    private String testClass;
    private String testMethod;
    private int totalRuns;
    private int passes;
    private int failures;
    private int healedPasses;
    private double flakyScore;
    private String classification;
    private String dominantFailureCategory;
    private String recommendation;

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

    public int getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(int totalRuns) {
        this.totalRuns = totalRuns;
    }

    public int getPasses() {
        return passes;
    }

    public void setPasses(int passes) {
        this.passes = passes;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getHealedPasses() {
        return healedPasses;
    }

    public void setHealedPasses(int healedPasses) {
        this.healedPasses = healedPasses;
    }

    public double getFlakyScore() {
        return flakyScore;
    }

    public void setFlakyScore(double flakyScore) {
        this.flakyScore = flakyScore;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getDominantFailureCategory() {
        return dominantFailureCategory;
    }

    public void setDominantFailureCategory(String dominantFailureCategory) {
        this.dominantFailureCategory = dominantFailureCategory;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String testKey() {
        return testClass + "." + testMethod;
    }
}
