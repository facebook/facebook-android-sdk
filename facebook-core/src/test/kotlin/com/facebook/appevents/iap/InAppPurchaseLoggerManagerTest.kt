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
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class InAppPurchaseLoggerManagerTest : FacebookPowerMockTestCase() {
    private val mockExecutor: Executor = FacebookSerialExecutor()
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val packageName = "sample.packagename"
    private val TIME_OF_LAST_LOGGED_PURCHASE_KEY = "TIME_OF_LAST_LOGGED_PURCHASE"
    private val TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY = "TIME_OF_LAST_LOGGED_SUBSCRIPTION"

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
        editor = mock()
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java, "sharedPreferences", mockPrefs
        )
        whenever(mockPrefs.edit()).thenReturn(editor)
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenReturn(editor)
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putStringSet(any(), any())).thenReturn(editor)
        doNothing().whenever(editor).apply()
    }

    @Test
    fun testGetTimeOfNewestPurchaseInOldCache() {
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            mutableMapOf(
                "purchase_token_1" to 100,
                "purchase_token_2" to 200,
                "purchase_token_3" to 300
            )
        )
        Assertions.assertThat(InAppPurchaseLoggerManager.getTimeOfNewestPurchaseInOldCache())
            .isEqualTo(300000)
    }

    @Test
    fun testCacheDeDupPurchaseOnFirstTimeLoggingWithNewIAPImplementation() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenReturn(0)
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
        // Construct purchase details map
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"purchase_token\",\"purchaseTime\":1600000000000,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        // Construct cached purchase map
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "cachedPurchaseMap",
            mutableMapOf(
                "purchase_token" to
                        1_600_000_000L
            )
        )
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            true
        )

        // Test duplicate purchase event can be successfully removed from purchase details map
        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, false)
        Assertions.assertThat(cachedMap).isEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isEqualTo(1600000000000)
    }

    @Test
    fun testCacheDeDupPurchaseOnFirstTimeLoggingWithNewIAPImplementationAndNewPurchase() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenReturn(0)
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
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
                "otherpurchasetoken" to
                        lastClearedTime
            )
        )
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            true
        )

        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, false)
        Assertions.assertThat(cachedMap).isNotEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isEqualTo(1620000000000)
    }

    @Test
    fun testCacheDeDupPurchaseOnFirstTimeLoggingWithNewIAPImplementationAndInvalidCacheHistory() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenReturn(0)
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
        // Construct purchase details map
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1730358000001,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        // Construct cached purchase map
        val lastClearedTime = 1_740_000_000L
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "cachedPurchaseMap",
            mutableMapOf(
                "otherpurchasetoken" to
                        lastClearedTime
            )
        )
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            true
        )

        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, false)
        Assertions.assertThat(cachedMap).isNotEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isEqualTo(1730358000001)
    }

    @Test
    fun testCacheDeDupPurchaseNotFirstTimeLoggingWithNewIAPImplementation() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenReturn(
            1630000000000
        )
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
        // Construct purchase details map
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1620000000001,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            false
        )

        // Test duplicate purchase event can be successfully removed from purchase details map
        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, false)
        Assertions.assertThat(cachedMap).isEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isNull()
    }

    @Test
    fun testCacheDeDupPurchaseNotFirstTimeLoggingWithNewIAPImplementationAndNewPurchase() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenReturn(
            1620000000000
        )
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_PURCHASE_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
        // Construct purchase details map
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1630000000000000,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            false
        )

        // Test duplicate purchase event can be successfully removed from purchase details map
        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, false)
        Assertions.assertThat(cachedMap).isNotEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isEqualTo(1630000000000000)
    }

    @Test
    fun testCacheDeDupSubscriptionOnFirstTimeLoggingWithNewIAPImplementation() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenReturn(0)
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
        // Construct purchase details map
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1600000000000,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        // Construct cached purchase map
        val lastClearedTime = 1_600_000_000L
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "cachedPurchaseMap",
            mutableMapOf(
                "purchaseToken=" to
                        lastClearedTime
            )
        )
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            true
        )

        // Test duplicate purchase event can be successfully removed from purchase details map
        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, true)
        Assertions.assertThat(cachedMap).isEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isEqualTo(1600000000000)
    }

    @Test
    fun testCacheDeDupSubscriptionOnFirstTimeLoggingWithNewIAPImplementationAndNewPurchase() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenReturn(0)
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
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
                "otherpurchasetoken" to
                        lastClearedTime
            )
        )
        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            true
        )

        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, true)
        Assertions.assertThat(cachedMap).isNotEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isEqualTo(1620000000000)
    }

    @Test
    fun testCacheDeDupSubscriptionNotFirstTimeLoggingWithNewIAPImplementation() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenReturn(
            1620000000002
        )
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
        // Construct purchase details map
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1620000000001,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            false
        )

        // Test duplicate purchase event can be successfully removed from purchase details map
        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, true)
        Assertions.assertThat(cachedMap).isEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isNull()
    }

    @Test
    fun testCacheDeDupSubscriptionNotFirstTimeLoggingWithNewIAPImplementationAndNewPurchase() {
        var timeAddedToCache: Long? = null
        whenever(mockPrefs.getLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenReturn(
            1620000000000
        )
        whenever(editor.putLong(eq(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY), any())).thenAnswer {
            timeAddedToCache = it.getArgument(1) as Long
            return@thenAnswer editor
        }
        // Construct purchase details map
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val purchaseDetailJson1 =
            JSONObject(
                "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":1620000000000001,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
            )
        mockPurchaseDetailsMap["espresso"] = purchaseDetailJson1

        Whitebox.setInternalState(
            InAppPurchaseLoggerManager::class.java,
            "firstTimeLoggingIAP",
            false
        )

        // Test duplicate purchase event can be successfully removed from purchase details map
        val cachedMap = InAppPurchaseLoggerManager.cacheDeDupPurchase(mockPurchaseDetailsMap, true)
        Assertions.assertThat(cachedMap).isNotEmpty()
        Assertions.assertThat(
            Whitebox.getInternalState(
                InAppPurchaseLoggerManager::class.java,
                "firstTimeLoggingIAP"
            ) as Boolean

        ).isFalse()
        Assertions.assertThat(timeAddedToCache).isEqualTo(1620000000000001)
    }


    @Test
    fun testConstructLoggingReadyMap() {
        val mockPurchaseDetailsMap: MutableMap<String, JSONObject> = mutableMapOf()
        val mockSkuDetailsMap: MutableMap<String, JSONObject> = HashMap()
        val skuDetailJson =
            JSONObject(
                "{\"productId\":\"coffee\",\"type\":\"inapp\",\"price\":\"$2.99\",\"price_amount_micros\":2990000,\"price_currency_code\":\"USD\",\"title\":\"coffee\",\"description\":\"Basic coffee \",\"skuDetailsToken\":\"detailsToken=\"}"
            )
        mockSkuDetailsMap["espresso"] = skuDetailJson

        // Test logging ready events can be added into map
        var newPurchaseDetailString =
            "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":%s,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}"
        val currTime = System.currentTimeMillis()
        newPurchaseDetailString = String.format(newPurchaseDetailString, currTime)
        val newPurchaseDetailStringJson = JSONObject(newPurchaseDetailString)
        mockPurchaseDetailsMap.clear()
        mockPurchaseDetailsMap["espresso"] = newPurchaseDetailStringJson
        val result2 =
            InAppPurchaseLoggerManager.constructLoggingReadyMap(
                mockPurchaseDetailsMap, mockSkuDetailsMap,
                packageName
            )
        Assertions.assertThat(result2).isNotEmpty
        val expectedResult = String.format(
            "{\"productId\":\"espresso\",\"purchaseToken\":\"token123\",\"purchaseTime\":%s,\"developerPayload\":null,\"packageName\":\"sample.packagename\"}",
            currTime
        )
        Assertions.assertThat(result2.containsKey(expectedResult))
            .isTrue()
    }
}
