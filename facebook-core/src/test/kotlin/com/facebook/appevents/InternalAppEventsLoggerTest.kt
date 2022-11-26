/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.powermock.reflect.Whitebox
import org.robolectric.RuntimeEnvironment

class InternalAppEventsLoggerTest : FacebookPowerMockTestCase() {
  private val mockEventName = "fb_mock_event"
  private val serialExecutor: Executor = FacebookSerialExecutor()

  @Mock private lateinit var logger: AppEventsLoggerImpl
  @Before
  fun setupTest() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setClientToken("abcdefg")
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
