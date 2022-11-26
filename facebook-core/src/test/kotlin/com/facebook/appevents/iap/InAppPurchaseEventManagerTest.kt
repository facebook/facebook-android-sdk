/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, InAppPurchaseUtils::class)
class InAppPurchaseEventManagerTest : FacebookPowerMockTestCase() {
  private lateinit var mockContext: Context
  private val nowSec = System.currentTimeMillis() / 1_000L
  private val oneHourAgo = nowSec - 60 * 60
  private val twoHourAgo = nowSec - 60 * 60 * 2
  private val eightDaysAgo = nowSec - 60 * 60 * 24 * 8

  companion object {
    private const val KEY1 = "product_1"
    private const val KEY2 = "product_2"
    private const val VAL1 = "123"
    private const val VAL2 = "234"
  }

  public fun dummyMethod() {}

  @Before
  fun init() {
    mockContext = mock()
    whenever(mockContext.packageName).thenReturn("com.facebook.appevents.iap")

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    whenever(InAppPurchaseUtils.getClassFromContext(any(), any()))
        .thenReturn(InAppPurchaseEventManagerTest::class.java)
    whenever(InAppPurchaseUtils.getDeclaredMethod(any(), any(), anyVararg()))
        .thenReturn(InAppPurchaseEventManagerTest::class.java.getMethod("dummyMethod"))
  }

  @Test
  fun testGetSkuDetailsWhenCacheIsEmpty() {
    val mockSharedPreferences = MockSharedPreference()
    Whitebox.setInternalState(
        InAppPurchaseEventManager::class.java, "skuDetailSharedPrefs", mockSharedPreferences)
    val result = Bundle()
    result.putInt("RESPONSE_CODE", 0)
    result.putStringArrayList("DETAILS_LIST", arrayListOf(VAL1, VAL2))
    whenever(InAppPurchaseUtils.invokeMethod(any(), any(), any(), anyVararg())).thenReturn(result)
    val billingObj = Bundle()
    val skuDetails =
        InAppPurchaseEventManager.getSkuDetails(
            mockContext, arrayListOf<String>(KEY1, KEY2), billingObj, false)
    assertThat(skuDetails.size).isEqualTo(2)
    assertThat(skuDetails[KEY1]).isEqualTo(VAL1)
    assertThat(skuDetails[KEY2]).isEqualTo(VAL2)

    assertThat(mockSharedPreferences.getString(KEY1, "")).contains(VAL1)
    assertThat(mockSharedPreferences.getString(KEY2, "")).contains(VAL2)
  }

  @Test
  fun testGetSkuDetailsWhenCacheIsNotEmpty() {
    val mockSharedPreferences = MockSharedPreference()
    mockSharedPreferences.edit().putString(KEY1, "$oneHourAgo;$VAL1").apply()
    Whitebox.setInternalState(
        InAppPurchaseEventManager::class.java, "skuDetailSharedPrefs", mockSharedPreferences)
    val result = Bundle()
    result.putInt("RESPONSE_CODE", 0)
    result.putStringArrayList("DETAILS_LIST", arrayListOf(VAL2))
    whenever(InAppPurchaseUtils.invokeMethod(any(), any(), any(), anyVararg())).thenReturn(result)
    val billingObj = Bundle()

    val skuDetails =
        InAppPurchaseEventManager.getSkuDetails(
            mockContext, arrayListOf<String>(KEY1, KEY2), billingObj, false)
    assertThat(skuDetails.size).isEqualTo(2)
    assertThat(skuDetails[KEY1]).isEqualTo(VAL1)
    assertThat(skuDetails[KEY2]).isEqualTo(VAL2)

    assertThat(mockSharedPreferences.getString(KEY1, "")).contains(VAL1)
    assertThat(mockSharedPreferences.getString(KEY2, "")).contains(VAL2)
  }

  @Test
  fun testClearSkuDetailsCache() {
    val mockSharedPreferences = MockSharedPreference()
    mockSharedPreferences
        .edit()
        .putString(KEY1, "$oneHourAgo;$VAL1")
        .putString(KEY2, "$twoHourAgo;$VAL2")
        .putLong("LAST_CLEARED_TIME", eightDaysAgo)
        .apply()
    Whitebox.setInternalState(
        InAppPurchaseEventManager::class.java, "skuDetailSharedPrefs", mockSharedPreferences)

    InAppPurchaseEventManager.clearSkuDetailsCache()

    assertThat(mockSharedPreferences.getString(KEY1, null)).isNull()
    assertThat(mockSharedPreferences.getString(KEY2, null)).isNull()
    assertThat(mockSharedPreferences.getLong("LAST_CLEARED_TIME", 0L)).isNotEqualTo(0L)
  }
}
