/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.any
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(LocalBroadcastManager::class, FacebookSdk::class)
class BoltsMeasurementEventListenerTest : FacebookPowerMockTestCase() {
  private lateinit var mockLocalBroadcastManager: LocalBroadcastManager
  private lateinit var mockContext: Context
  private var capturedReceiver: BroadcastReceiver? = null
  private var capturedIntentFilter: IntentFilter? = null

  @Before
  fun init() {
    mockContext = PowerMockito.mock(Context::class.java)
    PowerMockito.`when`(mockContext.applicationContext)
        .thenReturn(ApplicationProvider.getApplicationContext())

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)

    mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    PowerMockito.`when`(mockLocalBroadcastManager.registerReceiver(any(), any())).thenAnswer {
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
