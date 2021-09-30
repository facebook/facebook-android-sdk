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
package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventTestUtilities.BundleMatcher
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.powermock.reflect.Whitebox
import org.robolectric.RuntimeEnvironment

class InternalAppEventsLoggerTest : FacebookPowerMockTestCase() {
  private val mockEventName = "fb_mock_event"
  private val serialExecutor: Executor = FacebookSerialExecutor()

  @Mock private lateinit var logger: AppEventsLoggerImpl
  @Before
  fun setupTest() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", serialExecutor)
  }

  @Test
  fun testAutoLogAppEventsDisabled() {
    val mockPayload = Bundle()
    val mockVal = BigDecimal(1.0)
    val mockCurrency = Currency.getInstance(Locale.US)
    FacebookSdk.setAutoLogAppEventsEnabled(false)
    val internalLogger = InternalAppEventsLogger(logger)
    internalLogger.logEvent(mockEventName, mockPayload)
    internalLogger.logEvent(mockEventName, 1.0, mockPayload)
    internalLogger.logEventImplicitly(mockEventName)
    internalLogger.logEventImplicitly(mockEventName, mockPayload)
    internalLogger.logEventImplicitly(mockEventName, mockVal, mockCurrency, mockPayload)
    internalLogger.logPurchaseImplicitly(mockVal, mockCurrency, mockPayload)
    verify(logger, never()).logEvent(ArgumentMatchers.anyString())
    verify(logger, never()).logEvent(ArgumentMatchers.anyString(), any<Bundle>())
    verify(logger, never()).logEvent(ArgumentMatchers.anyString(), ArgumentMatchers.anyDouble())
    verify(logger, never())
        .logEvent(ArgumentMatchers.anyString(), ArgumentMatchers.anyDouble(), any<Bundle>())
    verify(logger, never())
        .logEventImplicitly(
            ArgumentMatchers.anyString(), ArgumentMatchers.anyDouble(), any<Bundle>())
    verify(logger, never())
        .logEventImplicitly(
            ArgumentMatchers.anyString(), any<BigDecimal>(), any<Currency>(), any<Bundle>())
    verify(logger, never()).logPurchase(any<BigDecimal>(), any<Currency>())
    verify(logger, never()).logPurchase(any<BigDecimal>(), any<Currency>(), any<Bundle>())
    verify(logger, never())
        .logPurchase(
            any<BigDecimal>(), any<Currency>(), any<Bundle>(), ArgumentMatchers.anyBoolean())
    verify(logger, never()).logPurchaseImplicitly(any<BigDecimal>(), any<Currency>(), any<Bundle>())
  }

  @Test
  fun testInternalAppEventsLoggerLogFunctions() {
    val mockPayload = Bundle()
    val mockVal = BigDecimal(1.0)
    val mockCurrency = Currency.getInstance(Locale.US)
    FacebookSdk.setAutoLogAppEventsEnabled(true)
    val internalLogger = InternalAppEventsLogger(logger)
    internalLogger.logEvent(mockEventName, null)
    verify(logger, times(1)).logEvent(mockEventName, null)
    internalLogger.logEvent(mockEventName, 1.0, mockPayload)
    verify(logger, times(1))
        .logEvent(eq(mockEventName), eq(1.0), ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logEventImplicitly(mockEventName)
    verify(logger, times(1)).logEventImplicitly(mockEventName, null, null)
    internalLogger.logEventImplicitly(mockEventName, mockPayload)
    verify(logger, times(1))
        .logEventImplicitly(
            eq(mockEventName),
            ArgumentMatchers.isNull(Double::class.java),
            ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logEventImplicitly(mockEventName, 1.0, mockPayload)
    verify(logger, times(1))
        .logEventImplicitly(
            eq(mockEventName), eq(1.0), ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logEventImplicitly(mockEventName, mockVal, mockCurrency, mockPayload)
    verify(logger, times(1))
        .logEventImplicitly(
            eq(mockEventName),
            eq(mockVal),
            eq(mockCurrency),
            ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logPurchaseImplicitly(mockVal, mockCurrency, mockPayload)
    verify(logger, times(1))
        .logPurchaseImplicitly(
            eq(mockVal), eq(mockCurrency), ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
  }
}
