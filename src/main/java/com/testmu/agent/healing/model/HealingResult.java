package com.testmu.agent.healing.model;

import java.util.ArrayList;
import java.util.List;

public class HealingResult {

    private final int appliedCount;
    private final int skippedCount;
    private final List<String> appliedActions;
    private final boolean retryRecommended;

    public HealingResult(int appliedCount, int skippedCount, List<String> appliedActions, boolean retryRecommended) {
        this.appliedCount = appliedCount;
        this.skippedCount = skippedCount;
        this.appliedActions = appliedActions != null ? appliedActions : new ArrayList<>();
        this.retryRecommended = retryRecommended;
    }

    public int getAppliedCount() {
        return appliedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public List<String> getAppliedActions() {
        return appliedActions;
    }

    public boolean isRetryRecommended() {
        return retryRecommended;
    }

    public static HealingResult none() {
        return new HealingResult(0, 0, List.of(), false);
    }
}
