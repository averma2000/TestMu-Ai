package com.testmu.agent.healing.model;

import java.time.LocalDateTime;

public record HealedLocatorEntry(
        String pageClass,
        String elementName,
        String locatorStrategy,
        String locatorValue,
        String originalLocator,
        LocalDateTime healedAt,
        double confidence,
        String sourceTest
) {
}
