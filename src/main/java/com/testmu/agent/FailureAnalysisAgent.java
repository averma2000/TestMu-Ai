package com.testmu.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testmu.agent.model.FailureAnalysis;
import com.testmu.agent.model.FailureContext;
import com.testmu.agent.model.FailureInsightRecord;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FailureAnalysisAgent {

    private static final Logger LOG = LoggerFactory.getLogger(FailureAnalysisAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert test automation engineer specializing in Selenium, TestNG, and REST API testing.
            Analyze test failures and provide actionable remediation guidance.
            Respond ONLY with valid JSON matching this schema (no markdown fences):
            {
              "rootCause": "concise root cause",
              "failureCategory": "locator|timing|assertion|test-data|environment|api|unknown",
              "summary": "2-3 sentence summary",
              "suggestedFixes": ["fix 1", "fix 2"],
              "implementationSteps": ["step 1", "step 2"],
              "correctedCodeSnippet": "Java code snippet to fix the issue",
              "healingRecommendation": "how to self-heal this in future runs",
              "confidence": 0.85,
              "healingActions": [
                {
                  "actionType": "LOCATOR_UPDATE",
                  "targetClass": "LoginPage",
                  "elementName": "usernameField",
                  "locatorStrategy": "css",
                  "locatorValue": "#user-name",
                  "confidence": 0.9
                },
                {
                  "actionType": "WAIT_INCREASE",
                  "targetClass": "LoginPage",
                  "waitSeconds": 20,
                  "confidence": 0.8
                },
                {
                  "actionType": "API_EXPECTATION_UPDATE",
                  "apiEndpoint": "/users/2",
                  "expectedStatusCode": 200,
                  "confidence": 0.85
                }
              ]
            }
            Include healingActions with machine-readable fixes. Use locatorStrategy: id|css|xpath|name|className.
            """;

    private final FrameworkConfig config = ConfigManager.get();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FailureAnalysis analyze(FailureContext context) {
        return analyze(context, List.of(), "");
    }

    public FailureAnalysis analyze(FailureContext context, List<FailureInsightRecord> history,
                                   String historySummary) {
        String apiKey = AiChatModelFactory.resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("{} API key not set — skipping LLM analysis", config.aiProvider());
            return buildFallbackAnalysis(apiKeyMessage());
        }

        try {
            ChatLanguageModel model = AiChatModelFactory.create();

            List<Content> contents = new ArrayList<>();
            contents.add(TextContent.from(buildUserPrompt(context, historySummary)));

            if (context.screenshotPath() != null) {
                Path screenshot = Path.of(context.screenshotPath());
                if (Files.exists(screenshot)) {
                    byte[] imageBytes = Files.readAllBytes(screenshot);
                    String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                    contents.add(ImageContent.from(base64, "image/png"));
                }
            }

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(contents))
                    .build();

            String response = model.chat(chatRequest).aiMessage().text();

            return parseAnalysis(response);
        } catch (Exception e) {
            LOG.error("LLM analysis failed for {}.{}", context.testClass(), context.testMethod(), e);
            return buildFallbackAnalysis("LLM analysis failed: " + e.getMessage());
        }
    }

    private String apiKeyMessage() {
        return switch (config.aiProvider().toLowerCase()) {
            case "gemini" -> "Gemini API key not configured. Set GEMINI_API_KEY in .env or environment.";
            case "openai" -> "OpenAI API key not configured. Set OPENAI_API_KEY in .env or environment.";
            case "openrouter" -> "OpenRouter API key not configured. Set OPENROUTER_API_KEY in .env (sk-or-v1-...).";
            default -> "AI API key not configured for provider: " + config.aiProvider();
        };
    }

    private String buildUserPrompt(FailureContext context, String historySummary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this test failure and suggest how to fix it.\n\n");

        if (historySummary != null && !historySummary.isBlank()) {
            prompt.append(historySummary).append("\n");
            prompt.append("Use prior failure patterns to improve root-cause analysis and healing actions.\n\n");
        }

        prompt.append("## Test Info\n");
        prompt.append("- Class: ").append(context.testClass()).append("\n");
        prompt.append("- Method: ").append(context.testMethod()).append("\n");
        prompt.append("- Description: ").append(context.testDescription()).append("\n");
        prompt.append("- Groups: ").append(String.join(", ", context.testGroups())).append("\n");
        prompt.append("- Type: ").append(context.uiTest() ? "UI (Selenium)" : "API").append("\n\n");

        prompt.append("## Exception\n");
        prompt.append("- Type: ").append(context.exceptionType()).append("\n");
        prompt.append("- Message: ").append(context.exceptionMessage()).append("\n\n");

        prompt.append("## Stack Trace\n```\n").append(context.stackTrace()).append("\n```\n\n");

        if (context.currentUrl() != null) {
            prompt.append("## Current URL\n").append(context.currentUrl()).append("\n\n");
        }

        if (context.apiRequestUrl() != null) {
            prompt.append("## API Request\n");
            prompt.append("- URL: ").append(context.apiRequestUrl()).append("\n");
            prompt.append("- Status: ").append(context.apiStatusCode()).append("\n");
            prompt.append("- Response: ").append(context.apiResponseBody()).append("\n\n");
        }

        if (context.htmlSource() != null && !context.htmlSource().isBlank()) {
            prompt.append("## HTML Source (DOM)\n```html\n").append(context.htmlSource()).append("\n```\n\n");
        }

        if (context.screenshotPath() != null) {
            prompt.append("A screenshot of the failure state is attached as an image.\n");
        }

        prompt.append("""
                Provide:
                1. Root cause analysis
                2. Category of failure
                3. Specific fixes for locators, waits, assertions, or API expectations
                4. Step-by-step implementation to correct the test or page object
                5. A corrected Java code snippet ready to apply
                6. Self-healing recommendation for future runs
                """);

        return prompt.toString();
    }

    private FailureAnalysis parseAnalysis(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, FailureAnalysis.class);
        } catch (Exception e) {
            LOG.warn("Failed to parse LLM JSON response, using raw text", e);
            return buildFallbackAnalysis(response);
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }

    private FailureAnalysis buildFallbackAnalysis(String message) {
        FailureAnalysis analysis = new FailureAnalysis();
        analysis.setRootCause("Analysis unavailable");
        analysis.setFailureCategory("unknown");
        analysis.setSummary(message);
        analysis.setSuggestedFixes(List.of("Review stack trace and screenshot manually"));
        analysis.setImplementationSteps(List.of("Inspect failure context in the generated report"));
        analysis.setCorrectedCodeSnippet("// Manual review required");
        analysis.setHealingRecommendation("Configure OPENROUTER_API_KEY, GEMINI_API_KEY, or OPENAI_API_KEY and re-run");
        analysis.setConfidence(0.0);
        return analysis;
    }
}
