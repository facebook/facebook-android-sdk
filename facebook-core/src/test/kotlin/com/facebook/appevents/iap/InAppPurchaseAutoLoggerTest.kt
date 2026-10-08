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
import com.facebook.appevents.integrity.ProtectedModeManager
import com.facebook.internal.FeatureManager
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    InAppPurchaseBillingClientWrapperV2V4::class,
    InAppPurchaseBillingClientWrapperV5V7::class,
    InAppPurchaseBillingClientWrapperV8Plus::class,
    InAppPurchaseManager::class,
    InAppPurchaseUtils::class,
    InAppPurchaseLoggerManager::class,
    FeatureManager::class
)
class InAppPurchaseAutoLoggerTest : FacebookPowerMockTestCase() {
    private lateinit var mockBillingClientWrapperV2_V4: InAppPurchaseBillingClientWrapperV2V4
    private lateinit var mockBillingClientWrapperV5Plus: InAppPurchaseBillingClientWrapperV5V7
    private lateinit var mockBillingClientWrapperV8Plus: InAppPurchaseBillingClientWrapperV8Plus
    private lateinit var mockV8QueryTimeoutExecutor: ScheduledExecutorService
    private lateinit var mockV8QueryTimeoutFuture: ScheduledFuture<*>
    private val v8QueryTimeoutCallbacks = mutableListOf<Runnable>()
    private lateinit var mockContext: Context
    private val className = "com.facebook.appevents.iap.InAppPurchaseAutoLoggerTest"
    private val packageName = "examplePackageName"

