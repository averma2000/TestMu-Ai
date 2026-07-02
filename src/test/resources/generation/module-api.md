# REST API Module Specification

Target: httpbin.org public API

## Scenarios to generate
1. **Pass**: GET /get returns 200 with url metadata
2. **Edge**: GET /get with query params echoes args in response
3. **Fail**: GET /status/404 returns 404

## API client available
- `apiClient.get(path)` — returns ApiResponse
- `apiClient.get(path, Map.of("key", "value"))` — with query params
- `apiClient.postWithoutBody(path)` — POST without body
- `ApiAssertionHelper.assertStatusCode(response, expectedCode)`

## Assertions
- response.body() returns JsonNode
- response.statusCode() returns int
