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
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.NONE
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V1
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V2_V4
import com.facebook.appevents.iap.InAppPurchaseUtils.BillingClientVersion.V5_V7
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.UserSettingsManager
import com.facebook.appevents.OperationalData
import com.facebook.appevents.OperationalDataEnum
import com.facebook.appevents.iap.InAppPurchaseLoggerManager.updateLatestPossiblePurchaseTime
import com.facebook.appevents.internal.AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled
import com.facebook.appevents.internal.Constants
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseManager {
    private val timesOfManualPurchases =
        ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>>()
    private val timesOfImplicitPurchases =
        ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>>()
    private var specificBillingLibraryVersion: String? = null;
    private const val GOOGLE_BILLINGCLIENT_VERSION = "com.google.android.play.billingclient.version"
    private val enabled = AtomicBoolean(false)

    @JvmStatic
    fun enableAutoLogging() {
        if (!isImplicitPurchaseLoggingEnabled()) {
            updateLatestPossiblePurchaseTime()
            return
        }
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

    // This method will perform deduplication and return the dedupe parameters
    @Synchronized
    @JvmStatic
    fun performDedupe(
        purchases: List<InAppPurchase>,
        time: Long,
        isImplicitlyLogged: Boolean,
        purchaseParameters: List<Pair<Bundle?, OperationalData?>>
    ): Bundle? {
        if (purchaseParameters.isNullOrEmpty()) {
            return null
        }
        if (purchases.size != purchaseParameters?.size) {
            return null
        }
        var dedupeParameters: Bundle? = null

        /**
         * After deduplication, we will have a series of entries to remove from the cached history.
         * We will specifically remove the purchases that were involved in a dedupe, as we don't want
         * a single purchase to be responsible for multiple dedupes.
         */
        val purchasesToRemoveFromHistory = ArrayList<Pair<InAppPurchase, Long>>()

        for (i in purchases.indices) {
            val purchase = purchases[i]
            val (newPurchaseParameters, newPurchaseOperationalData) = purchaseParameters[i]

            val dedupeCandidates: MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>?
            var foundActualDuplicate = false

            // Round to two decimal places
            val roundedPurchase = InAppPurchase(
                purchase.eventName,
                purchase.amount.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble(),
                purchase.currency
            )
            dedupeCandidates = if (isImplicitlyLogged) {
                timesOfManualPurchases[roundedPurchase]
            } else {
                timesOfImplicitPurchases[roundedPurchase]
            }

            // We should dedupe with the oldest one in the time window to allow for as many valid dedupes as possible
            var oldestValidTime: Long? = null
            var dedupeParameter: String? = null
            var testDedupeParameter: String? = null
            if (!dedupeCandidates.isNullOrEmpty()) {
                for (timeBundlePair in dedupeCandidates) {
                    val candidateTime = timeBundlePair.first
                    val (candidateParameters, candidateOperationalData) = timeBundlePair.second
                    if (abs(time - candidateTime) > InAppPurchaseDedupeConfig.getDedupeWindow()) {
                        continue
                    }
                    if ((oldestValidTime == null || candidateTime < oldestValidTime)) {
                        dedupeParameter = getDedupeParameter(
                            newPurchaseParameters,
                            newPurchaseOperationalData,
                            candidateParameters,
                            candidateOperationalData,
                            !isImplicitlyLogged
                        )
                        val potentialTestDedupeParameter = getDedupeParameter(
                            newPurchaseParameters,
                            newPurchaseOperationalData,
                            candidateParameters,
                            candidateOperationalData,
                            !isImplicitlyLogged,
                            withTestDedupeKeys = true
                        )
                        if (potentialTestDedupeParameter != null) {
                            testDedupeParameter = potentialTestDedupeParameter
                        }
                        if (dedupeParameter != null) {
                            foundActualDuplicate = true
                            oldestValidTime = candidateTime
                            purchasesToRemoveFromHistory.add(Pair(roundedPurchase, candidateTime))
                        }
                    }
                }
            }


            // If we would have deduped with the test dedupe keys,
            // we should include the relevant parameters.
            if (testDedupeParameter != null) {
                if (dedupeParameters == null) {
                    dedupeParameters = Bundle()
                }
                dedupeParameters.putString(Constants.IAP_TEST_DEDUP_RESULT, "1")
                dedupeParameters.putString(
                    Constants.IAP_TEST_DEDUP_KEY_USED,
                    testDedupeParameter
                )
            }


            // If we found an actual duplicate,
            // we should include the relevant parameters.
            if (foundActualDuplicate) {
                if (dedupeParameters == null) {
                    dedupeParameters = Bundle()
                }
                val oldestValidTimeInSeconds = oldestValidTime?.div(1000) ?: 0
                dedupeParameters.putString(
                    Constants.IAP_NON_DEDUPED_EVENT_TIME,
                    oldestValidTimeInSeconds.toString()
                )
                dedupeParameters.putString(
                    Constants.IAP_ACTUAL_DEDUP_RESULT,
                    "1"
                )
                dedupeParameters.putString(
                    Constants.IAP_ACTUAL_DEDUP_KEY_USED,
                    dedupeParameter
                )
            }

            // If we didn't find a duplicate, we should add our purchase event to the cache
            if (isImplicitlyLogged && !foundActualDuplicate) {
                if (timesOfImplicitPurchases[roundedPurchase] == null) {
                    timesOfImplicitPurchases[roundedPurchase] = mutableListOf()
                }
                timesOfImplicitPurchases[roundedPurchase]?.add(
                    Pair(
                        time,
                        Pair(newPurchaseParameters, newPurchaseOperationalData)
                    )
                )
            } else if (!isImplicitlyLogged && !foundActualDuplicate) {
                if (timesOfManualPurchases[roundedPurchase] == null) {
                    timesOfManualPurchases[roundedPurchase] = mutableListOf()
                }
                timesOfManualPurchases[roundedPurchase]?.add(
                    Pair(
                        time,
                        Pair(newPurchaseParameters, newPurchaseOperationalData)
                    )
                )
            }
        }

        // If we have a valid dedupe candidate, we should remove it from the list in memory
        // so it doesn't dedupe multiple purchase events.
        for (purchaseInfo in purchasesToRemoveFromHistory) {
            val purchaseHistory = if (isImplicitlyLogged) {
                timesOfManualPurchases[purchaseInfo.first]
            } else {
                timesOfImplicitPurchases[purchaseInfo.first]
            }
            if (purchaseHistory == null) {
                continue
            }
            for ((index, timeBundlePair) in purchaseHistory.withIndex()) {
                if (timeBundlePair.first == purchaseInfo.second) {
                    purchaseHistory.removeAt(index)
                    break
                }
            }
            if (isImplicitlyLogged) {
                if (purchaseHistory.isEmpty()) {
                    timesOfManualPurchases.remove(purchaseInfo.first)
                } else {
                    timesOfManualPurchases[purchaseInfo.first] = purchaseHistory
                }
            } else {
                if (purchaseHistory.isEmpty()) {
                    timesOfImplicitPurchases.remove(purchaseInfo.first)
                } else {
                    timesOfImplicitPurchases[purchaseInfo.first] = purchaseHistory
                }
            }
        }

        return dedupeParameters
    }

    fun getDedupeParameter(
        newPurchaseParameters: Bundle?,
        newPurchaseOperationalData: OperationalData?,
        oldPurchaseParameters: Bundle?,
        oldPurchaseOperationalData: OperationalData?,
        dedupingWithImplicitlyLoggedHistory: Boolean,
        withTestDedupeKeys: Boolean = false
    ): String? {
        val dedupeParameters = if (withTestDedupeKeys) {
            InAppPurchaseDedupeConfig.getTestDedupeParameters(dedupingWithImplicitlyLoggedHistory)
        } else {
            InAppPurchaseDedupeConfig.getDedupeParameters(dedupingWithImplicitlyLoggedHistory)
        }
        if (dedupeParameters == null) {
            return null
        }
        for (parameter in dedupeParameters) {
            val parameterInNewEvent = OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                parameter.first,
                newPurchaseParameters,
                newPurchaseOperationalData
            ) as? String
            if (parameterInNewEvent.isNullOrEmpty()) {
                continue
            }
            for (equivalentParameter in parameter.second) {
                val parameterInOldEvent = OperationalData.getParameter(
                    OperationalDataEnum.IAPParameters,
                    equivalentParameter,
                    oldPurchaseParameters,
                    oldPurchaseOperationalData
                ) as? String
                if (parameterInOldEvent.isNullOrEmpty()) {
                    continue
                }
                if (parameterInOldEvent == parameterInNewEvent) {
                    // Always return the parameter that was originally in the manually logged event for consistency
                    return if (dedupingWithImplicitlyLoggedHistory) {
                        parameter.first
                    } else {
                        equivalentParameter
                    }
                }
            }
        }
        return null
    }
}
