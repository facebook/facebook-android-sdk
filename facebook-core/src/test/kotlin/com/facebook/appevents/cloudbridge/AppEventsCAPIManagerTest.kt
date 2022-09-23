/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.cloudbridge

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.GraphResponse
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@PowerMockIgnore("org.powermock.*", "org.mockito.*", "org.robolectric.*", "kotlin.*", "kotlinx.*")
@RunWith(RobolectricTestRunner::class)
@PrepareForTest(FacebookSdk::class, LocalBroadcastManager::class)
@Config(manifest = Config.NONE)
class AppEventsCAPIManagerTest : FacebookPowerMockTestCase() {

  enum class Values(val rawValue: String) {
    DATASETID("id123"),
    ACCESSKEY("key123"),
    URL("https://www.123.com")
  }

  private val mockAppID = "1234"
  private val mockClientToken = "5678"

  private val correctJSONSettings =
      mapOf<String, Any>(
          "data" to
              listOf(
                  mapOf<String, Any>(
                      SettingsAPIFields.ENABLED.rawValue to true,
                      SettingsAPIFields.ACCESSKEY.rawValue to
                          "\"" + Values.ACCESSKEY.rawValue + "\"",
                      SettingsAPIFields.DATASETID.rawValue to
                          "\"" + Values.DATASETID.rawValue + "\"",
                      SettingsAPIFields.URL.rawValue to "\"" + Values.URL.rawValue + "\"")))

  private val incorrectJSONSettings =
      listOf(
          mapOf<String, Any>(
              "data" to
                  listOf(
                      mapOf<String, Any>(
                          SettingsAPIFields.ENABLED.rawValue to true,
                      ))),
          mapOf<String, Any>("data" to emptyList<String>()),
          mapOf<String, Any>("data" to listOf(null)))

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

    AppEventsCAPIManager.isEnabled = false
  }

  @After
  fun afterEach() {
    AppEventsCAPIManager.isEnabled = false
  }

  @Test
  fun testEnableWithNetworkErrorAndSharedPrefsNotSet() {

    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)

    AppEventsCAPIManager.savedCloudBridgeCredentials = null
    AppEventsCAPIManager.getCAPIGSettingsFromGraphResponse(
        mockGraphResponses(400, correctJSONSettings.toString()))

    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)
  }

  @Test
  fun testEnableWithNetworkErrorAndSharedPrefsSet() {
    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)

    val savedSettings: MutableMap<String, String> = mutableMapOf()
    savedSettings[SettingsAPIFields.URL.rawValue] = Values.URL.rawValue
    savedSettings[SettingsAPIFields.DATASETID.rawValue] = Values.DATASETID.rawValue
    savedSettings[SettingsAPIFields.ACCESSKEY.rawValue] = Values.ACCESSKEY.rawValue
    AppEventsCAPIManager.savedCloudBridgeCredentials = savedSettings
    AppEventsCAPIManager.getCAPIGSettingsFromGraphResponse(
        mockGraphResponses(400, correctJSONSettings.toString()))

    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(true)
  }

  @Test
  fun testEnableWithoutNetworkErrorWrongJSON() {

    for (settings in incorrectJSONSettings) {

      assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)

      AppEventsCAPIManager.savedCloudBridgeCredentials = null
      AppEventsCAPIManager.getCAPIGSettingsFromGraphResponse(
          mockGraphResponses(200, settings.toString()))

      assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)
    }
  }

  @Test
  fun testEnableWithoutNetworkErrorRightJSON() {
    assertThat(AppEventsCAPIManager.isEnabled).isEqualTo(false)

    AppEventsCAPIManager.savedCloudBridgeCredentials = null
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
