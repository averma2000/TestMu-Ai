package com.testmu.agent;

import com.testmu.agent.healing.HealingSession;
import com.testmu.agent.healing.SelfHealingEngine;
import com.testmu.agent.healing.model.HealingAction;
import com.testmu.agent.healing.model.HealingResult;
import com.testmu.agent.flaky.FlakyTestClassifier;
import com.testmu.agent.flaky.TestRunRecorder;
import com.testmu.agent.learning.LearningAdvisor;
import com.testmu.agent.model.FailureAnalysis;
import com.testmu.agent.model.FailureContext;
import com.testmu.agent.model.FailureInsightRecord;
import com.testmu.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;

import java.util.ArrayList;
import java.util.List;

public class FailureAnalysisOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(FailureAnalysisOrchestrator.class);

    private final FailureContextCollector contextCollector = new FailureContextCollector();
    private final FailureAnalysisAgent analysisAgent = new FailureAnalysisAgent();
    private final FailureInsightReportGenerator reportGenerator = new FailureInsightReportGenerator();
    private final AgentMemoryStore memoryStore = new AgentMemoryStore();
    private final SelfHealingEngine healingEngine = new SelfHealingEngine();
    private final LearningAdvisor learningAdvisor = new LearningAdvisor();
    private final FlakyTestClassifier flakyClassifier = new FlakyTestClassifier();
    private final TestRunRecorder runRecorder = TestRunRecorder.getInstance();

    public HealingResult handleFailure(ITestResult result) {
        if (!ConfigManager.get().aiAgentEnabled()) {
            LOG.debug("AI agent disabled — skipping failure analysis");
            return HealingResult.none();
        }

        try {
            LOG.info("AI agent analyzing failure: {}", result.getMethod().getMethodName());

            FailureContext context = contextCollector.collect(result);

            List<FailureInsightRecord> history = memoryStore.loadHistory(
                    context.testClass(), context.testMethod());
            String historySummary = learningAdvisor.buildHistorySummary(
                    context.testClass(), context.testMethod());

            FailureAnalysis analysis = analysisAgent.analyze(context, history, historySummary);

            List<HealingAction> actions = resolveHealingActions(analysis);
            HealingResult healingResult = healingEngine.apply(actions, context);
            HealingSession.recordHealing(result, healingResult);

            FailureInsightRecord record = reportGenerator.generate(context, analysis, healingResult);
            memoryStore.save(record);
            flakyClassifier.enrichFromFailureHistory(
                    context.testClass(), context.testMethod(), history);

            runRecorder.record(context.testClass(), context.testMethod(), "FAIL", false,
                    analysis.getFailureCategory());

            if (history.size() > 0) {
                LOG.info("Agent has {} prior failure(s) for {}.{} — learning from history",
                        history.size(), context.testClass(), context.testMethod());
            }

            LOG.info("Failure analysis complete | category={} | confidence={}% | healing={} | retry={} | report={}",
                    analysis.getFailureCategory(),
                    (int) (analysis.getConfidence() * 100),
                    healingResult.getAppliedCount(),
                    healingResult.isRetryRecommended(),
                    record.htmlReportPath());

            return healingResult;
        } catch (Exception e) {
            LOG.error("Failure analysis orchestration failed", e);
            return HealingResult.none();
        }
    }

    private List<HealingAction> resolveHealingActions(FailureAnalysis analysis) {
        List<HealingAction> actions = new ArrayList<>();
        if (analysis.getHealingActions() != null) {
            actions.addAll(analysis.getHealingActions());
        }
        if (actions.isEmpty()) {
            actions.addAll(healingEngine.extractFromCodeSnippet(analysis.getCorrectedCodeSnippet()));
        }
        return actions;
    }
}
