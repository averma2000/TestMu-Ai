package com.testmu.api;

public final class ApiContextHolder {

    private static final ThreadLocal<String> LAST_REQUEST_URL = new ThreadLocal<>();
    private static final ThreadLocal<Integer> LAST_STATUS_CODE = new ThreadLocal<>();
    private static final ThreadLocal<String> LAST_RESPONSE_BODY = new ThreadLocal<>();

    private ApiContextHolder() {
    }

    public static void record(String requestUrl, int statusCode, String responseBody) {
        LAST_REQUEST_URL.set(requestUrl);
        LAST_STATUS_CODE.set(statusCode);
        LAST_RESPONSE_BODY.set(truncate(responseBody));
    }

    public static void clear() {
        LAST_REQUEST_URL.remove();
        LAST_STATUS_CODE.remove();
        LAST_RESPONSE_BODY.remove();
    }

    public static String getLastRequestUrl() {
        return LAST_REQUEST_URL.get();
    }

    public static Integer getLastStatusCode() {
        return LAST_STATUS_CODE.get();
    }

    public static String getLastResponseBody() {
        return LAST_RESPONSE_BODY.get();
    }

    private static String truncate(String body) {
        if (body == null) {
            return null;
        }
        return body.length() > 5000 ? body.substring(0, 5000) + "...[truncated]" : body;
    }
}
