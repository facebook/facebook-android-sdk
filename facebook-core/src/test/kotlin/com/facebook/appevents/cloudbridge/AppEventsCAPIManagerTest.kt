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

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphRequestBatch
import com.facebook.GraphResponse
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@PowerMockIgnore("org.powermock.*", "org.mockito.*", "org.robolectric.*", "kotlin.*", "kotlinx.*")
@RunWith(RobolectricTestRunner::class)
@PrepareForTest(
    FacebookSdk::class,
    GraphRequest::class,
    GraphRequestBatch::class,
    LocalBroadcastManager::class,
)
@Config(manifest = Config.NONE)
class AppEventsCAPIManagerTest : FacebookPowerMockTestCase() {

  enum class Values(val rawValue: String) {
    DATASETID("id123"),
    ACCESSKEY("key123"),
    CLOUDBRIDGEURL("https://www.123.com")
  }

  private val mockAppID = "1234"
  private val mockClientToken = "5678"

  private val correctJSONSettings =
      mapOf<String, Any>(
          "data" to
              listOf(
                  mapOf<String, Any>(
                      "is_enabled" to true,
                      "access_key" to "\"" + Values.ACCESSKEY.rawValue + "\"",
                      "dataset_id" to "\"" + Values.DATASETID.rawValue + "\"",
                      "endpoint" to "\"" + Values.CLOUDBRIDGEURL.rawValue + "\"")))

  private val incorrectJSONSettings =
      mapOf<String, Any>(
          "data" to
              listOf(
                  mapOf<String, Any>(
                      "is_enabled" to true,
                  )))

  @Before
  override fun setup() {
    super.setup()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)

    whenever(FacebookSdk.getClientToken()).thenReturn(mockClientToken)
    whenever(FacebookSdk.isDebugEnabled()).thenReturn(false)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getGraphDomain()).thenCallRealMethod()
    whenever(FacebookSdk.getFacebookDomain()).thenCallRealMethod()
    whenever(FacebookSdk.getGraphApiVersion()).thenCallRealMethod()

    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    val mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(any())).thenReturn(mockLocalBroadcastManager)

    AppEventsCAPIManager.isEnabled = false
  }

  @After
  fun afterEach() {
    AppEventsCAPIManager.isEnabled = false
  }

  @Test
  fun testEnableWithNetworkError() {
    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)

    AppEventsCAPIManager.getCAPIGSettingsFromGraphResponse(
        mockGraphResponses(400, correctJSONSettings.toString()))

    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)
  }

  @Test
  fun testEnableWithoutNetworkErrorWrongJSON() {
    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)

    AppEventsCAPIManager.getCAPIGSettingsFromGraphResponse(
        mockGraphResponses(200, incorrectJSONSettings.toString()))

    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)
  }

  @Test
  fun testEnableWithoutNetworkErrorRightJSON() {
    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)

    AppEventsCAPIManager.getCAPIGSettingsFromGraphResponse(
        mockGraphResponses(200, correctJSONSettings.toString()))

    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(true)
  }

  private fun mockGraphResponses(responseCode: Int, graphRespJsonObjStr: String): GraphResponse {
    whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    whenever(FacebookSdk.isFullyInitialized()).thenReturn(true)

    val mockResponse = mock<GraphResponse>()
    if (responseCode == 200) {
      whenever(mockResponse.error).thenReturn(null)

      whenever(mockResponse.getJSONObject()).thenReturn(JSONObject(graphRespJsonObjStr))
    } else {
      val mockRequestError = mock<FacebookRequestError>()
      whenever(mockResponse.error).thenReturn(mockRequestError)
      whenever(mockRequestError.requestStatusCode).thenReturn(responseCode)
    }

    return mockResponse
  }
}
