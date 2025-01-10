/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.appevents.internal.AutomaticAnalyticsLogger.logPurchase
import com.facebook.appevents.internal.Constants
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import org.json.JSONObject
import kotlin.Exception
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseLoggerManager {
    // January 10, 2025 05:00:00 PM GMT
    private const val APPROXIMATE_IAP_ENHANCEMENT_RELEASE_TIME = 1736528400000L
    private const val MILLISECONDS_IN_SECONDS = 1000.0
    private const val PURCHASE_TIME = "purchaseTime"
    private const val IAP_SKU_CACHE_GPBLV1 = "com.facebook.internal.SKU_DETAILS"
    private const val IAP_PURCHASE_CACHE_GPBLV1 = "com.facebook.internal.PURCHASE"
    private const val IAP_CACHE_OLD = "com.facebook.internal.iap.PRODUCT_DETAILS"

    private const val IAP_CACHE_GPBLV2V7 = "com.facebook.internal.iap.IAP_CACHE_GPBLV2V7"
    private const val CACHED_PURCHASES_KEY = "PURCHASE_DETAILS_SET"
    private const val APP_HAS_BEEN_LAUNCHED_KEY = "APP_HAS_BEEN_LAUNCHED_KEY"
    private const val TIME_OF_LAST_LOGGED_PURCHASE_KEY = "TIME_OF_LAST_LOGGED_PURCHASE"
    private const val TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY = "TIME_OF_LAST_LOGGED_SUBSCRIPTION"

    @JvmStatic
    fun migrateOldCacheHistory() {
        val iapCache =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_GPBLV2V7,
                Context.MODE_PRIVATE
            )
        var newestCandidateTime = max(
            max(
                iapCache.getLong(
                    TIME_OF_LAST_LOGGED_PURCHASE_KEY,
                    0L
                ),
                iapCache.getLong(
                    TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY,
                    0L
                )
            ), APPROXIMATE_IAP_ENHANCEMENT_RELEASE_TIME
        )
        val cachedPurchaseSet: MutableSet<String> = CopyOnWriteArraySet()
        val sharedPreferences =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_OLD,
                Context.MODE_PRIVATE
            )
        if (sharedPreferences.contains(CACHED_PURCHASES_KEY)) {
            cachedPurchaseSet.addAll(
                sharedPreferences.getStringSet(CACHED_PURCHASES_KEY, hashSetOf()) ?: hashSetOf()
            )

            // Construct purchase de-dup map.
            for (purchaseHistory in cachedPurchaseSet) {
                try {
                    val splitPurchase = purchaseHistory.split(";", limit = 2)
                    val timeInMilliseconds = splitPurchase[1].toLong() * 1000L

                    // Don't migrate any invalid values in cache
                    val digitsInTime = timeInMilliseconds.toString().length
                    val digitsInReleaseTime =
                        APPROXIMATE_IAP_ENHANCEMENT_RELEASE_TIME.toString().length
                    if (abs(digitsInTime - digitsInReleaseTime) >= log10(
                            MILLISECONDS_IN_SECONDS
                        )
                    ) {
                        continue
                    }
                    newestCandidateTime = max(newestCandidateTime, timeInMilliseconds)

                } catch (e: Exception) {
                    /* Swallow */
                    continue
                }
            }
        }
        iapCache.edit()
            .putLong(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY, newestCandidateTime)
            .apply()
        iapCache.edit()
            .putLong(TIME_OF_LAST_LOGGED_PURCHASE_KEY, newestCandidateTime)
            .apply()
        deleteOldCacheHistory()
    }

    @JvmStatic
    fun deleteOldCacheHistory() {
        // clear cached purchases logged by lib 1
        val cachedSkuSharedPref =
            getApplicationContext()
                .getSharedPreferences(IAP_SKU_CACHE_GPBLV1, Context.MODE_PRIVATE)
        val cachedPurchaseSharedPref =
            getApplicationContext()
                .getSharedPreferences(IAP_PURCHASE_CACHE_GPBLV1, Context.MODE_PRIVATE)
        cachedSkuSharedPref.edit().clear().apply()
        cachedPurchaseSharedPref.edit().clear().apply()

        // clear cached purchases logged by lib 2 - 3
        val sharedPreferences =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_OLD,
                Context.MODE_PRIVATE
            )
        sharedPreferences.edit().clear().apply()
    }

    @JvmStatic
    fun getIsFirstAppLaunchWithNewIAP(): Boolean {
        val iapCache =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_GPBLV2V7,
                Context.MODE_PRIVATE
            )
        return !iapCache.contains(APP_HAS_BEEN_LAUNCHED_KEY)
    }

    @JvmStatic
    fun setAppHasBeenLaunchedWithNewIAP() {
        val iapCache =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_GPBLV2V7,
                Context.MODE_PRIVATE
            )
        try {
            iapCache.edit()
                .putBoolean(APP_HAS_BEEN_LAUNCHED_KEY, true)
                .apply()
        } catch (e: Exception) {
            /* Swallow */
        }
    }

    @JvmStatic
    fun updateLatestPossiblePurchaseTime() {
        try {
            val iapCache =
                getApplicationContext().getSharedPreferences(
                    IAP_CACHE_GPBLV2V7,
                    Context.MODE_PRIVATE
                )
            val currTime = System.currentTimeMillis()
            iapCache.edit()
                .putLong(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY, currTime)
                .apply()
            iapCache.edit()
                .putLong(TIME_OF_LAST_LOGGED_PURCHASE_KEY, currTime)
                .apply()
        } catch (e: Exception) {
            /* Swallow */
        }
    }

    @JvmStatic
    fun filterPurchaseLogging(
        purchaseDetailsMap: MutableMap<String, JSONObject>,
        skuDetailsMap: Map<String, JSONObject?>,
        isSubscription: Boolean,
        packageName: String,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion,
        isFirstAppLaunch: Boolean,
    ) {
        val deduped = cacheDeDupPurchase(purchaseDetailsMap, isSubscription)
        val loggingReady = constructLoggingReadyMap(
            deduped,
            skuDetailsMap,
            packageName
        )
        logPurchases(loggingReady, isSubscription, billingClientVersion, isFirstAppLaunch)
    }

    private fun logPurchases(
        purchaseDetailsMap: Map<String, String>,
        isSubscription: Boolean,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion,
        isFirstAppLaunch: Boolean
    ) {
        for ((purchaseDetails, skuDetails) in purchaseDetailsMap) {
            logPurchase(
                purchaseDetails,
                skuDetails,
                isSubscription,
                billingClientVersion,
                isFirstAppLaunch
            )
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun cacheDeDupPurchase(
        purchaseDetailsMap: MutableMap<String, JSONObject>,
        isSubscription: Boolean,
    ): Map<String, JSONObject> {
        val iapCache =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_GPBLV2V7,
                Context.MODE_PRIVATE
            )
        var timeOfLatestNewlyLoggedPurchase: Long = 0
        val timeOfLastLoggedPurchase: Long =
            if (isSubscription) {
                iapCache.getLong(
                    TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY,
                    APPROXIMATE_IAP_ENHANCEMENT_RELEASE_TIME
                )
            } else {
                iapCache.getLong(
                    TIME_OF_LAST_LOGGED_PURCHASE_KEY,
                    APPROXIMATE_IAP_ENHANCEMENT_RELEASE_TIME
                )
            }

        val tempPurchaseDetailsMap: Map<String, JSONObject> = purchaseDetailsMap.toMap()
        for ((key, purchaseJson) in tempPurchaseDetailsMap) {
            try {
                if (purchaseJson.has(Constants.GP_IAP_PURCHASE_TOKEN) && purchaseJson.has(
                        Constants.GP_IAP_PURCHASE_TIME
                    )
                ) {
                    val purchaseTime = purchaseJson.getLong(PURCHASE_TIME)
                    if (purchaseTime <= timeOfLastLoggedPurchase) {
                        purchaseDetailsMap.remove(key)
                    }

                    timeOfLatestNewlyLoggedPurchase =
                        max(timeOfLatestNewlyLoggedPurchase, purchaseTime)
                }
            } catch (e: Exception) {
                /* swallow */
            }
        }
        if (timeOfLatestNewlyLoggedPurchase >= timeOfLastLoggedPurchase) {
            if (isSubscription) {
                iapCache.edit()
                    .putLong(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY, timeOfLatestNewlyLoggedPurchase)
                    .apply()
            } else {
                iapCache.edit()
                    .putLong(TIME_OF_LAST_LOGGED_PURCHASE_KEY, timeOfLatestNewlyLoggedPurchase)
                    .apply()
            }
        }
        return HashMap(purchaseDetailsMap)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun constructLoggingReadyMap(
        purchaseDetailsMap: Map<String, JSONObject>,
        skuDetailsMap: Map<String, JSONObject?>,
        packageName: String
    ): Map<String, String> {
        val purchaseResultMap: MutableMap<String, String> = mutableMapOf()
        for ((key, purchaseDetail) in purchaseDetailsMap) {
            val skuDetail = skuDetailsMap[key]
            try {
                // Used during server-side processing of purchase verification
                purchaseDetail.put(InAppPurchaseConstants.PACKAGE_NAME, packageName)

                if (skuDetail != null) {
                    purchaseResultMap[purchaseDetail.toString()] = skuDetail.toString()
                }
            } catch (e: Exception) {
                /* swallow */
            }
        }
        return purchaseResultMap
    }
}
