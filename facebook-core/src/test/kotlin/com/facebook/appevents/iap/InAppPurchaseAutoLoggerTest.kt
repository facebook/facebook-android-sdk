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

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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
    PowerMockito.`when`(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
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
    PowerMockito.`when`(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(true)
    PowerMockito.`when`(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
      logPurchaseCallTimes++
      Unit
    }
    PowerMockito.`when`(mockBillingClientWrapper.queryPurchaseHistory(any(), any())).thenAnswer {
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
    PowerMockito.`when`(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(false)
    PowerMockito.`when`(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
      logPurchaseCallTimes++
      Unit
    }
    PowerMockito.`when`(mockBillingClientWrapper.queryPurchase(any(), any())).thenAnswer {
      runnable = it.getArgument(1) as Runnable
      Unit
    }

    InAppPurchaseAutoLogger.startIapLogging(mockContext)
    assertThat(runnable).isNotNull
    runnable?.run()
    assertThat(logPurchaseCallTimes).isEqualTo(1)
  }
}
