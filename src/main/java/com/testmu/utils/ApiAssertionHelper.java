package com.testmu.utils;

import com.testmu.agent.healing.ApiHealingStore;
import com.testmu.api.ApiResponse;
import com.testmu.core.TestContextHolder;
import org.testng.Assert;

public final class ApiAssertionHelper {

    private ApiAssertionHelper() {
    }

    public static void assertStatusCode(ApiResponse response, int defaultExpected) {
        String testKey = TestContextHolder.getTestKey();
        int expected = defaultExpected;

        if (testKey != null) {
            expected = ApiHealingStore.getInstance()
                    .get(testKey)
                    .map(ApiHealingStore.ApiHealEntry::expectedStatusCode)
                    .orElse(defaultExpected);
        }

        Assert.assertEquals(response.statusCode(), expected,
                "Status code mismatch" + (expected != defaultExpected ? " (healed expectation)" : ""));
    }
}
