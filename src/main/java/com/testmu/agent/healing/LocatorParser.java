package com.testmu.agent.healing;

import org.openqa.selenium.By;

public final class LocatorParser {

    private LocatorParser() {
    }

    public static By toBy(String strategy, String value) {
        if (strategy == null || value == null) {
            throw new IllegalArgumentException("Locator strategy and value are required");
        }
        return switch (strategy.toLowerCase()) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "classname", "class" -> By.className(value);
            case "tag", "tagname" -> By.tagName(value);
            case "linktext", "link_text" -> By.linkText(value);
            case "partiallinktext", "partial_link_text" -> By.partialLinkText(value);
            case "xpath" -> By.xpath(value);
            case "css", "cssselector", "css_selector" -> By.cssSelector(value);
            default -> throw new IllegalArgumentException("Unsupported locator strategy: " + strategy);
        };
    }

    public static String strategyFromBy(By by) {
        String repr = by.toString();
        if (repr.startsWith("By.id:")) return "id";
        if (repr.startsWith("By.name:")) return "name";
        if (repr.startsWith("By.className:")) return "className";
        if (repr.startsWith("By.tagName:")) return "tagName";
        if (repr.startsWith("By.linkText:")) return "linkText";
        if (repr.startsWith("By.partialLinkText:")) return "partialLinkText";
        if (repr.startsWith("By.xpath:")) return "xpath";
        if (repr.startsWith("By.cssSelector:")) return "css";
        return "css";
    }

    public static String valueFromBy(By by) {
        String repr = by.toString();
        int colon = repr.indexOf(':');
        return colon >= 0 ? repr.substring(colon + 1).trim() : repr;
    }
}
