package com.testmu.agent;

import com.testmu.config.ConfigManager;
import com.testmu.config.FrameworkConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class AiChatModelFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatModelFactory.class);

    private AiChatModelFactory() {
    }

    public static ChatLanguageModel create() {
        FrameworkConfig config = ConfigManager.get();
        String provider = config.aiProvider().toLowerCase();

        return switch (provider) {
            case "gemini" -> createGeminiModel(config);
            case "openai" -> createOpenAiModel(config);
            case "openrouter" -> createOpenRouterModel(config);
            default -> throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        };
    }

    public static String resolveApiKey() {
        FrameworkConfig config = ConfigManager.get();
        return switch (config.aiProvider().toLowerCase()) {
            case "gemini" -> resolveGeminiApiKey(config);
            case "openai" -> resolveOpenAiApiKey(config);
            case "openrouter" -> resolveOpenRouterApiKey(config);
            default -> null;
        };
    }

    private static ChatLanguageModel createGeminiModel(FrameworkConfig config) {
        String apiKey = resolveGeminiApiKey(config);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key not configured. Set GEMINI_API_KEY.");
        }

        LOG.info("Using Gemini model: {}", config.aiModel());
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(config.aiModel())
                .temperature(0.2)
                .build();
    }

    private static ChatLanguageModel createOpenAiModel(FrameworkConfig config) {
        String apiKey = resolveOpenAiApiKey(config);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key not configured. Set OPENAI_API_KEY.");
        }

        LOG.info("Using OpenAI model: {}", config.aiModel());
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(config.aiModel())
                .temperature(0.2)
                .build();
    }

    private static ChatLanguageModel createOpenRouterModel(FrameworkConfig config) {
        String apiKey = resolveOpenRouterApiKey(config);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenRouter API key not configured. Set OPENROUTER_API_KEY.");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("HTTP-Referer", config.openRouterSiteUrl());
        headers.put("X-OpenRouter-Title", config.openRouterAppName());

        LOG.info("Using OpenRouter model: {} via {}", config.aiModel(), config.openRouterBaseUrl());
        return OpenAiChatModel.builder()
                .baseUrl(config.openRouterBaseUrl())
                .apiKey(apiKey)
                .modelName(config.aiModel())
                .temperature(0.2)
                .customHeaders(headers)
                .build();
    }

    private static String resolveGeminiApiKey(FrameworkConfig config) {
        String fromConfig = firstUsableKey(
                config.geminiApiKey(),
                config.aiGeminiApiKey(),
                System.getenv("GEMINI_API_KEY"),
                System.getProperty("GEMINI_API_KEY")
        );
        if (fromConfig == null) {
            LOG.warn("Gemini API key missing. Set GEMINI_API_KEY in .env or environment.");
        }
        return fromConfig;
    }

    private static String resolveOpenAiApiKey(FrameworkConfig config) {
        return firstUsableKey(
                config.openAiApiKey(),
                System.getenv("OPENAI_API_KEY"),
                System.getProperty("OPENAI_API_KEY")
        );
    }

    private static String resolveOpenRouterApiKey(FrameworkConfig config) {
        String fromConfig = firstUsableKey(
                config.openRouterApiKey(),
                config.aiOpenRouterApiKey(),
                System.getenv("OPENROUTER_API_KEY"),
                System.getProperty("OPENROUTER_API_KEY")
        );
        if (fromConfig == null) {
            LOG.warn("OpenRouter API key missing. Set OPENROUTER_API_KEY in .env (key starts with sk-or-v1-).");
        }
        return fromConfig;
    }

    private static String firstUsableKey(String... candidates) {
        for (String candidate : candidates) {
            if (isUsableKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isUsableKey(String key) {
        return key != null && !key.isBlank()
                && !key.startsWith("${")
                && !key.contains("your-key")
                && !key.contains("your-gemini")
                && !key.contains("your-openrouter");
    }
}
