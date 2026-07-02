package com.testmu.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FailureAnalysis {

    private String rootCause;
    private String failureCategory;
    private String summary;
    private List<String> suggestedFixes = new ArrayList<>();
    private List<String> implementationSteps = new ArrayList<>();
    private String correctedCodeSnippet;
    private String healingRecommendation;
    private double confidence;
    private List<com.testmu.agent.healing.model.HealingAction> healingActions = new ArrayList<>();

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(String failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSuggestedFixes() {
        return suggestedFixes;
    }

    public void setSuggestedFixes(List<String> suggestedFixes) {
        this.suggestedFixes = suggestedFixes != null ? suggestedFixes : new ArrayList<>();
    }

    public List<String> getImplementationSteps() {
        return implementationSteps;
    }

    public void setImplementationSteps(List<String> implementationSteps) {
        this.implementationSteps = implementationSteps != null ? implementationSteps : new ArrayList<>();
    }

    public String getCorrectedCodeSnippet() {
        return correctedCodeSnippet;
    }

    public void setCorrectedCodeSnippet(String correctedCodeSnippet) {
        this.correctedCodeSnippet = correctedCodeSnippet;
    }

    public String getHealingRecommendation() {
        return healingRecommendation;
    }

    public void setHealingRecommendation(String healingRecommendation) {
        this.healingRecommendation = healingRecommendation;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<com.testmu.agent.healing.model.HealingAction> getHealingActions() {
        return healingActions;
    }

    public void setHealingActions(List<com.testmu.agent.healing.model.HealingAction> healingActions) {
        this.healingActions = healingActions != null ? healingActions : new ArrayList<>();
    }
}
