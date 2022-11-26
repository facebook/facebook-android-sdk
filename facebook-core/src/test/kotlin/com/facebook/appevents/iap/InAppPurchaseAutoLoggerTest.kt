/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    InAppPurchaseBillingClientWrapper::class,
    InAppPurchaseUtils::class,
    InAppPurchaseLoggerManager::class)
class InAppPurchaseAutoLoggerTest : FacebookPowerMockTestCase() {
  private lateinit var mockBillingClientWrapper: InAppPurchaseBillingClientWrapper
  private lateinit var mockContext: Context
  private val className = "com.facebook.appevents.iap.InAppPurchaseAutoLoggerTest"
  @Before
  fun init() {
    mockBillingClientWrapper = mock()
    mockContext = mock()
    PowerMockito.mockStatic(InAppPurchaseBillingClientWrapper::class.java)

    PowerMockito.mockStatic(InAppPurchaseLoggerManager::class.java)
    PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
    PowerMockito.doAnswer { Class.forName(className) }
        .`when`(InAppPurchaseUtils::class.java, "getClass", any())

    Whitebox.setInternalState(
        InAppPurchaseBillingClientWrapper::class.java, "initialized", AtomicBoolean(true))

    Whitebox.setInternalState(
        InAppPurchaseBillingClientWrapper::class.java, "instance", mockBillingClientWrapper)
  }

  @Test
  fun testStartIapLoggingNotCallLogPurchase() {
    var logPurchaseCallTimes = 0
    Whitebox.setInternalState(
        InAppPurchaseBillingClientWrapper::class.java, "isServiceConnected", AtomicBoolean(false))
    whenever(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
      logPurchaseCallTimes++
      Unit
    }
    assertThat(logPurchaseCallTimes).isEqualTo(0)
  }

  @Test
  fun testStartIapLoggingWhenEligibleQueryPurchaseHistory() {
    var logPurchaseCallTimes = 0
    var runnable: Runnable? = null
    Whitebox.setInternalState(
        InAppPurchaseBillingClientWrapper::class.java, "isServiceConnected", AtomicBoolean(true))
    whenever(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(true)
    whenever(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
      logPurchaseCallTimes++
      Unit
    }
    whenever(mockBillingClientWrapper.queryPurchaseHistory(any(), any())).thenAnswer {
      runnable = it.getArgument(1) as Runnable
      Unit
    }

    InAppPurchaseAutoLogger.startIapLogging(mockContext)
    assertThat(runnable).isNotNull
    runnable?.run()
    assertThat(logPurchaseCallTimes).isEqualTo(1)
  }

  @Test
  fun testStartIapLoggingWhenNotEligibleQueryPurchaseHistory() {
    var logPurchaseCallTimes = 0
    var runnable: Runnable? = null
    Whitebox.setInternalState(
        InAppPurchaseBillingClientWrapper::class.java, "isServiceConnected", AtomicBoolean(true))
    whenever(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(false)
    whenever(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
      logPurchaseCallTimes++
      Unit
    }
    whenever(mockBillingClientWrapper.queryPurchase(any(), any())).thenAnswer {
      runnable = it.getArgument(1) as Runnable
      Unit
    }

    InAppPurchaseAutoLogger.startIapLogging(mockContext)
    assertThat(runnable).isNotNull
    runnable?.run()
    assertThat(logPurchaseCallTimes).isEqualTo(1)
  }
}
