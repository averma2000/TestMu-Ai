package com.testmu.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

@Sources({
        "classpath:config.properties",
        "system:properties",
        "system:env"
})
public interface FrameworkConfig extends Config {

    @Key("browser")
    @DefaultValue("chrome")
    String browser();

    @Key("headless")
    @DefaultValue("false")
    boolean headless();

    @Key("implicit.wait.seconds")
    @DefaultValue("10")
    int implicitWaitSeconds();

    @Key("page.load.timeout.seconds")
    @DefaultValue("30")
    int pageLoadTimeoutSeconds();

    @Key("base.url")
    String baseUrl();

    @Key("api.base.url")
    @DefaultValue("https://httpbin.org")
    String apiBaseUrl();

    @Key("valid.username")
    @DefaultValue("standard_user")
    String validUsername();

    @Key("valid.password")
    @DefaultValue("secret_sauce")
    String validPassword();

    @Key("ai.agent.enabled")
    @DefaultValue("true")
    boolean aiAgentEnabled();

    @Key("ai.provider")
    @DefaultValue("gemini")
    String aiProvider();

    @Key("ai.model")
    @DefaultValue("gemini-2.0-flash")
    String aiModel();

    @Key("GEMINI_API_KEY")
    @DefaultValue("")
    String geminiApiKey();

    @Key("ai.gemini.api.key")
    @DefaultValue("")
    String aiGeminiApiKey();

    @Key("ai.agent.memory.path")
    @DefaultValue("agent-memory")
    String aiAgentMemoryPath();

    @Key("ai.agent.insights.dir")
    @DefaultValue("reports/failure-insights")
    String aiAgentInsightsDir();

    @Key("ai.html.max.chars")
    @DefaultValue("20000")
    int aiHtmlMaxChars();

    @Key("ai.openai.api.key")
    @DefaultValue("")
    String openAiApiKey();

    @Key("OPENROUTER_API_KEY")
    @DefaultValue("")
    String openRouterApiKey();

    @Key("ai.openrouter.api.key")
    @DefaultValue("")
    String aiOpenRouterApiKey();

    @Key("ai.openrouter.base.url")
    @DefaultValue("https://openrouter.ai/api/v1")
    String openRouterBaseUrl();

    @Key("ai.openrouter.site.url")
    @DefaultValue("http://localhost")
    String openRouterSiteUrl();

    @Key("ai.openrouter.app.name")
    @DefaultValue("TestMu AI Framework")
    String openRouterAppName();

    @Key("ai.self.healing.enabled")
    @DefaultValue("true")
    boolean aiSelfHealingEnabled();

    @Key("ai.heal.max.retries")
    @DefaultValue("1")
    int aiHealMaxRetries();

    @Key("ai.heal.min.confidence")
    @DefaultValue("0.7")
    double aiHealMinConfidence();

    @Key("ai.heal.patch.source")
    @DefaultValue("false")
    boolean aiHealPatchSource();

    @Key("screenshot.on.failure")
    @DefaultValue("true")
    boolean screenshotOnFailure();

    @Key("screenshot.on.pass")
    @DefaultValue("true")
    boolean screenshotOnPass();

    @Key("extent.report.enabled")
    @DefaultValue("true")
    boolean extentReportEnabled();

    @Key("extent.report.dir")
    @DefaultValue("reports/extent")
    String extentReportDir();

    @Key("extent.report.name")
    @DefaultValue("TestMu_Automation_Report")
    String extentReportName();

    @Key("reports.dir")
    @DefaultValue("reports")
    String reportsDir();

    @Key("ai.test.generation.enabled")
    @DefaultValue("true")
    boolean aiTestGenerationEnabled();

    @Key("ai.test.generation.output.dir")
    @DefaultValue("src/test/java/com/testmu/tests/generated")
    String aiTestGenerationOutputDir();

    @Key("ai.flaky.classifier.enabled")
    @DefaultValue("true")
    boolean aiFlakyClassifierEnabled();

    @Key("ai.flaky.threshold")
    @DefaultValue("0.3")
    double aiFlakyThreshold();
}
