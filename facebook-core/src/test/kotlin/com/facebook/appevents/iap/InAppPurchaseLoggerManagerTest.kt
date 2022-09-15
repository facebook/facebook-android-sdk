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

package com.facebook.appevents.iap

import android.content.Context
import android.content.SharedPreferences
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class InAppPurchaseLoggerManagerTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  private lateinit var mockPrefs: SharedPreferences
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    val context: Context = mock()
    whenever(FacebookSdk.getApplicationContext()).thenReturn(context)

    PowerMockito.spy(InAppPurchaseLoggerManager::class.java)
    mockPrefs = mock()
    whenever(context.getSharedPreferences(any<String>(), any<Int>())).thenReturn(mockPrefs)
    val editor: SharedPreferences.Editor = mock()
    Whitebox.setInternalState(
        InAppPurchaseLoggerManager::class.java, "sharedPreferences", mockPrefs)
    whenever(mockPrefs.edit()).thenReturn(editor)
    whenever(editor.putLong(any(), any())).thenReturn(editor)
    whenever(editor.putString(any(), any())).thenReturn(editor)
    whenever(editor.putStringSet(any(), any())).thenReturn(editor)
  }

  @Test
  fun testCacheDeDupPurchase() {
    // Construct purchase details map
    val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
    val purchaseDetailJson1 =
        JSONObject(
            "{\"productId\":\"espresso\",\"purchaseToken\":\"ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4\",\"purchaseTime\":1620000000000,\"developerPayload\":null,\"packageName\":\"com.cfsample.coffeeshop\"}")
    mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

    // Construct cached purchase map
    val lastClearedTime = 1_600_000_000L
    Whitebox.setInternalState(
        InAppPurchaseLoggerManager::class.java,
        "cachedPurchaseMap",
        mutableMapOf(
            "ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4" to
                lastClearedTime))

    // Test duplicate purchase event can be successfully removed from purchase details map
    val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap)
    Assertions.assertThat(cachedMap).isEmpty()
  }

  @Test
  fun testClearOutDatedProductInfoInCache() {
    // Add clear history data into cachedPurchaseMap
    val lastClearedTime = 1_600_000_000L
    whenever(mockPrefs.getLong("LAST_CLEARED_TIME", 0)).thenReturn(lastClearedTime)
    Whitebox.setInternalState(
        InAppPurchaseLoggerManager::class.java,
        "cachedPurchaseMap",
        mutableMapOf(
            "AEuhp4LDP2FB_51qEWpJOLSDtCZoq3-jLL1rRPd4V7k9c5RyHc9Phx8iYBQqvJFYhfI=" to
                lastClearedTime))

    InAppPurchaseLoggerManager.clearOutdatedProductInfoInCache()
    val cachedPurchaseMap =
        Whitebox.getInternalState<MutableMap<String, Long>>(
            InAppPurchaseLoggerManager::class.java, "cachedPurchaseMap")

    Assertions.assertThat(cachedPurchaseMap).isEmpty()
  }

  @Test
  fun testEligibleQueryPurchaseHistory() {
    // Mock return last query purchase history time
    var mockLastQueryPurchaseTimeStamp: Long = 0
    whenever(mockPrefs.getLong("LAST_QUERY_PURCHASE_HISTORY_TIME", 0))
        .thenReturn(mockLastQueryPurchaseTimeStamp)

    // Test eligible to query purchase history if no last query history time has been set
    val result1 = InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()
    Assertions.assertThat(result1).isTrue

    // Test not eligible to query purchase history if the time interval is shorter than 1 day
    mockLastQueryPurchaseTimeStamp = System.currentTimeMillis() / 1_000L
    whenever(mockPrefs.getLong("LAST_QUERY_PURCHASE_HISTORY_TIME", 0))
        .thenReturn(mockLastQueryPurchaseTimeStamp)
    val result2 = InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()
    Assertions.assertThat(result2).isFalse
  }

  @Test
  fun testConstructLoggingReadyMap() {
    val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
    val purchaseDetailJson1 =
        JSONObject(
            "{\"productId\":\"espresso\",\"purchaseToken\":\"ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4\",\"purchaseTime\":1620000000000,\"developerPayload\":null,\"packageName\":\"com.cfsample.coffeeshop\"}")
    mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1
    val mockSkuDetailsMap: MutableMap<String, JSONObject> = HashMap()
    val skuDetailJson =
        JSONObject(
            "{\"productId\":\"coffee\",\"type\":\"inapp\",\"price\":\"$2.99\",\"price_amount_micros\":2990000,\"price_currency_code\":\"USD\",\"title\":\"coffee (coffeeshop)\",\"description\":\"Basic coffee \",\"skuDetailsToken\":\"AEuhp4LDP2FB_51qEWpJOLSDtCZoq3-jLL1rRPd4V7k9c5RyHc9Phx8iYBQqvJFYhfI=\"}")
    mockSkuDetailsMap["espresso"] = skuDetailJson

    // Test purchase is too old to log
    val result1 =
        InAppPurchaseLoggerManager.constructLoggingReadyMap(
            mockPurchaseDetailsMap, mockSkuDetailsMap)
    Assertions.assertThat(result1).isEmpty()

    // Test logging ready events can be added into map
    var newPurchaseDetailString =
        "{\"productId\":\"espresso\",\"purchaseToken\":\"ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4\",\"purchaseTime\":%s,\"developerPayload\":null,\"packageName\":\"com.cfsample.coffeeshop\"}"
    newPurchaseDetailString = String.format(newPurchaseDetailString, System.currentTimeMillis())
    val newPurchaseDetailStringJson = JSONObject(newPurchaseDetailString)
    mockPurchaseDetailsMap.clear()
    mockPurchaseDetailsMap["espresso"] = newPurchaseDetailStringJson
    val result2 =
        InAppPurchaseLoggerManager.constructLoggingReadyMap(
            mockPurchaseDetailsMap, mockSkuDetailsMap)
    Assertions.assertThat(result2).isNotEmpty
  }
}
