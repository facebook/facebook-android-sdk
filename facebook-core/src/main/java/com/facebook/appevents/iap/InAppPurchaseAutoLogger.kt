/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V2_V4
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V5_V7
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V8_PLUS
import com.facebook.appevents.iap.InAppPurchaseUtils.IAPProductType.INAPP
import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.appevents.iap.InAppPurchaseLoggerManager.getIsFirstAppLaunchWithNewIAP
import com.facebook.appevents.iap.InAppPurchaseLoggerManager.migrateOldCacheHistory
import com.facebook.appevents.iap.InAppPurchaseLoggerManager.setAppHasBeenLaunchedWithNewIAP
import com.facebook.appevents.iap.InAppPurchaseUtils.IAPProductType.SUBS
import com.facebook.appevents.integrity.ProtectedModeManager
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseAutoLogger {
    private const val NO_ACTIVE_V8_QUERY = 0L

    val failedToCreateWrapper = AtomicBoolean(false)
    internal val isV8QueryInProgress = AtomicBoolean(false)
    internal val isV8QueryPending = AtomicBoolean(false)
    private val nextV8QueryGeneration = AtomicLong(NO_ACTIVE_V8_QUERY)
    private val activeV8QueryGeneration = AtomicLong(NO_ACTIVE_V8_QUERY)

    @VisibleForTesting
    internal var v8QueryTimeoutExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    @Volatile private var v8QueryTimeoutFuture: ScheduledFuture<*>? = null

    @Synchronized
    @JvmStatic
    fun startIapLogging(
        context: Context,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion
    ) {
        // Check if we have previously tried and failed to create a billing client wrapper
        if (failedToCreateWrapper.get()) {
            return
        }
        var billingClientWrapper: InAppPurchaseBillingClientWrapper? = null
        if (billingClientVersion == V2_V4) {
            billingClientWrapper =
                InAppPurchaseBillingClientWrapperV2V4.getOrCreateInstance(context)
        } else if (billingClientVersion == V5_V7) {
            billingClientWrapper =
                InAppPurchaseBillingClientWrapperV5V7.getOrCreateInstance(context)
        } else if (billingClientVersion == V8_PLUS) {
            billingClientWrapper =
                InAppPurchaseBillingClientWrapperV8Plus.getOrCreateInstance(context)
        }
        if (billingClientWrapper == null) {
            failedToCreateWrapper.set(true)
            return
        }

        val shouldQuerySubscriptions =
            isEnabled(FeatureManager.Feature.AndroidIAPSubscriptionAutoLogging) &&
                (!ProtectedModeManager.isEnabled() || billingClientVersion == V2_V4)
        if (billingClientVersion == V8_PLUS) {
            startV8IapLogging(
                context,
                billingClientWrapper as InAppPurchaseBillingClientWrapperV8Plus,
                shouldQuerySubscriptions,
            )
        } else {
            if (shouldQuerySubscriptions) {
                queryPurchaseOrderIds(billingClientWrapper, INAPP) {
                    billingClientWrapper.queryPurchaseHistory(INAPP) {
                        queryPurchaseOrderIds(billingClientWrapper, SUBS) {
                            billingClientWrapper.queryPurchaseHistory(SUBS) {
                                logPurchase(billingClientVersion, context.packageName)
                            }
                        }
                    }
                }
            } else {
                queryPurchaseOrderIds(billingClientWrapper, INAPP) {
                    billingClientWrapper.queryPurchaseHistory(INAPP) {
                        logPurchase(billingClientVersion, context.packageName)
                    }
                }
            }
        }
    }

    // Purchase history records from GPBL v5 - v7 don't include the order ID,
    // so look it up from the currently owned purchases before querying history
    private fun queryPurchaseOrderIds(
        billingClientWrapper: InAppPurchaseBillingClientWrapper,
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: Runnable
    ) {
        if (billingClientWrapper is InAppPurchaseBillingClientWrapperV5V7) {
            billingClientWrapper.queryPurchaseOrderIds(productType, completionHandler)
        } else {
            completionHandler.run()
        }
    }

    private fun startV8IapLogging(
        context: Context,
        billingClientWrapper: InAppPurchaseBillingClientWrapperV8Plus,
        shouldQuerySubscriptions: Boolean,
    ) {
        registerV8PurchasesUpdatedHandler()
        if (!isV8QueryInProgress.compareAndSet(false, true)) {
            isV8QueryPending.set(true)
            return
        }

        val generation = nextV8QueryGeneration.incrementAndGet()
        activeV8QueryGeneration.set(generation)
        val queryState = V8QueryState()
        try {
            v8QueryTimeoutFuture =
                v8QueryTimeoutExecutor.schedule(
                    {
                        finishV8Query(
                            context,
                            generation,
                            queryState,
                            timedOut = true,
                        )
                    },
                    V8_QUERY_TIMEOUT_MILLISECONDS,
                    TimeUnit.MILLISECONDS,
                )
            billingClientWrapper.queryPurchasesWithResult(
                INAPP,
                inAppQueryCallback@{ inAppQueryResult ->
                    if (!queryState.retainInAppResult(generation, inAppQueryResult)) {
                        Log.w(TAG, "Ignoring a late GPBL 8 in-app purchase query result")
                        return@inAppQueryCallback
                    }
                    if (shouldQuerySubscriptions) {
                        try {
                            billingClientWrapper.queryPurchasesWithResult(SUBS) { subsQueryResult ->
                                finishV8Query(
                                    context,
                                    generation,
                                    queryState,
                                    listOf(inAppQueryResult, subsQueryResult),
                                )
                            }
                        } catch (exception: Exception) {
                            Log.w(TAG, "Failed to start GPBL 8 subscription query", exception)
                            finishV8Query(context, generation, queryState)
                        }
                    } else {
                        finishV8Query(
                            context,
                            generation,
                            queryState,
                            listOf(inAppQueryResult),
                        )
                    }
                },
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to start GPBL 8 purchase query", exception)
            finishV8Query(context, generation, queryState)
        }
    }

    private fun finishV8Query(
        context: Context,
        generation: Long,
        queryState: V8QueryState,
        queryResults: List<InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResult> =
            emptyList(),
        timedOut: Boolean = false,
    ) {
        val retainedInAppResults = queryState.finishIfActive(generation)
        if (retainedInAppResults == null) {
            if (!timedOut) {
                Log.w(TAG, "Ignoring a late GPBL 8 query result")
            }
            return
        }
        v8QueryTimeoutFuture?.cancel(false)
        v8QueryTimeoutFuture = null
        if (timedOut) {
            Log.w(TAG, "GPBL 8 purchase query timed out")
        }
        try {
            // AdsSDK retains a completed INAPP result even when the following SUBS query does not
            // complete. Keep the same retryable data without logging an incomplete scan.
            val resultsToPublish =
                if (queryResults.isNotEmpty()) queryResults else retainedInAppResults
            resultsToPublish.forEach(InAppPurchaseBillingClientWrapperV8Plus::publishQueryResult)
            val completedFullScan =
                queryResults.isNotEmpty() && queryResults.all { it.succeeded }
            if (completedFullScan) {
                logPurchase(V8_PLUS, context.packageName, true, true)
            }
        } finally {
            activeV8QueryGeneration.set(NO_ACTIVE_V8_QUERY)
            isV8QueryInProgress.set(false)
            if (isV8QueryPending.getAndSet(false)) {
                startIapLogging(context, V8_PLUS)
            }
        }
    }

    private fun registerV8PurchasesUpdatedHandler() {
        InAppPurchaseBillingClientWrapperV8Plus.purchasesUpdatedHandler =
            purchasesUpdatedHandler@{ responseCode, purchases ->
                if (responseCode != BILLING_RESPONSE_OK || purchases.isEmpty()) {
                    return@purchasesUpdatedHandler
                }
                // The listener is not typed, so rescan both gated product types instead of
                // classifying subscriptions as one-time products.
                InAppPurchaseManager.startTracking()
            }
    }

    @VisibleForTesting
    internal fun resetV8QueryStateForTesting() {
        v8QueryTimeoutFuture?.cancel(false)
        v8QueryTimeoutFuture = null
        nextV8QueryGeneration.set(NO_ACTIVE_V8_QUERY)
        activeV8QueryGeneration.set(NO_ACTIVE_V8_QUERY)
        isV8QueryInProgress.set(false)
        isV8QueryPending.set(false)
    }

    @Synchronized
    private fun logPurchase(
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion,
        packageName: String,
        querySucceeded: Boolean = true,
        isFullScan: Boolean = true,
    ) {
        val isFirstAppLaunch = isFullScan && getIsFirstAppLaunchWithNewIAP()
        if (isFirstAppLaunch) {
            migrateOldCacheHistory()
        }
        if (billingClientVersion == V2_V4) {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap,
                false,
                packageName,
                billingClientVersion,
                isFirstAppLaunch
            )
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV2V4.skuDetailsMap,
                true,
                packageName,
                billingClientVersion,
                isFirstAppLaunch,
            )
            InAppPurchaseBillingClientWrapperV2V4.iapPurchaseDetailsMap.clear()
            InAppPurchaseBillingClientWrapperV2V4.subsPurchaseDetailsMap.clear()
        } else if (billingClientVersion == V5_V7) {
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5V7.productDetailsMap,
                false,
                packageName,
                billingClientVersion,
                isFirstAppLaunch,
            )
            InAppPurchaseLoggerManager.filterPurchaseLogging(
                InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap,
                InAppPurchaseBillingClientWrapperV5V7.productDetailsMap,
                true,
                packageName,
                billingClientVersion,
                isFirstAppLaunch,
            )
            InAppPurchaseBillingClientWrapperV5V7.iapPurchaseDetailsMap.clear()
            InAppPurchaseBillingClientWrapperV5V7.subsPurchaseDetailsMap.clear()
            InAppPurchaseBillingClientWrapperV5V7.purchaseTokenToOrderIdMap.clear()
        } else if (billingClientVersion == V8_PLUS) {
            val completedIapProductIds =
                InAppPurchaseLoggerManager.filterPurchaseLogging(
                    InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap,
                    InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap,
                    false,
                    packageName,
                    billingClientVersion,
                    isFirstAppLaunch,
                )
            val completedSubscriptionProductIds =
                InAppPurchaseLoggerManager.filterPurchaseLogging(
                    InAppPurchaseBillingClientWrapperV8Plus.subsPurchaseDetailsMap,
                    InAppPurchaseBillingClientWrapperV8Plus.productDetailsMap,
                    true,
                    packageName,
                    billingClientVersion,
                    isFirstAppLaunch,
                )
            completedIapProductIds.forEach(
                InAppPurchaseBillingClientWrapperV8Plus.iapPurchaseDetailsMap::remove
            )
            completedSubscriptionProductIds.forEach(
                InAppPurchaseBillingClientWrapperV8Plus.subsPurchaseDetailsMap::remove
            )
        }
        if (isFirstAppLaunch && querySucceeded) {
            setAppHasBeenLaunchedWithNewIAP()
        }
    }

    private val TAG = InAppPurchaseAutoLogger::class.java.canonicalName
    private const val BILLING_RESPONSE_OK = 0
    private const val FINISHING_V8_QUERY = -1L

    @VisibleForTesting internal const val V8_QUERY_TIMEOUT_MILLISECONDS = 30_000L

    private class V8QueryState {
        private var isFinished = false
        private var inAppQueryResult:
            InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResult? = null

        @Synchronized
        fun retainInAppResult(
            generation: Long,
            result: InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResult,
        ): Boolean {
            if (isFinished || InAppPurchaseAutoLogger.activeV8QueryGeneration.get() != generation) {
                return false
            }
            inAppQueryResult = result
            return true
        }

        @Synchronized
        fun finishIfActive(
            generation: Long
        ): List<InAppPurchaseBillingClientWrapperV8Plus.PurchaseQueryResult>? {
            if (
                isFinished ||
                    !InAppPurchaseAutoLogger.activeV8QueryGeneration.compareAndSet(
                        generation,
                        FINISHING_V8_QUERY,
                    )
            ) {
                return null
            }
            isFinished = true
            return listOfNotNull(inAppQueryResult)
        }
    }
}
