/**
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

package com.facebook.appevents.internal;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.appevents.InternalAppEventsLogger;
import com.facebook.internal.FetchedAppGateKeepersManager;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * com.facebook.appevents.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class AutomaticAnalyticsLogger {
    // Constants
    private static final String TAG = AutomaticAnalyticsLogger.class.getCanonicalName();

    private static final InternalAppEventsLogger internalAppEventsLogger =
            new InternalAppEventsLogger(FacebookSdk.getApplicationContext());

    public static void logActivateAppEvent() {
        final Context context = FacebookSdk.getApplicationContext();
        final String appId = FacebookSdk.getApplicationId();
        final boolean autoLogAppEvents = FacebookSdk.getAutoLogAppEventsEnabled();
        Validate.notNull(context, "context");
        if (autoLogAppEvents) {
            if (context instanceof Application) {
                AppEventsLogger.activateApp((Application) context, appId);
            } else { // Context is probably originated from ContentProvider or Mocked
                Log.w(
                        TAG,
                        "Automatic logging of basic events will not happen, because " +
                                "FacebookSdk.getApplicationContext() returns object that is not " +
                                "instance of android.app.Application. Make sure you call " +
                                "FacebookSdk.sdkInitialize() from Application class and pass " +
                                "application context.");
            }
        }
    }

    public static void logActivityTimeSpentEvent(String activityName, long timeSpentInSeconds) {
        final Context context = FacebookSdk.getApplicationContext();
        final String appId = FacebookSdk.getApplicationId();
        Validate.notNull(context, "context");
        final FetchedAppSettings settings = FetchedAppSettingsManager.queryAppSettings(
                appId, false);
        if (settings != null
                && settings.getAutomaticLoggingEnabled()
                && timeSpentInSeconds > 0) {
            InternalAppEventsLogger logger = new InternalAppEventsLogger(context);
            Bundle params = new Bundle(1);
            params.putCharSequence(Constants.AA_TIME_SPENT_SCREEN_PARAMETER_NAME, activityName);
            logger.logEvent(
                    Constants.AA_TIME_SPENT_EVENT_NAME, timeSpentInSeconds, params);
        }
    }

    public static void logPurchaseInapp(
            String purchase,
            String skuDetails
    ) {
        if (!isImplicitPurchaseLoggingEnabled()){
            return;
        }
        PurchaseLoggingParameters loggingParameters =
                getPurchaseLoggingParameters(purchase, skuDetails);

        if (loggingParameters != null) {
            internalAppEventsLogger.logPurchaseImplicitly(
                    loggingParameters.purchaseAmount,
                    loggingParameters.currency,
                    loggingParameters.param);
        }
    }

    /**
     * Log subscription related events: subscribe, start trial, cancel, restore, heartbeat, expire
     */
    public static void logPurchaseSubs(
            final SubscriptionType subsType,
            final String purchase,
            final String skuDetails
    ) {
        if (!isImplicitPurchaseLoggingEnabled()){
            return;
        }
        boolean passGK = FetchedAppGateKeepersManager.getGateKeeperForKey(
                FetchedAppGateKeepersManager.APP_EVENTS_IF_AUTO_LOG_SUBS,
                FacebookSdk.getApplicationId(),
                false);

        String eventName;
        switch (subsType) {
            case SUBSCRIBE:
                if (passGK) {
                    eventName = AppEventsConstants.EVENT_NAME_SUBSCRIBE;
                    break;
                } else {
                    logPurchaseInapp(purchase, skuDetails);
                    return;
                }
            case START_TRIAL:
                if (passGK) {
                    eventName = AppEventsConstants.EVENT_NAME_START_TRIAL;
                    break;
                } else {
                    logPurchaseInapp(purchase, skuDetails);
                    return;
                }
            default:
                return;
        }

        PurchaseLoggingParameters loggingParameters =
                getPurchaseLoggingParameters(purchase, skuDetails);

        if (loggingParameters != null) {
            internalAppEventsLogger.logEventImplicitly(
                    eventName,
                    loggingParameters.purchaseAmount,
                    loggingParameters.currency,
                    loggingParameters.param);
        }
    }

    public static boolean isImplicitPurchaseLoggingEnabled() {
        final String appId = FacebookSdk.getApplicationId();
        final FetchedAppSettings settings = FetchedAppSettingsManager.getAppSettingsWithoutQuery(
                appId);

        return settings != null &&
                FacebookSdk.getAutoLogAppEventsEnabled() &&
                settings.getIAPAutomaticLoggingEnabled();
    }

    @Nullable
    private static PurchaseLoggingParameters getPurchaseLoggingParameters(
            String purchase, String skuDetails) {
        return getPurchaseLoggingParameters(purchase, skuDetails, new HashMap<String, String>());
    }

    @Nullable
    private static PurchaseLoggingParameters getPurchaseLoggingParameters(
            String purchase, String skuDetails, Map<String, String> extraParameter) {

        try {
            JSONObject purchaseJSON = new JSONObject(purchase);
            JSONObject skuDetailsJSON = new JSONObject(skuDetails);

            Bundle params = new Bundle(1);

            params.putCharSequence(
                    Constants.IAP_PRODUCT_ID,
                    purchaseJSON.getString("productId"));
            params.putCharSequence(
                    Constants.IAP_PURCHASE_TIME,
                    purchaseJSON.getString("purchaseTime"));
            params.putCharSequence(
                    Constants.IAP_PURCHASE_TOKEN,
                    purchaseJSON.getString("purchaseToken"));
            params.putCharSequence(
                    Constants.IAP_PACKAGE_NAME,
                    purchaseJSON.optString("packageName"));
            params.putCharSequence(
                    Constants.IAP_PRODUCT_TITLE,
                    skuDetailsJSON.optString("title"));
            params.putCharSequence(
                    Constants.IAP_PRODUCT_DESCRIPTION,
                    skuDetailsJSON.optString("description"));

            String type = skuDetailsJSON.optString("type");
            params.putCharSequence(
                    Constants.IAP_PRODUCT_TYPE,
                    type);
            if (type.equals("subs")) {
                params.putCharSequence(
                        Constants.IAP_SUBSCRIPTION_AUTORENEWING,
                        Boolean.toString(purchaseJSON.optBoolean("autoRenewing",
                                false)));
                params.putCharSequence(
                        Constants.IAP_SUBSCRIPTION_PERIOD,
                        skuDetailsJSON.optString("subscriptionPeriod"));
                params.putCharSequence(
                        Constants.IAP_FREE_TRIAL_PERIOD,
                        skuDetailsJSON.optString("freeTrialPeriod"));

                String introductoryPriceCycles = skuDetailsJSON.optString("introductoryPriceCycles");
                if (!introductoryPriceCycles.isEmpty()) {
                    params.putCharSequence(
                            Constants.IAP_INTRO_PRICE_AMOUNT_MICROS,
                            skuDetailsJSON.optString("introductoryPriceAmountMicros"));
                    params.putCharSequence(
                            Constants.IAP_INTRO_PRICE_CYCLES,
                            introductoryPriceCycles);
                }
            }

            for (String key : extraParameter.keySet()) {
                params.putCharSequence(key, extraParameter.get(key));
            }

            return new PurchaseLoggingParameters(
                    new BigDecimal(skuDetailsJSON.getLong("price_amount_micros") / 1000000.0),
                    Currency.getInstance(skuDetailsJSON.getString("price_currency_code")),
                    params);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing in-app subscription data.", e);
            return null;
        }
    }

    private static class PurchaseLoggingParameters {
        BigDecimal purchaseAmount;
        Currency currency;
        Bundle param;

        PurchaseLoggingParameters(BigDecimal purchaseAmount,
                            Currency currency,
                            Bundle param) {
            this.purchaseAmount = purchaseAmount;
            this.currency = currency;
            this.param = param;
        }
    }

}
