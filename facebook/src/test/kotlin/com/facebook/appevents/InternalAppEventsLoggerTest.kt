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
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
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
    Mockito.verify(logger, Mockito.never()).logEvent(ArgumentMatchers.anyString())
    Mockito.verify(logger, Mockito.never())
        .logEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Bundle::class.java))
    Mockito.verify(logger, Mockito.never())
        .logEvent(ArgumentMatchers.anyString(), ArgumentMatchers.anyDouble())
    Mockito.verify(logger, Mockito.never())
        .logEvent(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyDouble(),
            ArgumentMatchers.any(Bundle::class.java))
    Mockito.verify(logger, Mockito.never())
        .logEventImplicitly(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyDouble(),
            ArgumentMatchers.any(Bundle::class.java))
    Mockito.verify(logger, Mockito.never())
        .logEventImplicitly(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(BigDecimal::class.java),
            ArgumentMatchers.any(Currency::class.java),
            ArgumentMatchers.any(Bundle::class.java))
    Mockito.verify(logger, Mockito.never())
        .logPurchase(
            ArgumentMatchers.any(BigDecimal::class.java),
            ArgumentMatchers.any(Currency::class.java))
    Mockito.verify(logger, Mockito.never())
        .logPurchase(
            ArgumentMatchers.any(BigDecimal::class.java),
            ArgumentMatchers.any(Currency::class.java),
            ArgumentMatchers.any(Bundle::class.java))
    Mockito.verify(logger, Mockito.never())
        .logPurchase(
            ArgumentMatchers.any(BigDecimal::class.java),
            ArgumentMatchers.any(Currency::class.java),
            ArgumentMatchers.any(Bundle::class.java),
            ArgumentMatchers.anyBoolean())
    Mockito.verify(logger, Mockito.never())
        .logPurchaseImplicitly(
            ArgumentMatchers.any(BigDecimal::class.java),
            ArgumentMatchers.any(Currency::class.java),
            ArgumentMatchers.any(Bundle::class.java))
  }

  @Test
  fun testInternalAppEventsLoggerLogFunctions() {
    val mockPayload = Bundle()
    val mockVal = BigDecimal(1.0)
    val mockCurrency = Currency.getInstance(Locale.US)
    FacebookSdk.setAutoLogAppEventsEnabled(true)
    val internalLogger = InternalAppEventsLogger(logger)
    internalLogger.logEvent(mockEventName, null)
    Mockito.verify(logger, Mockito.times(1)).logEvent(mockEventName, null)
    internalLogger.logEvent(mockEventName, 1.0, mockPayload)
    Mockito.verify(logger, Mockito.times(1))
        .logEvent(
            ArgumentMatchers.eq(mockEventName),
            ArgumentMatchers.eq(1.0),
            ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logEventImplicitly(mockEventName)
    Mockito.verify(logger, Mockito.times(1)).logEventImplicitly(mockEventName, null, null)
    internalLogger.logEventImplicitly(mockEventName, mockPayload)
    Mockito.verify(logger, Mockito.times(1))
        .logEventImplicitly(
            ArgumentMatchers.eq(mockEventName),
            ArgumentMatchers.isNull(Double::class.java),
            ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logEventImplicitly(mockEventName, 1.0, mockPayload)
    Mockito.verify(logger, Mockito.times(1))
        .logEventImplicitly(
            ArgumentMatchers.eq(mockEventName),
            ArgumentMatchers.eq(1.0),
            ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logEventImplicitly(mockEventName, mockVal, mockCurrency, mockPayload)
    Mockito.verify(logger, Mockito.times(1))
        .logEventImplicitly(
            ArgumentMatchers.eq(mockEventName),
            ArgumentMatchers.eq(mockVal),
            ArgumentMatchers.eq(mockCurrency),
            ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
    internalLogger.logPurchaseImplicitly(mockVal, mockCurrency, mockPayload)
    Mockito.verify(logger, Mockito.times(1))
        .logPurchaseImplicitly(
            ArgumentMatchers.eq(mockVal),
            ArgumentMatchers.eq(mockCurrency),
            ArgumentMatchers.argThat(BundleMatcher(mockPayload)))
  }
}