    @Before
    fun init() {
        InAppPurchaseAutoLogger.failedToCreateWrapper.set(false)
        InAppPurchaseAutoLogger.resetV8QueryStateForTesting()
        v8QueryTimeoutCallbacks.clear()
        mockV8QueryTimeoutExecutor = mock()
        mockV8QueryTimeoutFuture = mock()
        whenever(
                mockV8QueryTimeoutExecutor.schedule(
                    any<Runnable>(),
                    eq(InAppPurchaseAutoLogger.V8_QUERY_TIMEOUT_MILLISECONDS),
                    eq(TimeUnit.MILLISECONDS),
                )
            )
            .thenAnswer {
                v8QueryTimeoutCallbacks.add(it.getArgument(0))
                mockV8QueryTimeoutFuture
            }
        InAppPurchaseAutoLogger.v8QueryTimeoutExecutor = mockV8QueryTimeoutExecutor
        InAppPurchaseBillingClientWrapperV8Plus.purchasesUpdatedHandler = null
        InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV8Plus.subsPurchaseDetailsMap.clear()
        InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap.clear()
        ProtectedModeManager.disable()
        mockBillingClientWrapperV2_V4 = mock()
        mockBillingClientWrapperV5Plus = mock()
        mockBillingClientWrapperV8Plus = mock()
        mockContext = mock()
        whenever(mockContext.packageName).thenReturn(packageName)
        PowerMockito.mockStatic(InAppPurchaseBillingClientWrapperV2V4::class.java)
        PowerMockito.mockStatic(InAppPurchaseBillingClientWrapperV5V7::class.java)
        PowerMockito.mockStatic(InAppPurchaseBillingClientWrapperV8Plus::class.java)
        PowerMockito.mockStatic(InAppPurchaseManager::class.java)
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
                any(),
                any(),
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            emptySet<String>()
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
                any(),
                any(),
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            emptySet<String>()
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
                any(),
                any(),
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            emptySet<String>()
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

        var queryOrderIdsCount = 0
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseOrderIds(any(), any())
        ).thenAnswer {
            queryOrderIdsCount++
            (it.getArgument(1) as Runnable).run()
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
        assertThat(queryOrderIdsCount).isEqualTo(2)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testStartIapLoggingWithQuerySubsEnabledButProtectedModeOnV5_V7() {
        ProtectedModeManager.enable()
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
                any(),
                any(),
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            emptySet<String>()
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

        var queryOrderIdsCount = 0
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseOrderIds(any(), any())
        ).thenAnswer {
            queryOrderIdsCount++
            (it.getArgument(1) as Runnable).run()
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )
        assertThat(querySubsRunnable).isNull()
        assertThat(loggingRunnable).isNotNull
        loggingRunnable?.run()
        assertThat(logPurchaseCallTimes).isEqualTo(2)
        assertThat(queryPurchaseCount).isEqualTo(1)
        assertThat(querySubCount).isEqualTo(0)
        assertThat(queryOrderIdsCount).isEqualTo(1)
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
                any(),
                any(),
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            emptySet<String>()
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

        var queryOrderIdsCount = 0
        whenever(
            mockBillingClientWrapperV5Plus.queryPurchaseOrderIds(any(), any())
        ).thenAnswer {
            queryOrderIdsCount++
            (it.getArgument(1) as Runnable).run()
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
        assertThat(queryOrderIdsCount).isEqualTo(1)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testStartIapLoggingUsesCurrentPurchasesForV8Plus() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging)).thenReturn(
            true
        )
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV8Plus::class.java,
            "instance",
            mockBillingClientWrapperV8Plus
        )
        var logPurchaseCallTimes = 0
        var queryInAppCount = 0
        var querySubsCount = 0
        var querySubsCallback:
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback? = null
        var loggingCallback:
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback? = null
        whenever(
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        ).thenAnswer {
            logPurchaseCallTimes++
            emptySet<String>()
        }
        whenever(
            mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                eq(InAppPurchaseUtils.IAPProductType.INAPP),
                any()
            )
        ).thenAnswer {
            queryInAppCount++
            querySubsCallback =
                it.getArgument(1)
                    as InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback
            Unit
        }
        whenever(
            mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                eq(InAppPurchaseUtils.IAPProductType.SUBS),
                any()
            )
        ).thenAnswer {
            querySubsCount++
            loggingCallback =
                it.getArgument(1)
                    as InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback
            Unit
        }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS
        )
        querySubsCallback?.onComplete(v8QueryResult(InAppPurchaseUtils.IAPProductType.INAPP))
        loggingCallback?.onComplete(v8QueryResult(InAppPurchaseUtils.IAPProductType.SUBS))

        assertThat(queryInAppCount).isEqualTo(1)
        assertThat(querySubsCount).isEqualTo(1)
        assertThat(logPurchaseCallTimes).isEqualTo(2)
        assertThat(InAppPurchaseAutoLogger.failedToCreateWrapper.get()).isFalse()
    }

    @Test
    fun testConcurrentV8RefreshesAreCoalescedAndReplayed() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging))
            .thenReturn(false)
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV8Plus::class.java,
            "instance",
            mockBillingClientWrapperV8Plus,
        )
        var queryCount = 0
        var queryCallback:
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback? = null
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.INAPP),
                    any(),
                )
            )
            .thenAnswer {
                queryCount++
                queryCallback =
                    it.getArgument(1)
                        as InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback
                Unit
            }
        whenever(
                InAppPurchaseLoggerManager.filterPurchaseLogging(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(emptySet())

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS,
        )
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS,
        )

        assertThat(queryCount).isEqualTo(1)

        queryCallback?.onComplete(v8QueryResult(InAppPurchaseUtils.IAPProductType.INAPP))

        assertThat(queryCount).isEqualTo(2)
    }

    @Test
    fun testRealtimeUpdateRoutesThroughGatedManager() {
        var startTrackingCallTimes = 0
        PowerMockito.doAnswer {
              startTrackingCallTimes++
              null
            }
            .`when`(InAppPurchaseManager::class.java, "startTracking")
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging))
            .thenReturn(true)
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV8Plus::class.java,
            "instance",
            mockBillingClientWrapperV8Plus,
        )
        val inAppCallbacks =
            mutableListOf<
                InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback>()
        val subscriptionCallbacks =
            mutableListOf<
                InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback>()
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.INAPP),
                    any(),
                )
            )
            .thenAnswer {
                inAppCallbacks.add(it.getArgument(1))
                Unit
            }
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.SUBS),
                    any(),
                )
            )
            .thenAnswer {
                subscriptionCallbacks.add(it.getArgument(1))
                Unit
            }
        whenever(
                InAppPurchaseLoggerManager.filterPurchaseLogging(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(emptySet())

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS,
        )
        inAppCallbacks[0].onComplete(v8QueryResult(InAppPurchaseUtils.IAPProductType.INAPP))
        subscriptionCallbacks[0].onComplete(v8QueryResult(InAppPurchaseUtils.IAPProductType.SUBS))

        InAppPurchaseBillingClientWrapperV8Plus.purchasesUpdatedHandler?.invoke(
            0,
            listOf(JSONObject("{\"productId\":\"subscription\"}")),
        )
        assertThat(startTrackingCallTimes).isEqualTo(1)
        assertThat(inAppCallbacks).hasSize(1)
        assertThat(subscriptionCallbacks).hasSize(1)
    }

    @Test
    fun testSubscriptionStartFailureRetainsInAppResultWithoutLoggingPartialScan() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging))
            .thenReturn(true)
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV8Plus::class.java,
            "instance",
            mockBillingClientWrapperV8Plus,
        )
        var inAppCallback:
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback? = null
        var logPurchaseCallTimes = 0
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.INAPP),
                    any(),
                )
            )
            .thenAnswer {
                inAppCallback = it.getArgument(1)
                Unit
            }
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.SUBS),
                    any(),
                )
            )
            .thenThrow(IllegalStateException("subscription query failed to start"))
        whenever(
                InAppPurchaseLoggerManager.filterPurchaseLogging(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenAnswer {
                logPurchaseCallTimes++
                emptySet<String>()
            }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS,
        )
        inAppCallback?.onComplete(
            v8QueryResult(InAppPurchaseUtils.IAPProductType.INAPP, "retained")
        )

        assertThat(InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap)
            .containsKey("retained")
        assertThat(logPurchaseCallTimes).isEqualTo(0)
        assertThat(InAppPurchaseAutoLogger.isV8QueryInProgress.get()).isFalse()
    }

    @Test
    fun testSubscriptionTimeoutRetainsInAppResultAndRejectsLateSubscriptionResult() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging))
            .thenReturn(true)
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV8Plus::class.java,
            "instance",
            mockBillingClientWrapperV8Plus,
        )
        var inAppCallback:
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback? = null
        var subscriptionCallback:
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback? = null
        var logPurchaseCallTimes = 0
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.INAPP),
                    any(),
                )
            )
            .thenAnswer {
                inAppCallback = it.getArgument(1)
                Unit
            }
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.SUBS),
                    any(),
                )
            )
            .thenAnswer {
                subscriptionCallback = it.getArgument(1)
                Unit
            }
        whenever(
                InAppPurchaseLoggerManager.filterPurchaseLogging(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenAnswer {
                logPurchaseCallTimes++
                emptySet<String>()
            }

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS,
        )
        inAppCallback?.onComplete(
            v8QueryResult(InAppPurchaseUtils.IAPProductType.INAPP, "retained")
        )
        v8QueryTimeoutCallbacks[0].run()

        assertThat(InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap)
            .containsKey("retained")
        assertThat(logPurchaseCallTimes).isEqualTo(0)
        assertThat(InAppPurchaseAutoLogger.isV8QueryInProgress.get()).isFalse()

        subscriptionCallback?.onComplete(
            v8QueryResult(InAppPurchaseUtils.IAPProductType.SUBS, "stale-subscription")
        )
        assertThat(InAppPurchaseBillingClientWrapperV8Plus.subsPurchaseDetailsMap)
            .doesNotContainKey("stale-subscription")
    }

    @Test
    fun testTimedOutV8QueryReleasesLatchAndIgnoresLateCallback() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging))
            .thenReturn(false)
        Whitebox.setInternalState(
            InAppPurchaseBillingClientWrapperV8Plus::class.java,
            "instance",
            mockBillingClientWrapperV8Plus,
        )
        val queryCallbacks =
            mutableListOf<
                InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResultCallback>()
        whenever(
                mockBillingClientWrapperV8Plus.queryPurchasesWithResult(
                    eq(InAppPurchaseUtils.IAPProductType.INAPP),
                    any(),
                )
            )
            .thenAnswer {
                queryCallbacks.add(it.getArgument(1))
                Unit
            }
        whenever(
                InAppPurchaseLoggerManager.filterPurchaseLogging(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(emptySet())

        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS,
        )
        InAppPurchaseAutoLogger.startIapLogging(
            mockContext,
            InAppPurchaseUtils.BillingClientVersion.V8_PLUS,
        )
        v8QueryTimeoutCallbacks[0].run()

        assertThat(queryCallbacks).hasSize(2)
        assertThat(InAppPurchaseAutoLogger.isV8QueryInProgress.get()).isTrue()

        queryCallbacks[0].onComplete(
            v8QueryResult(InAppPurchaseUtils.IAPProductType.INAPP, "stale")
        )
        assertThat(InAppPurchaseAutoLogger.isV8QueryInProgress.get()).isTrue()
        assertThat(InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap)
            .doesNotContainKey("stale")

        queryCallbacks[1].onComplete(
            v8QueryResult(InAppPurchaseUtils.IAPProductType.INAPP, "current")
        )
        assertThat(InAppPurchaseAutoLogger.isV8QueryInProgress.get()).isFalse()
        assertThat(InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap)
            .containsKey("current")
    }

    private fun v8QueryResult(
        productType: InAppPurchaseUtils.IAPProductType,
        productId: String? = null,
    ): InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResult {
        val purchaseDetails =
            if (productId == null) {
                emptyMap()
            } else {
                mapOf(productId to JSONObject().put("productId", productId))
            }
        return InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResult(
            productType,
            true,
            purchaseDetails,
        )
    }
}
