/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
            InAppPurchaseLoggerManager::class.java, "sharedPreferences", mockPrefs
        )
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
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1620000000000,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        // Construct cached purchase map
        val lastClearedTime = 1_600_000_000L
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "cachedPurchaseMap",
            mutableMapOf(
                "token123" to
                        lastClearedTime
            )
        )

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
                "purchaseToken=" to
                        lastClearedTime
            )
        )

        InAppPurchaseLoggerManager.clearOutdatedProductInfoInCache()
        val cachedPurchaseMap =
            Whitebox.getInternalState<MutableMap<String, Long>>(
                InAppPurchaseLoggerManager::class.java, "cachedPurchaseMap"
            )

        Assertions.assertThat(cachedPurchaseMap).isEmpty()
    }

    @Test
    fun testConstructLoggingReadyMap() {
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1620000000000,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1
        val mockSkuDetailsMap: MutableMap<String, JSONObject> = HashMap()
        val skuDetailJson =
            JSONObject(
                "{\"productId\":\"coffee\",\"type\":\"inapp\",\"price\":\"$2.99\",\"price_amount_micros\":2990000,\"price_currency_code\":\"USD\",\"title\":\"coffee\",\"description\":\"Basic coffee \",\"skuDetailsToken\":\"detailsToken=\"}"
            )
        mockSkuDetailsMap["espresso"] = skuDetailJson

        // Test purchase is too old to log
        val result1 =
            InAppPurchaseLoggerManager.constructLoggingReadyMap(
                mockPurchaseDetailsMap, mockSkuDetailsMap
            )
        Assertions.assertThat(result1).isEmpty()

        // Test logging ready events can be added into map
        var newPurchaseDetailString =
            "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":%s,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
        newPurchaseDetailString = String.format(newPurchaseDetailString, System.currentTimeMillis())
        val newPurchaseDetailStringJson = JSONObject(newPurchaseDetailString)
        mockPurchaseDetailsMap.clear()
        mockPurchaseDetailsMap["espresso"] = newPurchaseDetailStringJson
        val result2 =
            InAppPurchaseLoggerManager.constructLoggingReadyMap(
                mockPurchaseDetailsMap, mockSkuDetailsMap
            )
        Assertions.assertThat(result2).isNotEmpty
    }
}
