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

package com.facebook.appevents.cloudbridge

import java.net.HttpURLConnection
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppEventsConversionsAPITransformerWebRequestsTest {

  private lateinit var mockWebServer: MockWebServer
  enum class Keys(val rawValue: String) {
    EVENT_NAME("event_name")
  }

  enum class Values(val rawValue: String) {
    DATASET_ID("id123"),
    ACCESS_KEY("key123"),
    CLOUDBRIDGE_URL("https://www.123.com"),
    TEST_EVENT_1("test1"),
    TEST_EVENT_2("test2")
  }

  @Before
  fun beforeEach() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    AppEventsConversionsAPITransformerWebRequests.transformedEvents = mutableListOf()

    AppEventsConversionsAPITransformerWebRequests.currentRetryCount = 0
  }

  @After
  fun afterEach() {
    mockWebServer.shutdown()
  }

  @Test
  fun testConfigure() {

    AppEventsConversionsAPITransformerWebRequests.configure(
        Values.DATASET_ID.rawValue,
        Values.CLOUDBRIDGE_URL.rawValue,
        Values.ACCESS_KEY.rawValue,
    )

    assertThat(AppEventsConversionsAPITransformerWebRequests.credentials.datasetID)
        .isEqualTo(Values.DATASET_ID.rawValue)
        .withFailMessage("Credential's dataset ID is not expected")
    assertThat(AppEventsConversionsAPITransformerWebRequests.credentials.accessKey)
        .isEqualTo(Values.ACCESS_KEY.rawValue)
        .withFailMessage("Credential's access key is not expected")
    assertThat(AppEventsConversionsAPITransformerWebRequests.credentials.cloudBridgeURL)
        .isEqualTo(Values.CLOUDBRIDGE_URL.rawValue)
        .withFailMessage("Credential's cloudbridge url is not expected")
  }

  @Test
  fun testErrorHandlingWithServerError() {

    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
            .addHeader("Content-Type", "application/json"))

    AppEventsConversionsAPITransformerWebRequests.handleError(
        HttpURLConnection.HTTP_INTERNAL_ERROR,
        mutableListOf(mutableMapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_1.rawValue)))
    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.isEmpty())
        .isEqualTo(true)
        .withFailMessage(
            "Should not re-append the events to the cache queue if the request fails for server error")
  }

  @Test
  fun testErrorHandlingWithoutServerError1() {
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)
            .addHeader("Content-Type", "application/json"))

    AppEventsConversionsAPITransformerWebRequests.transformedEvents =
        mutableListOf(mutableMapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_1.rawValue))
    AppEventsConversionsAPITransformerWebRequests.handleError(
        HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
        mutableListOf(mutableMapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_2.rawValue)))

    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.count())
        .isEqualTo(2)
        .withFailMessage(
            "Should re-append the events to the cache queue if the request fails for connectivity issue")

    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.first())
        .isEqualTo(mapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_2.rawValue))
        .withFailMessage("The appended event is not expected")
  }

  @Test
  fun testErrorHandlingWithoutServerError2() {
    mockWebServer.enqueue(
        MockResponse().setResponseCode(429).addHeader("Content-Type", "application/json"))

    AppEventsConversionsAPITransformerWebRequests.transformedEvents =
        mutableListOf(mutableMapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_1.rawValue))
    AppEventsConversionsAPITransformerWebRequests.handleError(
        HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
        mutableListOf(mutableMapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_2.rawValue)))

    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.count())
        .isEqualTo(2)
        .withFailMessage(
            "Should re-append the events to the cache queue if the request fails for server overloaded issue")

    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.first())
        .isEqualTo(mapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_2.rawValue))
        .withFailMessage("The appended event is not expected")
  }

  @Test
  fun testErrorHandlingWithoutServerErrorRetryLimit() {

    for (i in 1..AppEventsConversionsAPITransformerWebRequests.MAX_RETRY_COUNT + 1) {
      mockWebServer.enqueue(
          MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)
              .addHeader("Content-Type", "application/json"))

      AppEventsConversionsAPITransformerWebRequests.handleError(
          HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
          mutableListOf(mutableMapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_1.rawValue)),
          AppEventsConversionsAPITransformerWebRequests.MAX_RETRY_COUNT)
    }

    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.count())
        .isEqualTo(0)
        .withFailMessage(
            "After Max retry count, the cache queue should be empty, if the request fails for connectivity issue")
  }

  @Test
  fun testAppendEventsOverLimits() {
    assertThat(AppEventsConversionsAPITransformerWebRequests.MAX_CACHED_TRANSFORMED_EVENTS)
        .isGreaterThan(100)
        .withFailMessage("This test fails if max cached events less than or equals to 100")

    val event = mapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_1.rawValue)
    val events: MutableList<Map<String, Any>> = mutableListOf()
    for (i in 1..100) {
      events.add(event)
    }

    for (i in
        1..(AppEventsConversionsAPITransformerWebRequests.MAX_CACHED_TRANSFORMED_EVENTS - 10)) {
      AppEventsConversionsAPITransformerWebRequests.transformedEvents.add(event)
    }
    AppEventsConversionsAPITransformerWebRequests.appendEvents(events)

    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.count())
        .isEqualTo(AppEventsConversionsAPITransformerWebRequests.MAX_CACHED_TRANSFORMED_EVENTS)
        .withFailMessage(
            "Cached event queue should have events no more than $0",
            AppEventsConversionsAPITransformerWebRequests.MAX_CACHED_TRANSFORMED_EVENTS)
  }

  @Test
  fun testAppendEventsWithinLimits() {

    assertThat(AppEventsConversionsAPITransformerWebRequests.MAX_CACHED_TRANSFORMED_EVENTS)
        .isGreaterThan(110)
        .withFailMessage("This test fails if max cached events less than or equals to 110")

    val event = mapOf(Keys.EVENT_NAME.rawValue to Values.TEST_EVENT_1.rawValue)
    val events: MutableList<Map<String, Any>> = mutableListOf()
    for (i in 1..100) {
      events.add(event)
    }

    for (i in 1..10) {
      AppEventsConversionsAPITransformerWebRequests.transformedEvents.add(event)
    }
    AppEventsConversionsAPITransformerWebRequests.appendEvents(events)

    assertThat(AppEventsConversionsAPITransformerWebRequests.transformedEvents.count())
        .isEqualTo(110)
        .withFailMessage("Cached event queue should have events no more than 110")
  }

  @Test
  fun testPostRequest() {
    mockWebServer.enqueue(
        MockResponse()
            .setBody("[{\"receive_body\": 1}]")
            .addHeader("Content-Type", "application/json"))

    val httpUrl = mockWebServer.url("/").toString()
    AppEventsConversionsAPITransformerWebRequests.makeHttpRequest(
        httpUrl,
        "POST",
        "[{\"send_body\": 1}]",
        mapOf("Content-Type" to "application/json"),
        60,
    ) { outcome: String?, httpResponse: Int? ->
      // assert the result
      assertThat(outcome).isEqualTo("[{\"receive_body\": 1}]")
      assertThat(httpResponse).isEqualTo(200)
    }

    val recordedRequestCount = mockWebServer.requestCount
    assertThat(recordedRequestCount).isEqualTo(1)
    val recordedRequest = mockWebServer.takeRequest()
    assertThat(recordedRequest.body.toString()).isEqualTo("[text=[{\"send_body\": 1}]]")
    assertThat(recordedRequest.requestLine).isEqualTo("POST / HTTP/1.1")
  }
}
