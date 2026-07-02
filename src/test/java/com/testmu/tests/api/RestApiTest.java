package com.testmu.tests.api;

import com.testmu.api.ApiResponse;
import com.testmu.core.BaseAPITest;
import com.testmu.testdata.TestGroups;
import com.testmu.utils.ApiAssertionHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class RestApiTest extends BaseAPITest {

    // ── Pass scenario ──────────────────────────────────────────────────

    @Test(groups = {TestGroups.API, TestGroups.PASS},
            description = "GET /get should return 200 with request metadata")
    public void testGetRequestSuccess() {
        ApiResponse response = apiClient.get("/get");

        ApiAssertionHelper.assertStatusCode(response, 200);
        Assert.assertNotNull(response.body());
        Assert.assertTrue(response.body().get("url").asText().contains("httpbin.org/get"));
        Assert.assertEquals(response.body().get("headers").get("Accept").asText(), "application/json");
    }

    // ── Edge cases ─────────────────────────────────────────────────────

    @Test(groups = {TestGroups.API, TestGroups.EDGE},
            description = "GET /get with query params should echo args in response")
    public void testGetWithQueryParameters() {
        ApiResponse response = apiClient.get("/get", Map.of("page", "2", "limit", "10"));

        ApiAssertionHelper.assertStatusCode(response, 200);
        Assert.assertEquals(response.body().get("args").get("page").asText(), "2");
        Assert.assertEquals(response.body().get("args").get("limit").asText(), "10");
    }

    @Test(groups = {TestGroups.API, TestGroups.EDGE},
            description = "POST /post without body should still return 200 from httpbin")
    public void testPostWithoutBody() {
        ApiResponse response = apiClient.postWithoutBody("/post");

        ApiAssertionHelper.assertStatusCode(response, 200);
        Assert.assertNotNull(response.body());
        Assert.assertTrue(response.body().has("origin"), "Response should include origin metadata");
    }

    // ── Failed scenario ──────────────────────────────────────────────

    @Test(groups = {TestGroups.API, TestGroups.FAIL},
            description = "GET /status/404 should return 404 not found")
    public void testNotFoundStatus() {
        ApiResponse response = apiClient.get("/status/404");

        ApiAssertionHelper.assertStatusCode(response, 404);
    }
}
