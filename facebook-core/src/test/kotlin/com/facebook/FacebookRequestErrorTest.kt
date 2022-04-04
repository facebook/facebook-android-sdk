/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class FacebookRequestErrorTest : FacebookPowerMockTestCase() {
  @Before
  fun before() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
  }

  @Test
  fun testClientException() {
    val errorMsg = "some error happened"
    val error = FacebookRequestError(null, FacebookException(errorMsg))
    Assert.assertEquals(errorMsg, error.errorMessage)
    Assert.assertEquals(FacebookRequestError.Category.OTHER, error.category)
    Assert.assertEquals(FacebookRequestError.INVALID_ERROR_CODE, error.errorCode)
    Assert.assertEquals(FacebookRequestError.INVALID_HTTP_STATUS_CODE, error.requestStatusCode)
  }

  @Test
  fun testSingleRequestWithoutBody() {
    val withStatusCode = JSONObject()
    withStatusCode.put("code", 400)
    val error =
        FacebookRequestError.checkResponseAndCreateError(withStatusCode, withStatusCode, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(400, error?.requestStatusCode)
    Assert.assertEquals(FacebookRequestError.Category.OTHER, error?.category)
  }

  @Test
  fun testSingleErrorWithBody() {
    val originalResponse = JSONObject(ERROR_SINGLE_RESPONSE)
    val withStatusCodeAndBody = JSONObject()
    withStatusCodeAndBody.put("code", 400)
    withStatusCodeAndBody.put("body", originalResponse)
    val error =
        FacebookRequestError.checkResponseAndCreateError(
            withStatusCodeAndBody, originalResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(400, error?.requestStatusCode)
    Assert.assertEquals("Unknown path components: /unknown", error?.errorMessage)
    Assert.assertEquals("OAuthException", error?.errorType)
    Assert.assertEquals(2_500, error?.errorCode)
    assertThat(error?.batchRequestResult is JSONObject).isTrue
    Assert.assertEquals(FacebookRequestError.Category.OTHER, error?.category)
  }

  @Test
  fun testBatchRequest() {
    val batchResponse = JSONArray(ERROR_BATCH_RESPONSE)
    Assert.assertEquals(2, batchResponse.length())
    val firstResponse = batchResponse[0] as JSONObject
    val error = FacebookRequestError.checkResponseAndCreateError(firstResponse, batchResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(400, error?.requestStatusCode)
    Assert.assertEquals(
        "An active access token must be used to query information about the current user.",
        error?.errorMessage)
    Assert.assertEquals("OAuthException", error?.errorType)
    Assert.assertEquals(2_500, error?.errorCode)
    assertThat(error?.batchRequestResult is JSONArray).isTrue
    Assert.assertEquals(FacebookRequestError.Category.OTHER, error?.category)
  }

  @Test
  fun testSingleThrottledError() {
    val originalResponse = JSONObject(ERROR_SINGLE_RESPONSE_THROTTLE)
    val withStatusCodeAndBody = JSONObject()
    withStatusCodeAndBody.put("code", 403)
    withStatusCodeAndBody.put("body", originalResponse)
    val error =
        FacebookRequestError.checkResponseAndCreateError(
            withStatusCodeAndBody, originalResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(403, error?.requestStatusCode)
    Assert.assertEquals("Application request limit reached", error?.errorMessage)
    Assert.assertNull(error?.errorType)
    Assert.assertEquals(4, error?.errorCode)
    assertThat(error?.batchRequestResult is JSONObject).isTrue
    Assert.assertEquals(FacebookRequestError.Category.TRANSIENT, error?.category)
  }

  @Test
  fun testSingleServerError() {
    val originalResponse = JSONObject(ERROR_SINGLE_RESPONSE_SERVER)
    val withStatusCodeAndBody = JSONObject()
    withStatusCodeAndBody.put("code", 500)
    withStatusCodeAndBody.put("body", originalResponse)
    val error =
        FacebookRequestError.checkResponseAndCreateError(
            withStatusCodeAndBody, originalResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(500, error?.requestStatusCode)
    Assert.assertEquals("Some Server Error", error?.errorMessage)
    Assert.assertNull(error?.errorType)
    Assert.assertEquals(2, error?.errorCode)
    assertThat(error?.batchRequestResult is JSONObject).isTrue
    Assert.assertEquals(FacebookRequestError.Category.TRANSIENT, error?.category)
  }

  @Test
  fun testSinglePermissionError() {
    val originalResponse = JSONObject(ERROR_SINGLE_RESPONSE_PERMISSION)
    val withStatusCodeAndBody = JSONObject()
    withStatusCodeAndBody.put("code", 400)
    withStatusCodeAndBody.put("body", originalResponse)
    val error =
        FacebookRequestError.checkResponseAndCreateError(
            withStatusCodeAndBody, originalResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(400, error?.requestStatusCode)
    Assert.assertEquals("(#200) Requires extended permission: publish_actions", error?.errorMessage)
    Assert.assertEquals("OAuthException", error?.errorType)
    Assert.assertEquals(200, error?.errorCode)
    Assert.assertEquals(FacebookRequestError.INVALID_ERROR_CODE, error?.subErrorCode)
    assertThat(error?.batchRequestResult is JSONObject).isTrue
    Assert.assertEquals(FacebookRequestError.Category.OTHER, error?.category)
  }

  @Test
  fun testSingleWebLoginError() {
    val originalResponse = JSONObject(ERROR_SINGLE_RESPONSE_WEB_LOGIN)
    val withStatusCodeAndBody = JSONObject()
    withStatusCodeAndBody.put("code", 400)
    withStatusCodeAndBody.put("body", originalResponse)
    val error =
        FacebookRequestError.checkResponseAndCreateError(
            withStatusCodeAndBody, originalResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(400, error?.requestStatusCode)
    Assert.assertEquals("User need to login", error?.errorMessage)
    Assert.assertEquals("OAuthException", error?.errorType)
    Assert.assertEquals(102, error?.errorCode)
    Assert.assertEquals(459, error?.subErrorCode)
    assertThat(error?.batchRequestResult is JSONObject).isTrue
    Assert.assertEquals(FacebookRequestError.Category.LOGIN_RECOVERABLE, error?.category)
  }

  @Test
  fun testSingleReloginError() {
    val originalResponse = JSONObject(ERROR_SINGLE_RESPONSE_RELOGIN)
    val withStatusCodeAndBody = JSONObject()
    withStatusCodeAndBody.put("code", 400)
    withStatusCodeAndBody.put("body", originalResponse)
    val error =
        FacebookRequestError.checkResponseAndCreateError(
            withStatusCodeAndBody, originalResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(400, error?.requestStatusCode)
    Assert.assertEquals("User need to relogin", error?.errorMessage)
    Assert.assertEquals("OAuthException", error?.errorType)
    Assert.assertEquals(102, error?.errorCode)
    Assert.assertEquals(FacebookRequestError.INVALID_ERROR_CODE, error?.subErrorCode)
    assertThat(error?.batchRequestResult is JSONObject).isTrue
    Assert.assertEquals(FacebookRequestError.Category.LOGIN_RECOVERABLE, error?.category)
  }

  @Test
  fun testSingleReloginDeletedAppError() {
    val originalResponse = JSONObject(ERROR_SINGLE_RESPONSE_RELOGIN_DELETED_APP)
    val withStatusCodeAndBody = JSONObject()
    withStatusCodeAndBody.put("code", 400)
    withStatusCodeAndBody.put("body", originalResponse)
    val error =
        FacebookRequestError.checkResponseAndCreateError(
            withStatusCodeAndBody, originalResponse, null)
    Assert.assertNotNull(error)
    Assert.assertEquals(400, error?.requestStatusCode)
    Assert.assertEquals("User need to relogin", error?.errorMessage)
    Assert.assertEquals("OAuthException", error?.errorType)
    Assert.assertEquals(190, error?.errorCode)
    Assert.assertEquals(458, error?.subErrorCode)
    assertThat(error?.batchRequestResult is JSONObject).isTrue
    Assert.assertEquals(FacebookRequestError.Category.LOGIN_RECOVERABLE, error?.category)
  }

  companion object {
    const val ERROR_SINGLE_RESPONSE =
        "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"Unknown path components: /unknown\",\n" +
            "    \"type\": \"OAuthException\",\n" +
            "    \"code\": 2500\n" +
            "  }\n" +
            "}"
    const val ERROR_BATCH_RESPONSE =
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
            "]"
    const val ERROR_SINGLE_RESPONSE_THROTTLE =
        "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"Application request limit reached\",\n" +
            "    \"code\": 4\n" +
            "  }\n" +
            "}"
    const val ERROR_SINGLE_RESPONSE_SERVER =
        "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"Some Server Error\",\n" +
            "    \"code\": 2\n" +
            "  }\n" +
            "}"
    const val ERROR_SINGLE_RESPONSE_PERMISSION =
        "{\n" +
            "  \"error\": {\n" +
            "    \"type\": \"OAuthException\",\n" +
            "    \"message\": \"(#200) Requires extended permission: publish_actions\",\n" +
            "    \"code\": 200\n" +
            "  }\n" +
            "}"
    const val ERROR_SINGLE_RESPONSE_WEB_LOGIN =
        "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"User need to login\",\n" +
            "    \"type\": \"OAuthException\",\n" +
            "    \"code\": 102,\n" +
            "    \"error_subcode\": 459\n" +
            "  }\n" +
            "}"
    const val ERROR_SINGLE_RESPONSE_RELOGIN =
        "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"User need to relogin\",\n" +
            "    \"type\": \"OAuthException\",\n" +
            "    \"code\": 102\n" +
            "  }\n" +
            "}"
    const val ERROR_SINGLE_RESPONSE_RELOGIN_DELETED_APP =
        "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"User need to relogin\",\n" +
            "    \"type\": \"OAuthException\",\n" +
            "    \"code\": 190,\n" +
            "    \"error_subcode\": 458\n" +
            "  }\n" +
            "}"
  }
}
