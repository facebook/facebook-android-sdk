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
import org.assertj.core.api.Assertions
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
    InAppPurchaseBillingClientWrapperV5Plus::class,
    InAppPurchaseUtils::class,
    InAppPurchaseLoggerManager::class
)
class InAppPurchaseAutoLoggerTest : FacebookPowerMockTestCase() {
    private lateinit var mockBillingClientWrapperV2_V4: InAppPurchaseBillingClientWrapper
    private lateinit var mockBillingClientWrapperV5Plus: InAppPurchaseBillingClientWrapperV5Plus
    private lateinit var mockContext: Context
    private val className = "com.facebook.appevents.iap.InAppPurchaseAutoLoggerTest"

    @Before
    fun init() {
        InAppPurchaseAutoLogger.failedToCreateWrapper.set(false)
        mockBillingClientWrapperV2_V4 = mock()
        mockBillingClientWrapperV5Plus = mock()
        mockContext = mock()
        PowerMockito.mockStatic(InAppPurchaseBillingClientWrapper::class.java)
        PowerMockito.mockStatic(InAppPurchaseBillingClientWrapperV5Plus::class.java)
        PowerMockito.mockStatic(InAppPurchaseLoggerManager::class.java)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        PowerMockito.doAnswer { Class.forName(className) }
            .`when`(InAppPurchaseUtils::class.java, "getClass", any())
    }

    @Test
    fun testFailureToCreateWrapper_V2_V4() {
        var queryCount = 0
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapper::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapper
        )
        PowerMockito.doAnswer { null }
            .`when`(InAppPurchaseUtils::class.java, "getClass", any())
        whenever(mockBillingClientWrapperV2_V4.queryPurchaseHistory(any(), any())).thenAnswer {
            queryCount++
            Unit
        }
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isTrue()
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        assertThat(queryCount).isEqualTo(0)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isTrue()
    }

    @Test
    fun testFailureToCreateWrapper_V5_Plus() {
        var queryCount = 0
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV5Plus::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV5Plus
        )
        PowerMockito.doAnswer { null }
            .`when`(InAppPurchaseUtils::class.java, "getClass", any())
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseHistoryAsync(
                any(),
                any()
            )
        ).thenAnswer {
            queryCount++
            Unit
        }
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_Plus
        )
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isTrue()
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_Plus
        )
        assertThat(queryCount).isEqualTo(0)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isTrue()
    }

    @Test
    fun testStartIapLoggingWhenEligibleQueryPurchaseHistory_V2_V4() {
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapper::class.java, "instance", mockBillingClientWrapperV2_V4
        )
        var logPurchaseCallTimes = 0
        var runnable: Runnable? = null
        whenever(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(true)
        whenever(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(mockBillingClientWrapperV2_V4.queryPurchaseHistory(any(), any())).thenAnswer {
            runnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        assertThat(runnable).isNotNull
        runnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testStartIapLoggingWhenNotEligibleQueryPurchaseHistory_V2_V4() {
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapper::class.java, "instance", mockBillingClientWrapperV2_V4
        )

        var logPurchaseCallTimes = 0
        var runnable: Runnable? = null
        whenever(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(false)
        whenever(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(mockBillingClientWrapperV2_V4.queryPurchase(any(), any())).thenAnswer {
            runnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        assertThat(runnable).isNotNull
        runnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testStartIapLoggingWhenEligibleQueryPurchaseHistory_V5_Plus() {
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV5Plus::class.java,
            "instance",
            mockBillingClientWrapperV5Plus
        )
        var logPurchaseCallTimes = 0
        var runnable: Runnable? = null
        whenever(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(true)
        whenever(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseHistoryAsync(
                any(),
                any()
            )
        ).thenAnswer {
            runnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_Plus
        )
        assertThat(runnable).isNotNull
        runnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }


    @Test
    fun testStartIapLoggingWhenNotEligibleQueryPurchaseHistory_V5_Plus() {
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV5Plus::class.java,
            "instance",
            mockBillingClientWrapperV5Plus
        )
        var logPurchaseCallTimes = 0
        var runnable: Runnable? = null
        whenever(InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory()).thenReturn(false)
        whenever(InAppPurchaseLoggerManager.filterPurchaseLogging(any(), any())).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchasesAsync(
                any(),
                any()
            )
        ).thenAnswer {
            runnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_Plus
        )
        assertThat(runnable).isNotNull
        runnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }
}
