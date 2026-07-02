package com.testmu.agent.healing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageObjectPatcher {

    private static final Logger LOG = LoggerFactory.getLogger(PageObjectPatcher.class);
    private static final Path PAGES_DIR = Path.of("src/main/java/com/testmu/pages");

    public boolean patchLocator(String pageClass, String elementName, String strategy, String value) {
        Path pageFile = PAGES_DIR.resolve(pageClass + ".java");
        if (!Files.exists(pageFile)) {
            LOG.warn("Page object file not found for patching: {}", pageFile);
            return false;
        }

        try {
            String content = Files.readString(pageFile, StandardCharsets.UTF_8);
            String byCode = buildByCode(strategy, value);

            Pattern pattern = Pattern.compile(
                    "(private\\s+final\\s+By\\s+" + Pattern.quote(elementName) + "\\s*=\\s*)By\\.[^;]+(;)"
            );
            Matcher matcher = pattern.matcher(content);
            boolean found = matcher.find();

            if (!found) {
                String methodBlock = extractMethodBlock(content, elementName + "()");
                if (methodBlock != null) {
                    Matcher defaultMatcher = Pattern.compile("DEFAULT_\\w+").matcher(methodBlock);
                    if (defaultMatcher.find()) {
                        String constName = defaultMatcher.group();
                        pattern = Pattern.compile(
                                "(private\\s+static\\s+final\\s+By\\s+" + Pattern.quote(constName)
                                        + "\\s*=\\s*)By\\.[^;]+(;)"
                        );
                        matcher = pattern.matcher(content);
                        found = matcher.find();
                    }
                }
            }

            if (!found) {
                LOG.warn("Locator field {} not found in {}", elementName, pageFile);
                return false;
            }

            String updated = matcher.replaceFirst("$1" + Matcher.quoteReplacement(byCode) + "$2");
            if (updated.equals(content)) {
                return false;
            }

            Files.writeString(pageFile, updated, StandardCharsets.UTF_8);
            LOG.info("Patched {} in {} -> {}", elementName, pageFile, byCode);
            return true;
        } catch (IOException e) {
            LOG.warn("Failed to patch page object {}", pageFile, e);
            return false;
        }
    }

    private String extractMethodBlock(String content, String methodSignature) {
        int start = content.indexOf(methodSignature);
        if (start < 0) {
            return null;
        }
        int brace = content.indexOf('{', start);
        if (brace < 0) {
            return null;
        }
        int depth = 0;
        for (int i = brace; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            }
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String buildByCode(String strategy, String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return switch (strategy.toLowerCase()) {
            case "id" -> "By.id(\"" + escaped + "\")";
            case "name" -> "By.name(\"" + escaped + "\")";
            case "classname", "class" -> "By.className(\"" + escaped + "\")";
            case "tag", "tagname" -> "By.tagName(\"" + escaped + "\")";
            case "linktext", "link_text" -> "By.linkText(\"" + escaped + "\")";
            case "partiallinktext", "partial_link_text" -> "By.partialLinkText(\"" + escaped + "\")";
            case "xpath" -> "By.xpath(\"" + escaped + "\")";
            default -> "By.cssSelector(\"" + escaped + "\")";
        };
    }
}
