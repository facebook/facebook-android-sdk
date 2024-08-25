/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.content.Context
import android.content.SharedPreferences
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import kotlin.test.assertNotNull

@PrepareForTest(FacebookSdk::class)
class AppLinkManagerTest: FacebookPowerMockTestCase()  {

  private lateinit var mockApplicationContext: Context
  private lateinit var mockSharedPreference: SharedPreferences

  @Before
  fun init() {
    mockApplicationContext = mock()
    mockSharedPreference = MockSharedPreference()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)
    whenever(mockApplicationContext.getSharedPreferences(any<String>(), any()))
      .thenReturn(mockSharedPreference)
  }

  @Test
  fun testGetInstance() {
    assertNotNull(AppLinkManager.getInstance())
  }
}
