/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.applinks

import android.content.Context
import android.content.SharedPreferences
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import com.facebook.internal.FeatureManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

/** Tests the first-launch-window gate on the deferred app link fetch. */
@PrepareForTest(FacebookSdk::class, FeatureManager::class)
class AppLinkDataDeferredFetchTest : FacebookPowerMockTestCase() {

  private lateinit var mockContext: Context
  private lateinit var sharedPreferences: SharedPreferences

  @Before
  fun init() {
    sharedPreferences = MockSharedPreference()
    mockContext = mock()
    whenever(mockContext.getSharedPreferences(any<String>(), any())).thenReturn(sharedPreferences)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APPLICATION_ID)
    PowerMockito.mockStatic(FeatureManager::class.java)
  }

  @Test
  fun `test first fetch is allowed and records the timestamp`() {
    val before = System.currentTimeMillis()

    assertFalse(shouldSkip())

    val recorded = sharedPreferences.getLong(FIRST_FETCH_TIME_KEY, 0L)
    assertTrue(recorded >= before)
    assertTrue(recorded <= System.currentTimeMillis())
  }

  @Test
  fun `test a later fetch inside the window is still allowed`() {
    // A retry after a failed first attempt, or a second call in the same session.
    setFirstFetchTime(System.currentTimeMillis() - FIVE_MINUTES_MS)

    assertFalse(shouldSkip())
  }

  @Test
  fun `test the recorded timestamp is not overwritten by a later fetch`() {
    val original = System.currentTimeMillis() - FIVE_MINUTES_MS
    setFirstFetchTime(original)

    shouldSkip()

    assertEquals(original, sharedPreferences.getLong(FIRST_FETCH_TIME_KEY, 0L))
  }

  @Test
  fun `test a fetch past the window is refused`() {
    setFirstFetchTime(System.currentTimeMillis() - TWO_HOURS_MS)

    assertTrue(shouldSkip())
  }

  @Test
  fun `test a backwards clock fails open`() {
    setFirstFetchTime(System.currentTimeMillis() + TWO_HOURS_MS)

    assertFalse(shouldSkip())
  }

  @Test
  fun `test a link already received stops all further fetches`() {
    // Inside the window, but the server has already handed over the link and dropped its record.
    setFirstFetchTime(System.currentTimeMillis())
    sharedPreferences.edit().putBoolean(LINK_RECEIVED_KEY, true).apply()

    assertTrue(shouldSkip())
  }

  @Test
  fun `test gated fetch short-circuits once the window has closed`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidDeferredAppLinkFirstLaunchOnly))
        .thenReturn(true)
    setFirstFetchTime(System.currentTimeMillis() - TWO_HOURS_MS)
    val fetched = mutableListOf<AppLinkData?>()

    fetchFromServer { appLinkData -> fetched.add(appLinkData) }

    assertEquals(1, fetched.size)
    assertNull(fetched[0])
  }

  private fun setFirstFetchTime(timeMillis: Long) {
    sharedPreferences.edit().putLong(FIRST_FETCH_TIME_KEY, timeMillis).apply()
  }

  private fun shouldSkip(): Boolean =
      Whitebox.invokeMethod(AppLinkData::class.java, "shouldSkipDeferredAppLinkFetch", mockContext)

  private fun fetchFromServer(completionHandler: AppLinkData.CompletionHandler) {
    Whitebox.invokeMethod<Any?>(
        AppLinkData::class.java,
        "fetchDeferredAppLinkFromServer",
        mockContext,
        APPLICATION_ID,
        completionHandler)
  }

  companion object {
    private const val APPLICATION_ID = "123456789"
    private const val FIRST_FETCH_TIME_KEY = "fbsdk_ddl_first_fetch_time"
    private const val LINK_RECEIVED_KEY = "fbsdk_ddl_link_received"
    private const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
  }
}
