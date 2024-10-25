/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.appevents.internal.AutomaticAnalyticsLogger.logPurchase
import com.facebook.appevents.internal.Constants
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.Exception
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseLoggerManager {
    private lateinit var sharedPreferences: SharedPreferences
    private const val APPROXIMATE_IAP_ENHANCEMENT_RELEASE_TIME = 1730358000000L
    private const val PURCHASE_TIME = "purchaseTime"
    private const val IAP_SKU_CACHE_GPBLV1 = "com.facebook.internal.SKU_DETAILS"
    private const val IAP_PURCHASE_CACHE_GPBLV1 = "com.facebook.internal.PURCHASE"
    private const val IAP_CACHE_OLD = "com.facebook.internal.iap.PRODUCT_DETAILS"
    private val cachedPurchaseSet: MutableSet<String> = CopyOnWriteArraySet()
    private val cachedPurchaseMap: MutableMap<String, Long> = ConcurrentHashMap()

    private const val IAP_CACHE_GPBLV2V7 = "com.facebook.internal.iap.IAP_CACHE_GPBLV2V7"
    private const val CACHED_PURCHASES_KEY = "PURCHASE_DETAILS_SET"
    private const val TIME_OF_LAST_LOGGED_PURCHASE_KEY = "TIME_OF_LAST_LOGGED_PURCHASE"
    private const val TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY = "TIME_OF_LAST_LOGGED_SUBSCRIPTION"
    private var firstTimeLoggingIAP = false

    private fun readOldCaches() {
        // clear cached purchases logged by lib 1
        val cachedSkuSharedPref =
            getApplicationContext()
                .getSharedPreferences(IAP_SKU_CACHE_GPBLV1, Context.MODE_PRIVATE)
        val cachedPurchaseSharedPref =
            getApplicationContext()
                .getSharedPreferences(IAP_PURCHASE_CACHE_GPBLV1, Context.MODE_PRIVATE)
        cachedSkuSharedPref.edit().clear().apply()
        cachedPurchaseSharedPref.edit().clear().apply()


        sharedPreferences =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_OLD,
                Context.MODE_PRIVATE
            )
        if (sharedPreferences.contains(CACHED_PURCHASES_KEY)) {
            firstTimeLoggingIAP = true
            cachedPurchaseSet.addAll(
                sharedPreferences.getStringSet(CACHED_PURCHASES_KEY, hashSetOf()) ?: hashSetOf()
            )

            // Construct purchase de-dup map.
            for (purchaseHistory in cachedPurchaseSet) {
                val splitPurchase = purchaseHistory.split(";", limit = 2)
                cachedPurchaseMap[splitPurchase[0]] = splitPurchase[1].toLong()
            }

        }

        // clear cached purchases logged by lib2 - lib4 in the old implementation of IAP auto-logging
        sharedPreferences.edit().clear().apply()
    }

    @JvmStatic
    fun filterPurchaseLogging(
        purchaseDetailsMap: MutableMap<String, JSONObject>,
        skuDetailsMap: Map<String, JSONObject?>,
        isSubscription: Boolean,
        packageName: String,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion

    ) {
        readOldCaches()
        val loggingReadyMap: Map<String, String> =
            constructLoggingReadyMap(
                cacheDeDupPurchase(purchaseDetailsMap, isSubscription),
                skuDetailsMap,
                packageName
            )
        logPurchases(loggingReadyMap, isSubscription, billingClientVersion)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getTimeOfNewestPurchaseInOldCache(): Long {
        var newestPurchaseTime = 0L
        if (cachedPurchaseMap.isEmpty()) {
            return newestPurchaseTime
        }
        for ((_, time) in cachedPurchaseMap) {
            newestPurchaseTime = max(newestPurchaseTime, time)
        }
        return min(newestPurchaseTime * 1000L, APPROXIMATE_IAP_ENHANCEMENT_RELEASE_TIME)
    }

    private fun logPurchases(
        purchaseDetailsMap: Map<String, String>,
        isSubscription: Boolean,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion
    ) {
        for ((purchaseDetails, skuDetails) in purchaseDetailsMap) {
            logPurchase(purchaseDetails, skuDetails, isSubscription, billingClientVersion)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun cacheDeDupPurchase(
        purchaseDetailsMap: MutableMap<String, JSONObject>,
        isSubscription: Boolean
    ): Map<String, JSONObject> {
        val iapCache =
            getApplicationContext().getSharedPreferences(
                IAP_CACHE_GPBLV2V7,
                Context.MODE_PRIVATE
            )
        var timeOfLatestNewlyLoggedPurchase: Long = 0
        var timeOfLastLoggedPurchase: Long = 0
        if (firstTimeLoggingIAP) {
            timeOfLastLoggedPurchase = getTimeOfNewestPurchaseInOldCache()
        } else {
            if (isSubscription) {
                timeOfLastLoggedPurchase = iapCache.getLong(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY, 0)
            } else {
                timeOfLastLoggedPurchase = iapCache.getLong(TIME_OF_LAST_LOGGED_PURCHASE_KEY, 0)
            }
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
        if (firstTimeLoggingIAP) {
            iapCache.edit()
                .putLong(TIME_OF_LAST_LOGGED_SUBSCRIPTION_KEY, timeOfLatestNewlyLoggedPurchase)
                .apply()
            iapCache.edit()
                .putLong(TIME_OF_LAST_LOGGED_PURCHASE_KEY, timeOfLatestNewlyLoggedPurchase)
                .apply()
        } else if (timeOfLatestNewlyLoggedPurchase >= timeOfLastLoggedPurchase) {
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
        firstTimeLoggingIAP = false
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
