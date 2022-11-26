/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.eventdeactivation

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.eventdeactivation.EventDeactivationManager.DeprecatedParamFilter
import com.facebook.appevents.eventdeactivation.EventDeactivationManager.enable
import com.facebook.appevents.eventdeactivation.EventDeactivationManager.processDeprecatedParameters
import com.facebook.appevents.eventdeactivation.EventDeactivationManager.processEvents
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import org.assertj.core.api.Assertions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FetchedAppSettings::class, FetchedAppSettingsManager::class)
class EventDeactivationManagerTest : FacebookPowerMockTestCase() {
  companion object {
    private const val MOCK_APP_ID = "123"
    private val eventParam: Map<String, String?> =
        hashMapOf("last_name" to "ln", "first_name" to "fn", "ssn" to "val3")

    private fun getAppEvent(eventName: String): AppEvent {
      return AppEvent("", eventName, 0.0, null, false, false, null)
    }
  }

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID)
    Whitebox.setInternalState(EventDeactivationManager::class.java, "enabled", true)
  }

  @Test
  fun testEnable() {
    val expectedParam = hashMapOf("last_name" to "0", "first_name" to "0")
    val map = hashMapOf("is_deprecated_event" to true)
    val expectDeprecatedParam = listOf("ssn", "mid_name")
    val jsonObject = JSONObject()
    val jsonObject1 = JSONObject()
    val jsonObject2 = JSONObject()
    jsonObject1.put("restrictive_param", JSONObject(expectedParam as Map<*, *>))
    jsonObject2.put("deprecated_param", JSONArray(expectDeprecatedParam))
    jsonObject.put("fb_deprecated_event", JSONObject(map as Map<*, *>))
    jsonObject.put("fb_test_event", jsonObject1)
    jsonObject.put("fb_test_deprecated_event", jsonObject2)
    val mockResponse = jsonObject.toString()
    val fetchedAppSettings: FetchedAppSettings = mock()
    whenever(fetchedAppSettings.restrictiveDataSetting).thenReturn(mockResponse)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    whenever(queryAppSettings(any(), any())).thenReturn(fetchedAppSettings)
    enable()
    val deprecatedParams =
        Whitebox.getInternalState<List<DeprecatedParamFilter>>(
            EventDeactivationManager::class.java, "deprecatedParamFilters")
    val deprecatedEvents =
        Whitebox.getInternalState<Set<String>>(
            EventDeactivationManager::class.java, "deprecatedEvents")
    Assertions.assertThat(deprecatedParams.size).isEqualTo(2)
    val rule = deprecatedParams[0]
    Assertions.assertThat(rule.eventName).isEqualTo("fb_test_event")
    Assertions.assertThat(deprecatedEvents.size).isEqualTo(1)
    Assertions.assertThat(deprecatedEvents.contains("fb_deprecated_event")).isEqualTo(true)
    val real = deprecatedParams[1]
    Assertions.assertThat(real.eventName).isEqualTo("fb_test_deprecated_event")
    Assertions.assertThat(real.deprecateParams).isEqualTo(expectDeprecatedParam)
  }

  @Test
  fun testProcessEvents() {
    val deprecatedEvents: MutableSet<String> = HashSet()
    deprecatedEvents.add("fb_deprecated_event")
    Whitebox.setInternalState(
        EventDeactivationManager::class.java, "deprecatedEvents", deprecatedEvents)
    val mockAppEvents: MutableList<AppEvent> = ArrayList()
    mockAppEvents.add(getAppEvent("fb_mobile_install"))
    mockAppEvents.add(getAppEvent("fb_deprecated_event"))
    mockAppEvents.add(getAppEvent("fb_sdk_initialized"))
    val expectedEventNames = arrayOf("fb_mobile_install", "fb_sdk_initialized")
    processEvents(mockAppEvents)
    Assertions.assertThat(mockAppEvents.size).isEqualTo(2)
    for (i in expectedEventNames.indices) {
      Assertions.assertThat(mockAppEvents[i].name).isEqualTo(expectedEventNames[i])
    }
  }

  @Test
  fun testProcessDeprecatedParameters() {
    val mockDeprecatedParams: MutableList<DeprecatedParamFilter> = ArrayList()
    val mockParameters: MutableMap<String, String> = HashMap()
    mockParameters["last_name"] = "0"
    mockParameters["first_name"] = "1"
    val mockDeprecatedParam: MutableList<String> = ArrayList()
    mockDeprecatedParam.add("ssn")
    mockDeprecatedParams.add(DeprecatedParamFilter("fb_restrictive_event", mockDeprecatedParam))
    Whitebox.setInternalState(
        EventDeactivationManager::class.java, "deprecatedParamFilters", mockDeprecatedParams)
    var mockEventParam = eventParam.toMutableMap()
    processDeprecatedParameters(mockEventParam, "fb_test_event")
    Assertions.assertThat(mockEventParam).isEqualTo(eventParam)
    mockEventParam = eventParam.toMutableMap()
    processDeprecatedParameters(mockEventParam, "fb_restrictive_event")
    Assertions.assertThat(mockEventParam.containsKey("last_name")).isEqualTo(true)
    Assertions.assertThat(mockEventParam.containsKey("first_name")).isEqualTo(true)
    Assertions.assertThat(mockEventParam.containsKey("ssn")).isEqualTo(false)
  }
}
