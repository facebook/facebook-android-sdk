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

import android.app.Application
import android.content.Intent
import android.content.ServiceConnection
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.appevents.internal.AutomaticAnalyticsLogger
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    AutomaticAnalyticsLogger::class,
    InternalAppEventsLogger::class,
    InAppPurchaseActivityLifecycleTracker::class,
    InAppPurchaseEventManager::class,
    InAppPurchaseUtils::class)
class InAppPurchaseActivityLifecycleTrackerTest : FacebookPowerMockTestCase() {
  private lateinit var applicationContext: Application

  override fun setup() {
    super.setup()
    PowerMockito.whenNew(InternalAppEventsLogger::class.java).withAnyArguments().thenReturn(mock())
    applicationContext = mock<Application>()
    whenever(applicationContext.registerActivityLifecycleCallbacks(any())).thenAnswer {}
    whenever(applicationContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()))
        .thenReturn(true)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn("123456789")
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(applicationContext)
    PowerMockito.mockStatic(AutomaticAnalyticsLogger::class.java)
    PowerMockito.mockStatic(InAppPurchaseEventManager::class.java)

    Whitebox.setInternalState(
        InAppPurchaseActivityLifecycleTracker::class.java, "hasBillingService", null as Boolean?)
    Whitebox.setInternalState(
        InAppPurchaseActivityLifecycleTracker::class.java, "isTracking", AtomicBoolean(false))
    PowerMockito.spy(InAppPurchaseActivityLifecycleTracker::class.java)
    PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
    PowerMockito.doAnswer { this.javaClass }
        .`when`(InAppPurchaseUtils::class.java, "getClass", any<String>())
  }

  @Test
  fun `test startIapLogging will bind iap intent and lifecycle callback`() {
    val intentCaptor = argumentCaptor<Intent>()
    PowerMockito.`when`(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled())
        .thenReturn(true)
    InAppPurchaseActivityLifecycleTracker.startIapLogging()
    verify(applicationContext).registerActivityLifecycleCallbacks(any())
    verify(applicationContext)
        .bindService(intentCaptor.capture(), any<ServiceConnection>(), any<Int>())
    assertThat(intentCaptor.firstValue.action)
        .isEqualTo("com.android.vending.billing.InAppBillingService.BIND")
    assertThat(intentCaptor.firstValue.`package`).isEqualTo("com.android.vending")
  }

  @Test
  fun `test startIapLogging will only register once`() {
    PowerMockito.`when`(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled())
        .thenReturn(true)
    InAppPurchaseActivityLifecycleTracker.startIapLogging()
    InAppPurchaseActivityLifecycleTracker.startIapLogging()
    verify(applicationContext, times(1)).registerActivityLifecycleCallbacks(any())
  }

  @Test
  fun `test startIapLogging will not register if implicit purchase disabled`() {
    PowerMockito.`when`(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled())
        .thenReturn(false)
    InAppPurchaseActivityLifecycleTracker.startIapLogging()
    verify(applicationContext, never()).registerActivityLifecycleCallbacks(any())
  }
}
