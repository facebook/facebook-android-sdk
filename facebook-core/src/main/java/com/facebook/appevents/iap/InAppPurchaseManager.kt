/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.NONE
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V1
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V2_V4
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V5_V7
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.internal.Constants
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseManager {
    private val timesOfManualPurchases =
        ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Bundle>>>()
    private val timesOfImplicitPurchases =
        ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Bundle>>>()
    private var specificBillingLibraryVersion: String? = null;
    private const val GOOGLE_BILLINGCLIENT_VERSION = "com.google.android.play.billingclient.version"
    private val enabled = AtomicBoolean(false)

    @JvmStatic
    fun enableAutoLogging() {
        enabled.set(true)
        startTracking()
    }

    @JvmStatic
    fun startTracking() {
        if (!enabled.get()) {
            return
        }
        // Delegate IAP logic to separate handler based on Google Play Billing Library version
        when (val billingClientVersion = getBillingClientVersion()) {
            NONE -> return
            V1 -> InAppPurchaseActivityLifecycleTracker.startIapLogging(V1)
            V2_V4 -> {
                if (isEnabled(FeatureManager.Feature.IapLoggingLib2)) {
                    InAppPurchaseAutoLogger.startIapLogging(
                        getApplicationContext(),
                        billingClientVersion
                    )
                } else {
                    InAppPurchaseActivityLifecycleTracker.startIapLogging(V2_V4)
                }
            }

            V5_V7 -> {
                if (isEnabled(FeatureManager.Feature.IapLoggingLib5To7)) {
                    InAppPurchaseAutoLogger.startIapLogging(
                        getApplicationContext(),
                        billingClientVersion
                    )
                }
            }
        }
    }

    @JvmStatic
    private fun setSpecificBillingLibraryVersion(version: String) {
        specificBillingLibraryVersion = version
    }

    @JvmStatic
    fun getSpecificBillingLibraryVersion(): String? {
        return specificBillingLibraryVersion
    }

    private fun getBillingClientVersion(): InAppPurchaseUtils.BillingClientVersion {
        try {
            val context = getApplicationContext()
            val info =
                context.packageManager.getApplicationInfo(
                    context.packageName, PackageManager.GET_META_DATA
                )
            // If we can't find the package, the billing client wrapper will not be able
            // to fetch any of the necessary methods/classes.
            val version = info.metaData.getString(GOOGLE_BILLINGCLIENT_VERSION)
                ?: return NONE
            val versionArray = version.split(
                ".",
                limit = 3
            )
            if (version.isEmpty()) {
                // Default to newest version
                return V5_V7
            }
            setSpecificBillingLibraryVersion("GPBL.$version")
            val majorVersion =
                versionArray[0].toIntOrNull() ?: return V5_V7
            return if (majorVersion == 1) {
                V1
            } else if (majorVersion < 5) {
                V2_V4
            } else {
                V5_V7
            }
        } catch (e: Exception) {
            // Default to newest version
            return V5_V7
        }
    }

    @Synchronized
    @JvmStatic
    fun isDuplicate(
        purchase: InAppPurchase,
        time: Long,
        isImplicitlyLogged: Boolean,
        newPurchaseParameters: Bundle?
    ): Boolean {
        if (newPurchaseParameters == null) {
            return false
        }
        val dedupeCandidates: MutableList<Pair<Long, Bundle>>?

        // Round to two decimal places
        val roundedPurchase = InAppPurchase(
            purchase.eventName,
            purchase.amount.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble(),
            purchase.currency
        )
        if (isImplicitlyLogged) {
            dedupeCandidates = timesOfManualPurchases[roundedPurchase]
        } else {
            dedupeCandidates = timesOfImplicitPurchases[roundedPurchase]
        }

        // We should dedupe with the oldest one in the time window to allow for as many valid dedupes as possible
        var indexOfOldestValidDedupe: Int? = null
        var oldestValidTime: Long? = null
        if (!dedupeCandidates.isNullOrEmpty()) {
            for ((index, timeBundlePair) in dedupeCandidates.withIndex()) {
                val candidateTime = timeBundlePair.first
                val candidateParameters = timeBundlePair.second
                if (abs(time - candidateTime) > InAppPurchaseDedupeConfig.dedupeWindow) {
                    continue
                }
                if ((oldestValidTime == null || candidateTime < oldestValidTime)
                    && atLeastOneEquivalentDedupeParameter(
                        newPurchaseParameters,
                        candidateParameters,
                        !isImplicitlyLogged
                    )
                ) {
                    oldestValidTime = candidateTime
                    indexOfOldestValidDedupe = index
                }
            }
        }

        // If we have a valid dedupe candidate, we should remove it from the list in memory
        // so it doesn't dedupe multiple purchase events. Likewise, we should not add the current
        // purchase event time because we won't actually log it because is a duplicate.

        if (!dedupeCandidates.isNullOrEmpty() && indexOfOldestValidDedupe != null) {
            dedupeCandidates.removeAt(indexOfOldestValidDedupe)
            if (isImplicitlyLogged) {
                timesOfManualPurchases[roundedPurchase] = dedupeCandidates
            } else {
                timesOfImplicitPurchases[roundedPurchase] = dedupeCandidates
            }
            return true
        }

        // If we don't have a valid dedupe candidate, we should add our purchase event to the cache
        if (isImplicitlyLogged) {
            if (timesOfImplicitPurchases[roundedPurchase] == null) {
                timesOfImplicitPurchases[roundedPurchase] = mutableListOf()
            }
            timesOfImplicitPurchases[roundedPurchase]?.add(Pair(time, newPurchaseParameters))
        } else {
            if (timesOfManualPurchases[roundedPurchase] == null) {
                timesOfManualPurchases[roundedPurchase] = mutableListOf()
            }
            timesOfManualPurchases[roundedPurchase]?.add(Pair(time, newPurchaseParameters))
        }
        return false
    }

    fun atLeastOneEquivalentDedupeParameter(
        newPurchaseParameters: Bundle,
        oldPurchaseParameters: Bundle,
        dedupingWithImplicitlyLoggedHistory: Boolean
    ): Boolean {
        val dedupeParameters =
            InAppPurchaseDedupeConfig.getDedupeParameters(dedupingWithImplicitlyLoggedHistory)
        for (parameter in dedupeParameters) {
            val parameterInNewEvent = newPurchaseParameters.getString(parameter.first)
            if (parameterInNewEvent.isNullOrEmpty()) {
                continue
            }
            for (equivalentParameter in parameter.second) {
                val parameterInOldEvent = oldPurchaseParameters.getString(equivalentParameter)
                if (parameterInOldEvent.isNullOrEmpty()) {
                    continue
                }
                if (parameterInOldEvent == parameterInNewEvent) {
                    return true
                }
            }
        }
        return false;
    }
}
