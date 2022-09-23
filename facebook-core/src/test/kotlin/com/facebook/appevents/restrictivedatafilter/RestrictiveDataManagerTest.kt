/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.restrictivedatafilter

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager.RestrictiveParamFilter
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager.enable
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager.processEvent
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager.processParameters
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FetchedAppSettings::class, FetchedAppSettingsManager::class)
class RestrictiveDataManagerTest : FacebookPowerMockTestCase() {

  companion object {
    private const val MOCK_APP_ID = "123"
    private val eventParam =
        mutableMapOf("key1" to "val1", "key2" to "val2", "last_name" to "ln", "first_name" to "fn")
  }

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID)
    Whitebox.setInternalState(RestrictiveDataManager::class.java, "enabled", true)
  }

  @Test
  fun testEnable() {
    val expectedParam = hashMapOf("last_name" to "0", "first_name" to "0")
    val expectedParamDetail =
        hashMapOf("Quantity" to "1", "Product name" to "Coffee", "Price" to "10")
    val jsonObject = JSONObject()

    val jsonObject1 = JSONObject()
    jsonObject1.put("restrictive_param", JSONObject(expectedParam as Map<*, *>))
    val jsonObject2 = JSONObject()
    jsonObject2.put("restrictive_param", JSONObject(expectedParamDetail as Map<*, *>))
    jsonObject2.put("process_event_name", false)
    jsonObject.put("fb_test_event", jsonObject1)
    jsonObject.put("manual_initiated_checkout", jsonObject2)

    val mockResponse = jsonObject.toString()
    val fetchedAppSettings: FetchedAppSettings = mock()
    whenever(fetchedAppSettings.restrictiveDataSetting).thenReturn(mockResponse)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    whenever(queryAppSettings(any(), any())).thenReturn(fetchedAppSettings)
    val restrictiveParamFilters =
        Whitebox.getInternalState<MutableList<RestrictiveParamFilter>>(
            RestrictiveDataManager::class.java, "restrictiveParamFilters")
    restrictiveParamFilters.clear()
    val restrictedEvents =
        Whitebox.getInternalState<MutableSet<String>>(
            RestrictiveDataManager::class.java, "restrictedEvents")
    restrictedEvents.clear()
    enable()
    Assert.assertEquals(2, restrictiveParamFilters.size.toLong())
    val rule = restrictiveParamFilters[0]
    Assert.assertEquals("fb_test_event", rule.eventName)
    Assert.assertEquals(expectedParam, rule.restrictiveParams)
    Assert.assertEquals(1, restrictedEvents.size.toLong())
    assertThat(restrictedEvents).contains("manual_initiated_checkout")
  }

  @Test
  fun testProcessParameters() {
    val mockRestrictiveParams = arrayListOf<RestrictiveParamFilter>()
    val mockParam = hashMapOf("last_name" to "0", "first_name" to "1")
    mockRestrictiveParams.add(RestrictiveParamFilter("fb_restrictive_event", mockParam))
    Whitebox.setInternalState(
        RestrictiveDataManager::class.java, "restrictiveParamFilters", mockRestrictiveParams)
    var mockEventParam = eventParam as MutableMap<String, String?>
    processParameters(mockEventParam, "fb_test_event")
    Assert.assertEquals(eventParam, mockEventParam)
    mockEventParam = eventParam
    processParameters(mockEventParam, "fb_restrictive_event")
    assertThat(mockEventParam.containsKey("key1")).isTrue
    assertThat(mockEventParam.containsKey("key2")).isTrue
    assertThat(mockEventParam.containsKey("_restrictedParams")).isTrue
    assertThat(mockEventParam.containsKey("last_name")).isFalse
    assertThat(mockEventParam.containsKey("first_name")).isFalse
  }

  @Test
  fun testProcessEvent() {
    val input = "name_should_be_replaced"
    Whitebox.setInternalState(
        RestrictiveDataManager::class.java, "restrictedEvents", hashSetOf(input))
    val output = processEvent(input)
    Assert.assertEquals(output, "_removed_")
  }
}
