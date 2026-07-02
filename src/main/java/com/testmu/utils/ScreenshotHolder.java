package com.testmu.utils;

public final class ScreenshotHolder {

    private static final ThreadLocal<String> LAST_SCREENSHOT = new ThreadLocal<>();

    private ScreenshotHolder() {
    }

    public static void set(String path) {
        LAST_SCREENSHOT.set(path);
    }

    public static String get() {
        return LAST_SCREENSHOT.get();
    }

    public static void clear() {
        LAST_SCREENSHOT.remove();
    }
}
