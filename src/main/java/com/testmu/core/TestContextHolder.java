package com.testmu.core;

public final class TestContextHolder {

    private static final ThreadLocal<String> TEST_KEY = new ThreadLocal<>();

    private TestContextHolder() {
    }

    public static void setTestKey(String testKey) {
        TEST_KEY.set(testKey);
    }

    public static String getTestKey() {
        return TEST_KEY.get();
    }

    public static void clear() {
        TEST_KEY.remove();
    }
}
