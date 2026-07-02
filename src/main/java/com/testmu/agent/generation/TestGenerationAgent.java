package com.testmu.agent.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testmu.agent.AiChatModelFactory;
import com.testmu.agent.generation.model.GeneratedTestSuite;
import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestGenerationAgent {

    private static final Logger LOG = LoggerFactory.getLogger(TestGenerationAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert test automation engineer writing Selenium + TestNG + REST API tests in Java.
            Generate runnable test methods for the TestMu AI framework.
            Respond ONLY with valid JSON (no markdown fences) matching this schema:
            {
              "module": "login|dashboard|api",
              "className": "GeneratedLoginTest",
              "baseClass": "BaseUITest or BaseAPITest",
              "imports": ["com.testmu.pages.LoginPage", "..."],
              "tests": [
                {
                  "methodName": "testValidLoginSuccess",
                  "description": "what the test verifies",
                  "groups": ["login", "pass", "generated"],
                  "scenario": "pass|edge|fail",
                  "methodBody": "full Java method body WITHOUT @Test annotation, using 4-space indent"
                }
              ]
            }
            Rules:
            - UI tests extend BaseUITest and use LoginPage, DashboardPage, config.validUsername(), config.validPassword()
            - API tests extend BaseAPITest and use apiClient, ApiAssertionHelper.assertStatusCode(response, code)
            - Each module needs at least 1 pass, 1 edge, and 1 fail scenario
            - Use org.testng.Assert for assertions
            - methodBody must be compilable Java (no markdown)
            - Include groups: module name, scenario, and "generated"
            """;

    private final FrameworkConfig config = ConfigManager.get();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneratedTestSuite generate(String moduleSpec) {
        String apiKey = AiChatModelFactory.resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("API key not set — using fallback test templates for module spec");
            return buildFallbackSuite(moduleSpec);
        }

        try {
            ChatLanguageModel model = AiChatModelFactory.create();
            ChatRequest request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(SYSTEM_PROMPT),
                            UserMessage.from(buildUserPrompt(moduleSpec))
                    )
                    .build();

            String response = model.chat(request).aiMessage().text();
            String json = extractJson(response);
            GeneratedTestSuite suite = objectMapper.readValue(json, GeneratedTestSuite.class);
            LOG.info("LLM generated {} test(s) for module {}", suite.getTests().size(), suite.getModule());
            return suite;
        } catch (Exception e) {
            LOG.error("LLM test generation failed, using fallback templates", e);
            return buildFallbackSuite(moduleSpec);
        }
    }

    private String buildUserPrompt(String moduleSpec) {
        return """
                Generate TestNG test cases for this module specification:

                %s

                Application context:
                - UI base URL: %s
                - API base URL: %s
                - Valid username: %s
                - Valid password: %s

                Produce exactly 3 tests: one pass, one edge, one fail scenario.
                """.formatted(
                moduleSpec,
                config.baseUrl(),
                config.apiBaseUrl(),
                config.validUsername(),
                config.validPassword()
        );
    }

    private GeneratedTestSuite buildFallbackSuite(String moduleSpec) {
        String module = detectModule(moduleSpec);
        return switch (module) {
            case "login" -> fallbackLoginSuite();
            case "dashboard" -> fallbackDashboardSuite();
            case "api" -> fallbackApiSuite();
            default -> fallbackLoginSuite();
        };
    }

    private String detectModule(String spec) {
        String lower = spec.toLowerCase();
        if (lower.contains("dashboard") || lower.contains("inventory") || lower.contains("cart")) {
            return "dashboard";
        }
        if (lower.contains("api") || lower.contains("httpbin") || lower.contains("rest")) {
            return "api";
        }
        return "login";
    }

    private GeneratedTestSuite fallbackLoginSuite() {
        try {
            String json = new String(getClass().getResourceAsStream("/generation/fallback-login.json")
                    .readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, GeneratedTestSuite.class);
        } catch (IOException e) {
            throw new IllegalStateException("Fallback login suite missing", e);
        }
    }

    private GeneratedTestSuite fallbackDashboardSuite() {
        try {
            String json = new String(getClass().getResourceAsStream("/generation/fallback-dashboard.json")
                    .readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, GeneratedTestSuite.class);
        } catch (IOException e) {
            throw new IllegalStateException("Fallback dashboard suite missing", e);
        }
    }

    private GeneratedTestSuite fallbackApiSuite() {
        try {
            String json = new String(getClass().getResourceAsStream("/generation/fallback-api.json")
                    .readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, GeneratedTestSuite.class);
        } catch (IOException e) {
            throw new IllegalStateException("Fallback API suite missing", e);
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
}
