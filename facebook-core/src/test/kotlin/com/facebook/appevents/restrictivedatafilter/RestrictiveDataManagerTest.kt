/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    RestrictiveDataManager::class,
    FacebookSdk::class,
    FetchedAppSettings::class,
    FetchedAppSettingsManager::class)
class RestrictiveDataManagerTest : FacebookPowerMockTestCase() {

  companion object {
    private const val MOCK_APP_ID = "123"
    private val eventParam =
        mutableMapOf("key1" to "val1", "key2" to "val2", "last_name" to "ln", "first_name" to "fn")
  }

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.spy(RestrictiveDataManager::class.java)
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
    Assert.assertTrue(restrictedEvents.contains("manual_initiated_checkout"))
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
    Assert.assertTrue(mockEventParam.containsKey("key1"))
    Assert.assertTrue(mockEventParam.containsKey("key2"))
    Assert.assertTrue(mockEventParam.containsKey("_restrictedParams"))
    Assert.assertFalse(mockEventParam.containsKey("last_name"))
    Assert.assertFalse(mockEventParam.containsKey("first_name"))
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
