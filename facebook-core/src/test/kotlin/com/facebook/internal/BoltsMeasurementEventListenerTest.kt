/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(LocalBroadcastManager::class, FacebookSdk::class)
class BoltsMeasurementEventListenerTest : FacebookPowerMockTestCase() {
  private lateinit var mockLocalBroadcastManager: LocalBroadcastManager
  private lateinit var mockContext: Context
  private lateinit var capturedReceiver: BroadcastReceiver
  private lateinit var capturedIntentFilter: IntentFilter

  @Before
  fun init() {
    mockContext = PowerMockito.mock(Context::class.java)
    whenever(mockContext.applicationContext).thenReturn(ApplicationProvider.getApplicationContext())

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)

    mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    whenever(mockLocalBroadcastManager.registerReceiver(any(), any())).thenAnswer {
      capturedReceiver = it.arguments[0] as BroadcastReceiver
      capturedIntentFilter = it.arguments[1] as IntentFilter
      Unit
    }
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    PowerMockito.`when`(
            LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()))
        .thenReturn(mockLocalBroadcastManager)
  }

  @Test
  fun `test getInstance registers the receiver`() {
    val listener = BoltsMeasurementEventListener.getInstance(mockContext)
    Assert.assertEquals(listener, capturedReceiver)
    Assert.assertEquals(
        capturedIntentFilter?.getAction(0),
        BoltsMeasurementEventListener.MEASUREMENT_EVENT_NOTIFICATION_NAME)
  }
}
