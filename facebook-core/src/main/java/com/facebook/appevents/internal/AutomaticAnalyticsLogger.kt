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
import com.facebook.appevents.OperationalData
import com.facebook.appevents.OperationalDataEnum.IAPParameters
import com.facebook.appevents.iap.InAppPurchase
import com.facebook.appevents.iap.InAppPurchaseDedupeConfig
import com.facebook.appevents.iap.InAppPurchaseEventManager
import com.facebook.appevents.iap.InAppPurchaseManager
import com.facebook.appevents.iap.InAppPurchaseUtils
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
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
    fun logPurchase(
        purchase: String,
        skuDetails: String,
        isSubscription: Boolean,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion?,
        isFirstAppLaunch: Boolean = false
    ) {
        if (!isImplicitPurchaseLoggingEnabled()) {
            return
        }
        var loggingParameters =
            getPurchaseLoggingParameters(purchase, skuDetails, billingClientVersion) ?: return
        if (loggingParameters.isEmpty()) {
            return
        }
        val logAsSubs =
            isSubscription &&
                    getGateKeeperForKey(
                        APP_EVENTS_IF_AUTO_LOG_SUBS,
                        FacebookSdk.getApplicationId(),
                        false
                    )
        val eventName = if (logAsSubs) {
            if (isFirstAppLaunch) {
                Constants.EVENT_NAME_SUBSCRIPTION_RESTORED
            } else if (InAppPurchaseEventManager.hasFreeTrialPeirod(skuDetails)) {
                AppEventsConstants.EVENT_NAME_START_TRIAL
            } else {
                AppEventsConstants.EVENT_NAME_SUBSCRIBE
            }
        } else {
            if (isFirstAppLaunch) {
                Constants.EVENT_NAME_PURCHASE_RESTORED
            } else {
                AppEventsConstants.EVENT_NAME_PURCHASED
            }
        }
        val dedupeParameters =
            if (isSubscription &&
                isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe)
            ) {
                getSubscriptionDedupeParameters(loggingParameters, eventName)
            } else if (!isSubscription &&
                isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe)
            ) {
                getPurchaseDedupeParameters(loggingParameters)
            } else {
                null
            }
        InAppPurchaseDedupeConfig.addDedupeParameters(
            dedupeParameters,
            loggingParameters[0].param,
            loggingParameters[0].operationalData
        )

        if (eventName != AppEventsConstants.EVENT_NAME_PURCHASED) {
            internalAppEventsLogger.logEventImplicitly(
                eventName,
                loggingParameters[0].purchaseAmount,
                loggingParameters[0].currency,
                loggingParameters[0].param,
                loggingParameters[0].operationalData
            )
        } else {
            internalAppEventsLogger.logPurchaseImplicitly(
                loggingParameters[0].purchaseAmount,
                loggingParameters[0].currency,
                loggingParameters[0].param,
                loggingParameters[0].operationalData
            )
        }
    }

    @Synchronized
    @JvmStatic
    fun getPurchaseDedupeParameters(purchaseLoggingParametersList: List<PurchaseLoggingParameters>): Bundle? {
        val purchaseParams = purchaseLoggingParametersList[0]
        val inAppPurchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            purchaseParams.purchaseAmount.toDouble(),
            purchaseParams.currency
        )
        return InAppPurchaseManager.performDedupe(
            listOf(inAppPurchase),
            System.currentTimeMillis(),
            true,
            listOf(Pair(purchaseParams.param, purchaseParams.operationalData))
        )
    }

    @Synchronized
    @JvmStatic
    fun getSubscriptionDedupeParameters(
        purchaseLoggingParametersList: List<PurchaseLoggingParameters>,
        eventName: String
    ): Bundle? {
        val purchasesToDedupe = ArrayList<InAppPurchase>()
        for (purchaseParams in purchaseLoggingParametersList) {
            val inAppPurchase =
                InAppPurchase(
                    eventName,
                    purchaseParams.purchaseAmount.toDouble(),
                    purchaseParams.currency
                )
            purchasesToDedupe.add(inAppPurchase)
        }
        return InAppPurchaseManager.performDedupe(
            purchasesToDedupe,
            System.currentTimeMillis(),
            true,
            purchaseLoggingParametersList.map { Pair(it.param, it.operationalData) })
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
        skuDetails: String,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion?
    ): List<PurchaseLoggingParameters>? {
        return getPurchaseLoggingParameters(purchase, skuDetails, HashMap(), billingClientVersion)
    }

    private fun getPurchaseParametersGPBLV2V4(
        type: String,
        params: Bundle,
        operationalData: OperationalData,
        purchaseJSON: JSONObject,
        skuDetailsJSON: JSONObject
    ): PurchaseLoggingParameters {
        if (type == InAppPurchaseUtils.IAPProductType.SUBS.type) {
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_SUBSCRIPTION_AUTORENEWING,
                java.lang.Boolean.toString(
                    purchaseJSON.optBoolean(
                        Constants.GP_IAP_AUTORENEWING,
                        false
                    )
                ),
                params,
                operationalData
            )
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_SUBSCRIPTION_PERIOD,
                skuDetailsJSON.optString(Constants.GP_IAP_SUBSCRIPTION_PERIOD),
                params,
                operationalData
            )
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_FREE_TRIAL_PERIOD,
                skuDetailsJSON.optString(Constants.GP_IAP_FREE_TRIAL_PERIOD),
                params,
                operationalData
            )
            val introductoryPriceCycles =
                skuDetailsJSON.optString(Constants.GP_IAP_INTRODUCTORY_PRICE_CYCLES)
            if (introductoryPriceCycles.isNotEmpty()) {
                OperationalData.addParameter(
                    IAPParameters,
                    Constants.IAP_INTRO_PRICE_CYCLES,
                    introductoryPriceCycles,
                    params,
                    operationalData
                )
            }

            val introductoryPricePeriod =
                skuDetailsJSON.optString(Constants.GP_IAP_INTRODUCTORY_PRICE_PERIOD)
            if (introductoryPricePeriod.isNotEmpty()) {
                OperationalData.addParameter(
                    IAPParameters,
                    Constants.IAP_INTRO_PERIOD,
                    introductoryPricePeriod,
                    params,
                    operationalData
                )
            }
            val introductoryPriceAmountMicros =
                skuDetailsJSON.optString(Constants.GP_IAP_INTRODUCTORY_PRICE_AMOUNT_MICROS)
            if (introductoryPriceAmountMicros.isNotEmpty()) {
                OperationalData.addParameter(
                    IAPParameters,
                    Constants.IAP_INTRO_PRICE_AMOUNT_MICROS,
                    introductoryPriceAmountMicros,
                    params,
                    operationalData
                )
            }
        }
        return PurchaseLoggingParameters(
            BigDecimal(skuDetailsJSON.getLong(Constants.GP_IAP_PRICE_AMOUNT_MICROS_V2V4) / 1_000_000.0),
            Currency.getInstance(skuDetailsJSON.getString(Constants.GP_IAP_PRICE_CURRENCY_CODE_V2V4)),
            params,
            operationalData
        )
    }

    private fun getPurchaseParametersGPBLV5V7(
        type: String,
        params: Bundle,
        operationalData: OperationalData,
        skuDetailsJSON: JSONObject
    ): List<PurchaseLoggingParameters>? {
        if (type == InAppPurchaseUtils.IAPProductType.SUBS.type) {
            val subscriptionParametersList = mutableListOf<PurchaseLoggingParameters>()
            val subscriptionOfferDetailsJSONArray =
                skuDetailsJSON.getJSONArray(Constants.GP_IAP_SUBSCRIPTION_OFFER_DETAILS)
                    ?: return null
            val numberOfBasePlans = subscriptionOfferDetailsJSONArray.length()
            for (index in 0 until numberOfBasePlans) {
                val subscriptionOfferDetailsJSON =
                    skuDetailsJSON.getJSONArray(Constants.GP_IAP_SUBSCRIPTION_OFFER_DETAILS)
                        .getJSONObject(index) ?: return null
                val planSpecificBundle = Bundle(params)
                val planSpecificOperationalData = operationalData.copy()
                val basePlanId =
                    subscriptionOfferDetailsJSON.getString(Constants.GP_IAP_BASE_PLAN_ID)
                OperationalData.addParameter(
                    IAPParameters,
                    Constants.IAP_BASE_PLAN,
                    basePlanId,
                    planSpecificBundle,
                    planSpecificOperationalData
                )

                val pricingPhases =
                    subscriptionOfferDetailsJSON.getJSONArray(Constants.GP_IAP_SUBSCRIPTION_PRICING_PHASES)

                // Get the price of the final phase, which is the price of the actual base plan
                val subscriptionJSON =
                    pricingPhases.getJSONObject(pricingPhases.length() - 1) ?: return null

                OperationalData.addParameter(
                    IAPParameters,
                    Constants.IAP_SUBSCRIPTION_PERIOD, subscriptionJSON.optString(
                        Constants.GP_IAP_BILLING_PERIOD
                    ),
                    planSpecificBundle,
                    planSpecificOperationalData
                )
                if (subscriptionJSON.has(Constants.GP_IAP_RECURRENCE_MODE) && subscriptionJSON.getInt(
                        Constants.GP_IAP_RECURRENCE_MODE
                    ) != 3
                ) {
                    OperationalData.addParameter(
                        IAPParameters,
                        Constants.IAP_SUBSCRIPTION_AUTORENEWING,
                        true.toString(),
                        planSpecificBundle,
                        planSpecificOperationalData
                    )
                } else {
                    OperationalData.addParameter(
                        IAPParameters,
                        Constants.IAP_SUBSCRIPTION_AUTORENEWING,
                        false.toString(),
                        planSpecificBundle,
                        planSpecificOperationalData
                    )
                }
                subscriptionParametersList.add(
                    PurchaseLoggingParameters(
                        BigDecimal(subscriptionJSON.getLong(Constants.GP_IAP_PRICE_AMOUNT_MICROS_V5V7) / 1_000_000.0),
                        Currency.getInstance(subscriptionJSON.getString(Constants.GP_IAP_PRICE_CURRENCY_CODE_V5V7)),
                        planSpecificBundle,
                        planSpecificOperationalData
                    )
                )
            }
            return subscriptionParametersList
        } else {
            val oneTimePurchaseOfferDetailsJSON =
                skuDetailsJSON.getJSONObject(Constants.GP_IAP_ONE_TIME_PURCHASE_OFFER_DETAILS)
                    ?: return null
            return mutableListOf(
                PurchaseLoggingParameters(
                    BigDecimal(oneTimePurchaseOfferDetailsJSON.getLong(Constants.GP_IAP_PRICE_AMOUNT_MICROS_V5V7) / 1_000_000.0),
                    Currency.getInstance(oneTimePurchaseOfferDetailsJSON.getString(Constants.GP_IAP_PRICE_CURRENCY_CODE_V5V7)),
                    params,
                    operationalData
                )
            )
        }
    }

    private fun getPurchaseLoggingParameters(
        purchase: String,
        skuDetails: String,
        extraParameter: Map<String, String>,
        billingClientVersion: InAppPurchaseUtils.BillingClientVersion?
    ): List<PurchaseLoggingParameters>? {
        try {
            val purchaseJSON = JSONObject(purchase)
            val skuDetailsJSON = JSONObject(skuDetails)
            val params = Bundle(1)
            val operationalData = OperationalData()
            if (billingClientVersion != null) {
                OperationalData.addParameter(
                    IAPParameters,
                    Constants.IAP_AUTOLOG_IMPLEMENTATION,
                    billingClientVersion.type,
                    params,
                    operationalData
                )
            }
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_PRODUCT_ID,
                purchaseJSON.getString(Constants.GP_IAP_PRODUCT_ID),
                params,
                operationalData
            )
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_PURCHASE_TIME,
                purchaseJSON.getString(Constants.GP_IAP_PURCHASE_TIME),
                params,
                operationalData
            )
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_PURCHASE_TOKEN,
                purchaseJSON.getString(Constants.GP_IAP_PURCHASE_TOKEN),
                params,
                operationalData
            )
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_PACKAGE_NAME,
                purchaseJSON.optString(Constants.GP_IAP_PACKAGE_NAME),
                params,
                operationalData
            )
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_PRODUCT_TITLE,
                skuDetailsJSON.optString(Constants.GP_IAP_TITLE),
                params,
                operationalData
            )
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_PRODUCT_DESCRIPTION,
                skuDetailsJSON.optString(Constants.GP_IAP_DESCRIPTION),
                params,
                operationalData
            )
            val type = skuDetailsJSON.optString(Constants.GP_IAP_TYPE)
            OperationalData.addParameter(
                IAPParameters,
                Constants.IAP_PRODUCT_TYPE,
                type,
                params,
                operationalData
            )
            val specificBillingLibraryVersion =
                InAppPurchaseManager.getSpecificBillingLibraryVersion()
            if (specificBillingLibraryVersion != null) {
                OperationalData.addParameter(
                    IAPParameters,
                    Constants.IAP_BILLING_LIBRARY_VERSION,
                    specificBillingLibraryVersion,
                    params,
                    operationalData
                )
            }

            extraParameter.forEach { (k, v) ->
                OperationalData.addParameter(
                    IAPParameters,
                    k,
                    v,
                    params,
                    operationalData
                )
            }


            return if (skuDetailsJSON.has(Constants.GP_IAP_PRICE_AMOUNT_MICROS_V2V4)) {
                /**
                 * We can get the specific base plan of in app purchases coming from GPBL v2-v4,
                 * so we only need to return one PurchaseLoggingParameters object.
                 */
                mutableListOf(
                    getPurchaseParametersGPBLV2V4(
                        type,
                        params,
                        operationalData,
                        purchaseJSON,
                        skuDetailsJSON
                    )
                )

            } else if (skuDetailsJSON.has(Constants.GP_IAP_SUBSCRIPTION_OFFER_DETAILS) || skuDetailsJSON.has(
                    Constants.GP_IAP_ONE_TIME_PURCHASE_OFFER_DETAILS
                )
            ) {
                // GPBL v5 - v7
                getPurchaseParametersGPBLV5V7(type, params, operationalData, skuDetailsJSON)
            } else {
                null
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing in-app purchase/subscription data.", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get purchase logging parameters,", e)
            return null
        }
    }

    class PurchaseLoggingParameters
    internal constructor(
        var purchaseAmount: BigDecimal,
        var currency: Currency,
        var param: Bundle,
        var operationalData: OperationalData
    )
}
