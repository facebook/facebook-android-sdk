/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.appevents.iap.InAppPurchaseEventManager
import com.facebook.appevents.iap.InAppPurchaseUtils
import com.facebook.internal.FetchedAppGateKeepersManager.getGateKeeperForKey
import com.facebook.internal.FetchedAppSettingsManager.getAppSettingsWithoutQuery
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import java.math.BigDecimal
import java.util.Currency
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.appevents.internal is solely for the use of other packages within the Facebook SDK
 * for Android. Use of any of the classes in this package is unsupported, and they may be modified
 * or removed without warning at any time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AutomaticAnalyticsLogger {
    // Constants
    private val TAG = AutomaticAnalyticsLogger::class.java.canonicalName
    private const val APP_EVENTS_IF_AUTO_LOG_SUBS = "app_events_if_auto_log_subs"
    private val internalAppEventsLogger =
        InternalAppEventsLogger(FacebookSdk.getApplicationContext())

    @JvmStatic
    fun logActivateAppEvent() {
        val context = FacebookSdk.getApplicationContext()
        val appId = FacebookSdk.getApplicationId()
        val autoLogAppEvents = FacebookSdk.getAutoLogAppEventsEnabled()
        if (autoLogAppEvents) {
            if (context is Application) {
                AppEventsLogger.activateApp(context, appId)
            } else { // Context is probably originated from ContentProvider or Mocked
                Log.w(
                    TAG,
                    "Automatic logging of basic events will not happen, because " +
                            "FacebookSdk.getApplicationContext() returns object that is not " +
                            "instance of android.app.Application. Make sure you call " +
                            "FacebookSdk.sdkInitialize() from Application class and pass " +
                            "application context."
                )
            }
        }
    }

    @JvmStatic
    fun logActivityTimeSpentEvent(activityName: String?, timeSpentInSeconds: Long) {
        val context = FacebookSdk.getApplicationContext()
        val appId = FacebookSdk.getApplicationId()
        val settings = queryAppSettings(appId, false)
        if (settings != null && settings.automaticLoggingEnabled && timeSpentInSeconds > 0) {
            val logger = InternalAppEventsLogger(context)
            val params = Bundle(1)
            params.putCharSequence(Constants.AA_TIME_SPENT_SCREEN_PARAMETER_NAME, activityName)
            logger.logEvent(
                Constants.AA_TIME_SPENT_EVENT_NAME,
                timeSpentInSeconds.toDouble(),
                params
            )
        }
    }

    @JvmStatic
    fun logPurchase(purchase: String, skuDetails: String, isSubscription: Boolean) {
        if (!isImplicitPurchaseLoggingEnabled()) {
            return
        }
        val loggingParameters = getPurchaseLoggingParameters(purchase, skuDetails) ?: return
        val logAsSubs =
            isSubscription &&
                    getGateKeeperForKey(
                        APP_EVENTS_IF_AUTO_LOG_SUBS,
                        FacebookSdk.getApplicationId(),
                        false
                    )
        if (logAsSubs) {
            val eventName =
                if (InAppPurchaseEventManager.hasFreeTrialPeirod(skuDetails)) {
                    AppEventsConstants.EVENT_NAME_START_TRIAL
                } else {
                    AppEventsConstants.EVENT_NAME_SUBSCRIBE
                }
            internalAppEventsLogger.logEventImplicitly(
                eventName,
                loggingParameters.purchaseAmount,
                loggingParameters.currency,
                loggingParameters.param
            )
        } else {
            internalAppEventsLogger.logPurchaseImplicitly(
                loggingParameters.purchaseAmount,
                loggingParameters.currency,
                loggingParameters.param
            )
        }
    }

    @JvmStatic
    fun isImplicitPurchaseLoggingEnabled(): Boolean {
        val appId = FacebookSdk.getApplicationId()
        val settings = getAppSettingsWithoutQuery(appId)
        return settings != null &&
                FacebookSdk.getAutoLogAppEventsEnabled() &&
                settings.iAPAutomaticLoggingEnabled
    }

    private fun getPurchaseLoggingParameters(
        purchase: String,
        skuDetails: String
    ): PurchaseLoggingParameters? {
        return getPurchaseLoggingParameters(purchase, skuDetails, HashMap())
    }

    private fun getPurchaseLoggingParameters(
        purchase: String,
        skuDetails: String,
        extraParameter: Map<String, String>
    ): PurchaseLoggingParameters? {
        try {
            val purchaseJSON = JSONObject(purchase)
            val skuDetailsJSON = JSONObject(skuDetails)
            val params = Bundle(1)
            params.putCharSequence(
                Constants.IAP_PRODUCT_ID,
                purchaseJSON.getString(Constants.GP_IAP_PRODUCT_ID)
            )
            params.putCharSequence(
                Constants.IAP_PURCHASE_TIME,
                purchaseJSON.getString(Constants.GP_IAP_PURCHASE_TIME)
            )
            params.putCharSequence(
                Constants.IAP_PURCHASE_TOKEN,
                purchaseJSON.getString(Constants.GP_IAP_PURCHASE_TOKEN)
            )
            params.putCharSequence(
                Constants.IAP_PACKAGE_NAME,
                purchaseJSON.optString(Constants.GP_IAP_PACKAGE_NAME)
            )
            params.putCharSequence(
                Constants.IAP_PRODUCT_TITLE,
                skuDetailsJSON.optString(Constants.GP_IAP_TITLE)
            )
            params.putCharSequence(
                Constants.IAP_PRODUCT_DESCRIPTION,
                skuDetailsJSON.optString(Constants.GP_IAP_DESCRIPTION)
            )
            val type = skuDetailsJSON.optString(Constants.GP_IAP_TYPE)
            params.putCharSequence(Constants.IAP_PRODUCT_TYPE, type)
            if (type == InAppPurchaseUtils.IAPProductType.SUBS.type) {
                params.putCharSequence(
                    Constants.IAP_SUBSCRIPTION_AUTORENEWING,
                    java.lang.Boolean.toString(
                        purchaseJSON.optBoolean(
                            Constants.GP_IAP_AUTORENEWING,
                            false
                        )
                    )
                )
                params.putCharSequence(
                    Constants.IAP_SUBSCRIPTION_PERIOD,
                    skuDetailsJSON.optString(Constants.GP_IAP_SUBSCRIPTION_PERIOD)
                )
                params.putCharSequence(
                    Constants.IAP_FREE_TRIAL_PERIOD,
                    skuDetailsJSON.optString(Constants.GP_IAP_FREE_TRIAL_PERIOD)
                )
                val introductoryPriceCycles =
                    skuDetailsJSON.optString(Constants.GP_IAP_INTRODUCTORY_PRICE_CYCLES)
                if (introductoryPriceCycles.isNotEmpty()) {
                    params.putCharSequence(
                        Constants.IAP_INTRO_PRICE_AMOUNT_MICROS,
                        skuDetailsJSON.optString(Constants.GP_IAP_INTRODUCTORY_PRICE_AMOUNT_MICROS)
                    )
                    params.putCharSequence(
                        Constants.IAP_INTRO_PRICE_CYCLES,
                        introductoryPriceCycles
                    )
                }
            }
            extraParameter.forEach { (k, v) -> params.putCharSequence(k, v) }
            if (skuDetailsJSON.has(Constants.GP_IAP_PRICE_AMOUNT_MICROS_V2V4) && skuDetailsJSON.has(
                    Constants.GP_IAP_PRICE_CURRENCY_CODE_V2V4
                )
            ) {
                return PurchaseLoggingParameters(
                    BigDecimal(skuDetailsJSON.getLong(Constants.GP_IAP_PRICE_AMOUNT_MICROS_V2V4) / 1_000_000.0),
                    Currency.getInstance(skuDetailsJSON.getString(Constants.GP_IAP_PRICE_CURRENCY_CODE_V2V4)),
                    params
                )
            } else if (skuDetailsJSON.has(Constants.GP_IAP_ONE_TIME_PURCHASE_OFFER_DETAILS)) {
                val oneTimePurchaseOfferDetailsJSON =
                    skuDetailsJSON.getJSONObject(Constants.GP_IAP_ONE_TIME_PURCHASE_OFFER_DETAILS)
                return PurchaseLoggingParameters(
                    BigDecimal(oneTimePurchaseOfferDetailsJSON.getLong(Constants.GP_IAP_PRICE_AMOUNT_MICROS_V5V7) / 1_000_000.0),
                    Currency.getInstance(oneTimePurchaseOfferDetailsJSON.getString(Constants.GP_IAP_PRICE_CURRENCY_CODE_V5V7)),
                    params
                )
            } else {
                return null
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing in-app purchase/subscription data.", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get purchase logging parameters,", e)
            return null
        }
    }

    private class PurchaseLoggingParameters
    internal constructor(var purchaseAmount: BigDecimal, var currency: Currency, var param: Bundle)
}
