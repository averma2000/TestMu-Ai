package com.testmu.agent.healing;

import com.testmu.agent.healing.model.HealedLocatorEntry;
import com.testmu.agent.healing.model.HealingAction;
import com.testmu.agent.healing.model.HealingResult;
import com.testmu.agent.model.FailureContext;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelfHealingEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SelfHealingEngine.class);
    private static final Pattern BY_PATTERN = Pattern.compile(
            "By\\.(id|cssSelector|xpath|name|className|tagName|linkText|partialLinkText)\\(([^)]+)\\)"
    );

    private final FrameworkConfig config = ConfigManager.get();
    private final LocatorHealingStore locatorStore = LocatorHealingStore.getInstance();
    private final WaitHealingStore waitStore = WaitHealingStore.getInstance();
    private final ApiHealingStore apiStore = ApiHealingStore.getInstance();
    private final PageObjectPatcher pageObjectPatcher = new PageObjectPatcher();

    public HealingResult apply(List<HealingAction> actions, FailureContext context) {
        if (!config.aiSelfHealingEnabled() || actions == null || actions.isEmpty()) {
            return HealingResult.none();
        }

        List<String> applied = new ArrayList<>();
        int skipped = 0;
        String sourceTest = context.testClass() + "." + context.testMethod();
        String testKey = sourceTest;

        for (HealingAction action : actions) {
            if (action.getConfidence() < config.aiHealMinConfidence()) {
                skipped++;
                continue;
            }

            try {
                switch (normalizeType(action.getActionType())) {
                    case "LOCATOR_UPDATE" -> {
                        if (applyLocatorUpdate(action, sourceTest)) {
                            applied.add("LOCATOR: " + action.getTargetClass() + "." + action.getElementName());
                        } else {
                            skipped++;
                        }
                    }
                    case "WAIT_INCREASE" -> {
                        String pageClass = normalizePageClass(action.getTargetClass(), context);
                        if (pageClass != null && action.getWaitSeconds() != null) {
                            waitStore.setWaitSeconds(pageClass, action.getWaitSeconds());
                            applied.add("WAIT: " + pageClass + " -> " + action.getWaitSeconds() + "s");
                        } else {
                            skipped++;
                        }
                    }
                    case "API_EXPECTATION_UPDATE" -> {
                        if (action.getApiEndpoint() != null && action.getExpectedStatusCode() != null) {
                            apiStore.save(testKey, action.getApiEndpoint(), action.getExpectedStatusCode());
                            applied.add("API: " + action.getApiEndpoint() + " -> " + action.getExpectedStatusCode());
                        } else {
                            skipped++;
                        }
                    }
                    default -> skipped++;
                }
            } catch (Exception e) {
                LOG.warn("Failed to apply healing action: {}", action.getActionType(), e);
                skipped++;
            }
        }

        boolean retryRecommended = !applied.isEmpty();
        LOG.info("Self-healing applied {}/{} action(s) for {}", applied.size(), actions.size(), sourceTest);
        return new HealingResult(applied.size(), skipped, applied, retryRecommended);
    }

    public List<HealingAction> extractFromCodeSnippet(String codeSnippet) {
        List<HealingAction> actions = new ArrayList<>();
        if (codeSnippet == null || codeSnippet.isBlank()) {
            return actions;
        }

        Matcher fieldMatcher = Pattern.compile(
                "private\\s+final\\s+By\\s+(\\w+)\\s*=\\s*(By\\.[^;]+);"
        ).matcher(codeSnippet);

        String pageClass = extractPageClass(codeSnippet);
        while (fieldMatcher.find()) {
            HealingAction action = new HealingAction();
            action.setActionType("LOCATOR_UPDATE");
            action.setTargetClass(pageClass);
            action.setElementName(fieldMatcher.group(1));
            parseByExpression(fieldMatcher.group(2), action);
            action.setConfidence(0.75);
            actions.add(action);
        }
        return actions;
    }

    private boolean applyLocatorUpdate(HealingAction action, String sourceTest) {
        if (action.getTargetClass() == null || action.getElementName() == null
                || action.getLocatorStrategy() == null || action.getLocatorValue() == null) {
            return false;
        }

        By healedBy = LocatorParser.toBy(action.getLocatorStrategy(), action.getLocatorValue());
        HealedLocatorEntry entry = locatorStore.buildEntry(
                action.getTargetClass(),
                action.getElementName(),
                action.getLocatorStrategy(),
                action.getLocatorValue(),
                healedBy,
                action.getConfidence(),
                sourceTest
        );
        locatorStore.save(entry);

        if (config.aiHealPatchSource()) {
            pageObjectPatcher.patchLocator(
                    action.getTargetClass(),
                    action.getElementName(),
                    action.getLocatorStrategy(),
                    action.getLocatorValue()
            );
        }
        return true;
    }

    private void parseByExpression(String byExpr, HealingAction action) {
        Matcher matcher = BY_PATTERN.matcher(byExpr.trim());
        if (matcher.find()) {
            action.setLocatorStrategy(mapStrategy(matcher.group(1)));
            action.setLocatorValue(unquote(matcher.group(2)));
        }
    }

    private String extractPageClass(String code) {
        Matcher m = Pattern.compile("class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : "UnknownPage";
    }

    private String mapStrategy(String seleniumStrategy) {
        return switch (seleniumStrategy) {
            case "cssSelector" -> "css";
            case "className" -> "className";
            case "tagName" -> "tagName";
            case "linkText" -> "linkText";
            case "partialLinkText" -> "partialLinkText";
            default -> seleniumStrategy;
        };
    }

    private String unquote(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }

    private String normalizePageClass(String targetClass, FailureContext context) {
        if (targetClass == null || targetClass.isBlank()) {
            return inferPageFromTest(context.testClass());
        }
        if (targetClass.endsWith("Page")) {
            return targetClass;
        }
        if (targetClass.endsWith("Test")) {
            return targetClass.replace("Test", "Page");
        }
        if (targetClass.contains("Login")) {
            return "LoginPage";
        }
        if (targetClass.contains("Dashboard")) {
            return "DashboardPage";
        }
        return targetClass;
    }

    private String inferPageFromTest(String testClass) {
        if (testClass.contains("Login")) {
            return "LoginPage";
        }
        if (testClass.contains("Dashboard")) {
            return "DashboardPage";
        }
        if (testClass.endsWith("Test")) {
            return testClass.replace("Test", "Page");
        }
        return testClass;
    }
}
