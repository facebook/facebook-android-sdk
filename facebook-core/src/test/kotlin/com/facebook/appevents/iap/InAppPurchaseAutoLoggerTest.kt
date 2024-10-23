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
import com.facebook.internal.FeatureManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    InAppPurchaseBillingClientWrapperV2V4::class,
    InAppPurchaseBillingClientWrapperV5V7::class,
    InAppPurchaseUtils::class,
    InAppPurchaseLoggerManager::class,
    FeatureManager::class
)
class InAppPurchaseAutoLoggerTest : FacebookPowerMockTestCase() {
    private lateinit var mockBillingClientWrapperV2_V4: InAppPurchaseBillingClientWrapperV2V4
    private lateinit var mockBillingClientWrapperV5Plus: InAppPurchaseBillingClientWrapperV5V7
    private lateinit var mockContext: Context
    private val className = "com.facebook.appevents.iap.InAppPurchaseAutoLoggerTest"
    private val packageName = "examplePackageName"

    @Before
    fun init() {
        InAppPurchaseAutoLogger.failedToCreateWrapper.set(false)
        mockBillingClientWrapperV2_V4 = mock()
        mockBillingClientWrapperV5Plus = mock()
        mockContext = mock()
        whenever(mockContext.packageName).thenReturn(packageName)
        PowerMockito.mockStatic(InAppPurchaseBillingClientWrapperV2V4::class.java)
        PowerMockito.mockStatic(InAppPurchaseBillingClientWrapperV5V7::class.java)
        PowerMockito.mockStatic(InAppPurchaseLoggerManager::class.java)
        PowerMockito.mockStatic(InAppPurchaseUtils::class.java)
        PowerMockito.mockStatic(FeatureManager::class.java)
        PowerMockito.doAnswer { Class.forName(className) }
            .`when`(InAppPurchaseUtils::class.java, "getClass", any())
    }

    @Test
    fun testFailureToCreateWrapper_V2_V4() {
        var queryCount = 0
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV2V4
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
            InAppPurchaseBillingClientWrapperV5V7::class.java,
            "instance",
            null as? InAppPurchaseBillingClientWrapperV5V7
        )
        PowerMockito.doAnswer { null }
            .`when`(InAppPurchaseUtils::class.java, "getClass", any())
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseHistory(
                any(),
                any()
            )
        ).thenAnswer {
            queryCount++
            Unit
        }
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isTrue()
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )
        assertThat(queryCount).isEqualTo(0)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isTrue()
    }

    @Test
    fun testStartIapLoggingWithQuerySubsEnabledV2_V4() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging)).thenReturn(
            true
        )
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            mockBillingClientWrapperV2_V4
        )
        var logPurchaseCallTimes = 0
        var queryPurchaseCount = 0
        var querySubCount = 0
        var loggingRunnable: Runnable? = null
        var querySubsRunnable: Runnable? = null
        whenever(
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(
            mockBillingClientWrapperV2_V4.queryPurchaseHistory(
                eq(InAppPurchaseUtils.IAPProductType.INAPP),
                any()
            )
        ).thenAnswer {
            queryPurchaseCount++
            querySubsRunnable = it.getArgument(1) as Runnable
            Unit
        }
        whenever(
            mockBillingClientWrapperV2_V4.queryPurchaseHistory(
                eq(InAppPurchaseUtils.IAPProductType.SUBS),
                any()
            )
        ).thenAnswer {
            querySubCount++
            loggingRunnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        assertThat(querySubsRunnable).isNotNull
        querySubsRunnable?.run()
        assertThat(loggingRunnable).isNotNull
        loggingRunnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(2)
        assertThat(queryPurchaseCount).isEqualTo(1)
        assertThat(querySubCount).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testStartIapLoggingWithQuerySubsDisabledV2_V4() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging)).thenReturn(
            false
        )
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV2V4::class.java,
            "instance",
            mockBillingClientWrapperV2_V4
        )
        var logPurchaseCallTimes = 0
        var queryPurchaseCount = 0
        var querySubCount = 0
        var loggingRunnable: Runnable? = null
        var querySubsRunnable: Runnable? = null
        whenever(
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(
            mockBillingClientWrapperV2_V4.queryPurchaseHistory(
                eq(InAppPurchaseUtils.IAPProductType.INAPP),
                any()
            )
        ).thenAnswer {
            queryPurchaseCount++
            loggingRunnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        assertThat(loggingRunnable).isNotNull
        loggingRunnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(2)
        assertThat(queryPurchaseCount).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testStartIapLoggingWithQuerySubsEnabledV5_V7() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging)).thenReturn(
            true
        )
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV5V7::class.java,
            "instance",
            mockBillingClientWrapperV5Plus
        )
        var logPurchaseCallTimes = 0
        var queryPurchaseCount = 0
        var querySubCount = 0
        var loggingRunnable: Runnable? = null
        var querySubsRunnable: Runnable? = null
        whenever(
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseHistory(
                eq(InAppPurchaseUtils.IAPProductType.INAPP),
                any()
            )
        ).thenAnswer {
            queryPurchaseCount++
            querySubsRunnable = it.getArgument(1) as Runnable
            Unit
        }
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseHistory(
                eq(InAppPurchaseUtils.IAPProductType.SUBS),
                any()
            )
        ).thenAnswer {
            querySubCount++
            loggingRunnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )
        assertThat(querySubsRunnable).isNotNull
        querySubsRunnable?.run()
        assertThat(loggingRunnable).isNotNull
        loggingRunnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(2)
        assertThat(queryPurchaseCount).isEqualTo(1)
        assertThat(querySubCount).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testStartIapLoggingWithQuerySubsDisabledV5_V7() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging)).thenReturn(
            false
        )
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV5V7::class.java,
            "instance",
            mockBillingClientWrapperV5Plus
        )
        var logPurchaseCallTimes = 0
        var queryPurchaseCount = 0
        var querySubCount = 0
        var loggingRunnable: Runnable? = null
        var querySubsRunnable: Runnable? = null
        whenever(
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            Unit
        }
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseHistory(
                eq(InAppPurchaseUtils.IAPProductType.INAPP),
                any()
            )
        ).thenAnswer {
            queryPurchaseCount++
            loggingRunnable = it.getArgument(1) as Runnable
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )
        assertThat(loggingRunnable).isNotNull
        loggingRunnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(2)
        assertThat(queryPurchaseCount).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }
}
