package com.testmu.config;

import org.aeonbits.owner.ConfigFactory;

public final class ConfigManager {

    private static final FrameworkConfig CONFIG;

    static {
        EnvLoader.load();
        CONFIG = ConfigFactory.create(FrameworkConfig.class);
    }

    private ConfigManager() {
    }

    public static FrameworkConfig get() {
        return CONFIG;
    }
}
