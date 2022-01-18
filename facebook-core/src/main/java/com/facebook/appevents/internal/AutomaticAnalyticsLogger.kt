/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
  private val internalAppEventsLogger = InternalAppEventsLogger(FacebookSdk.getApplicationContext())

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
                "application context.")
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
      logger.logEvent(Constants.AA_TIME_SPENT_EVENT_NAME, timeSpentInSeconds.toDouble(), params)
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
            getGateKeeperForKey(APP_EVENTS_IF_AUTO_LOG_SUBS, FacebookSdk.getApplicationId(), false)
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
          loggingParameters.param)
    } else {
      internalAppEventsLogger.logPurchaseImplicitly(
          loggingParameters.purchaseAmount, loggingParameters.currency, loggingParameters.param)
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
    return try {
      val purchaseJSON = JSONObject(purchase)
      val skuDetailsJSON = JSONObject(skuDetails)
      val params = Bundle(1)
      params.putCharSequence(Constants.IAP_PRODUCT_ID, purchaseJSON.getString("productId"))
      params.putCharSequence(Constants.IAP_PURCHASE_TIME, purchaseJSON.getString("purchaseTime"))
      params.putCharSequence(Constants.IAP_PURCHASE_TOKEN, purchaseJSON.getString("purchaseToken"))
      params.putCharSequence(Constants.IAP_PACKAGE_NAME, purchaseJSON.optString("packageName"))
      params.putCharSequence(Constants.IAP_PRODUCT_TITLE, skuDetailsJSON.optString("title"))
      params.putCharSequence(
          Constants.IAP_PRODUCT_DESCRIPTION, skuDetailsJSON.optString("description"))
      val type = skuDetailsJSON.optString("type")
      params.putCharSequence(Constants.IAP_PRODUCT_TYPE, type)
      if (type == "subs") {
        params.putCharSequence(
            Constants.IAP_SUBSCRIPTION_AUTORENEWING,
            java.lang.Boolean.toString(purchaseJSON.optBoolean("autoRenewing", false)))
        params.putCharSequence(
            Constants.IAP_SUBSCRIPTION_PERIOD, skuDetailsJSON.optString("subscriptionPeriod"))
        params.putCharSequence(
            Constants.IAP_FREE_TRIAL_PERIOD, skuDetailsJSON.optString("freeTrialPeriod"))
        val introductoryPriceCycles = skuDetailsJSON.optString("introductoryPriceCycles")
        if (!introductoryPriceCycles.isEmpty()) {
          params.putCharSequence(
              Constants.IAP_INTRO_PRICE_AMOUNT_MICROS,
              skuDetailsJSON.optString("introductoryPriceAmountMicros"))
          params.putCharSequence(Constants.IAP_INTRO_PRICE_CYCLES, introductoryPriceCycles)
        }
      }
      extraParameter.forEach { (k, v) -> params.putCharSequence(k, v) }
      PurchaseLoggingParameters(
          BigDecimal(skuDetailsJSON.getLong("price_amount_micros") / 1_000_000.0),
          Currency.getInstance(skuDetailsJSON.getString("price_currency_code")),
          params)
    } catch (e: JSONException) {
      Log.e(TAG, "Error parsing in-app subscription data.", e)
      null
    }
  }

  private class PurchaseLoggingParameters
  internal constructor(var purchaseAmount: BigDecimal, var currency: Currency, var param: Bundle)
}
