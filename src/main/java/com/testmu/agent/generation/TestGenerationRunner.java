package com.testmu.agent.generation;

import com.testmu.config.EnvLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run test generation: mvn exec:java -Dexec.mainClass=com.testmu.agent.generation.TestGenerationRunner
 */
public class TestGenerationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TestGenerationRunner.class);

    public static void main(String[] args) {
        EnvLoader.load();
        TestGenerationOrchestrator orchestrator = new TestGenerationOrchestrator();

        try {
            if (args.length > 0) {
                var path = orchestrator.generateModule(args[0]);
                LOG.info("Generated: {}", path);
            } else {
                var paths = orchestrator.generateAllModules();
                paths.forEach(p -> LOG.info("Generated: {}", p));
            }
            LOG.info("Test generation complete. Re-run mvn test to execute generated tests.");
        } catch (Exception e) {
            LOG.error("Test generation failed", e);
            System.exit(1);
        }
    }
}
