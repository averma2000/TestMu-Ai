package com.testmu.core;

import com.testmu.agent.flaky.FlakyReportGenerator;
import com.testmu.agent.flaky.FlakyTestClassifier;
import com.testmu.agent.flaky.model.TestStabilityProfile;
import com.testmu.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.util.List;

public class SuiteListener implements ISuiteListener {

    private static final Logger LOG = LoggerFactory.getLogger(SuiteListener.class);

    @Override
    public void onFinish(ISuite suite) {
        if (!ConfigManager.get().aiFlakyClassifierEnabled()) {
            return;
        }

        try {
            FlakyTestClassifier classifier = new FlakyTestClassifier();
            List<TestStabilityProfile> profiles = classifier.classifyAll();
            if (!profiles.isEmpty()) {
                new FlakyReportGenerator().generate(profiles);
                long flaky = profiles.stream().filter(p -> "flaky".equals(p.getClassification())).count();
                LOG.info("Flaky classifier: {} test(s) analyzed, {} flaky", profiles.size(), flaky);
            }
        } catch (Exception e) {
            LOG.warn("Failed to generate flaky report", e);
        }
    }
}
