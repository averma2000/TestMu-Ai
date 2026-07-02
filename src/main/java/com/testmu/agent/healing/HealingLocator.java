package com.testmu.agent.healing;

import org.openqa.selenium.By;

public final class HealingLocator {

    private static final LocatorHealingStore STORE = LocatorHealingStore.getInstance();

    private HealingLocator() {
    }

    public static By resolve(String pageClass, String elementName, By defaultBy) {
        return STORE.resolve(pageClass, elementName, defaultBy);
    }
}
