package com.testmu.agent.generation;

import com.testmu.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class TestGenerationOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(TestGenerationOrchestrator.class);

    private final TestGenerationAgent agent = new TestGenerationAgent();
    private final GeneratedTestWriter writer = new GeneratedTestWriter();

    public List<Path> generateAllModules() throws IOException {
        return List.of(
                generateModule("login"),
                generateModule("dashboard"),
                generateModule("api")
        );
    }

    public Path generateModule(String module) throws IOException {
        if (!ConfigManager.get().aiTestGenerationEnabled()) {
            LOG.warn("Test generation disabled in config");
            return null;
        }

        String spec = loadModuleSpec(module);
        LOG.info("Generating tests for module: {}", module);
        var suite = agent.generate(spec);
        if (suite.getModule() == null || suite.getModule().isBlank()) {
            suite.setModule(module);
        }
        return writer.write(suite);
    }

    private String loadModuleSpec(String module) throws IOException {
        String resource = "/generation/module-" + module + ".md";
        try (var stream = getClass().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Module spec not found: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
