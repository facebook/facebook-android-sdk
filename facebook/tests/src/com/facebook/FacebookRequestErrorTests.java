/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FacebookRequestErrorTests extends FacebookTestCase {
    public static final String ERROR_SINGLE_RESPONSE =
            "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"Unknown path components: /unknown\",\n" +
            "    \"type\": \"OAuthException\",\n" +
            "    \"code\": 2500\n" +
            "  }\n" +
            "}";

    public static final String ERROR_BATCH_RESPONSE =
            "[\n" +
            "  {\n" +
            "    \"headers\": [\n" +
            "      {\n" +
            "        \"value\": \"*\",\n" +
            "        \"name\": \"Access-Control-Allow-Origin\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"no-store\",\n" +
            "        \"name\": \"Cache-Control\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"close\",\n" +
            "        \"name\": \"Connection\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"text\\/javascript; charset=UTF-8\",\n" +
            "        \"name\": \"Content-Type\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"Sat, 01 Jan 2000 00:00:00 GMT\",\n" +
            "        \"name\": \"Expires\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"no-cache\",\n" +
            "        \"name\": \"Pragma\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"OAuth \\\"Facebook Platform\\\" \\\"invalid_request\\\" \\\"An active access token must be used to query information about the current user.\\\"\",\n" +
            "        \"name\": \"WWW-Authenticate\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"body\": \"{\\\"error\\\":{\\\"message\\\":\\\"An active access token must be used to query information about the current user.\\\",\\\"type\\\":\\\"OAuthException\\\",\\\"code\\\":2500}}\",\n" +
            "    \"code\": 400\n" +
            "  },\n" +
            "  {\n" +
            "    \"headers\": [\n" +
            "      {\n" +
            "        \"value\": \"*\",\n" +
            "        \"name\": \"Access-Control-Allow-Origin\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"no-store\",\n" +
            "        \"name\": \"Cache-Control\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"close\",\n" +
            "        \"name\": \"Connection\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"text\\/javascript; charset=UTF-8\",\n" +
            "        \"name\": \"Content-Type\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"Sat, 01 Jan 2000 00:00:00 GMT\",\n" +
            "        \"name\": \"Expires\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"no-cache\",\n" +
            "        \"name\": \"Pragma\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"value\": \"OAuth \\\"Facebook Platform\\\" \\\"invalid_request\\\" \\\"An active access token must be used to query information about the current user.\\\"\",\n" +
            "        \"name\": \"WWW-Authenticate\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"body\": \"{\\\"error\\\":{\\\"message\\\":\\\"An active access token must be used to query information about the current user.\\\",\\\"type\\\":\\\"OAuthException\\\",\\\"code\\\":2500}}\",\n" +
            "    \"code\": 400\n" +
            "  }\n" +
            "]";

    public void testSingleRequestWithoutBody() throws JSONException {
        JSONObject withStatusCode = new JSONObject();
        withStatusCode.put("code", 400);
        FacebookRequestError error =
                FacebookRequestError.checkResponseAndCreateError(withStatusCode, withStatusCode, null);
        assertNotNull(error);
        assertEquals(400, error.getRequestStatusCode());
    }

    public void testSingleRequestWithBody() throws JSONException {
        JSONObject originalResponse = new JSONObject(ERROR_SINGLE_RESPONSE);
        JSONObject withStatusCodeAndBody = new JSONObject();
        withStatusCodeAndBody.put("code", 400);
        withStatusCodeAndBody.put("body", originalResponse);
        FacebookRequestError error =
                FacebookRequestError.checkResponseAndCreateError(withStatusCodeAndBody, originalResponse, null);
        assertNotNull(error);
        assertEquals(400, error.getRequestStatusCode());
        assertEquals("Unknown path components: /unknown", error.getErrorMessage());
        assertEquals("OAuthException", error.getErrorType());
        assertEquals(2500, error.getErrorCode());
        assertTrue(error.getBatchRequestResult() instanceof JSONObject);
    }

    public void testBatchRequest() throws JSONException {
        JSONArray batchResponse = new JSONArray(ERROR_BATCH_RESPONSE);
        assertEquals(2, batchResponse.length());
        JSONObject firstResponse = (JSONObject) batchResponse.get(0);
        FacebookRequestError error =
                FacebookRequestError.checkResponseAndCreateError(firstResponse, batchResponse, null);
        assertNotNull(error);
        assertEquals(400, error.getRequestStatusCode());
        assertEquals("An active access token must be used to query information about the current user.",
                error.getErrorMessage());
        assertEquals("OAuthException", error.getErrorType());
        assertEquals(2500, error.getErrorCode());
        assertTrue(error.getBatchRequestResult() instanceof  JSONArray);
    }
}
