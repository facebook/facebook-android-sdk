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

package com.facebook.appevents;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;

import com.facebook.appevents.AppEventsLogger.FlushBehavior;
import com.facebook.appevents.AppEventsLogger.ProductAvailability;
import com.facebook.appevents.AppEventsLogger.ProductCondition;
import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.internal.ActivityLifecycleTracker;
import com.facebook.appevents.internal.AutomaticAnalyticsLogger;
import com.facebook.appevents.internal.Constants;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.BundleJSONConverter;
import com.facebook.internal.FetchedAppGateKeepersManager;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class AppEventsLoggerImpl {

    // Constants
    private static final String TAG = AppEventsLoggerImpl.class.getCanonicalName();

    private static final int APP_SUPPORTS_ATTRIBUTION_ID_RECHECK_PERIOD_IN_SECONDS = 60 * 60 * 24;

    private static final String PUSH_PAYLOAD_KEY = "fb_push_payload";
    private static final String PUSH_PAYLOAD_CAMPAIGN_KEY = "campaign";

    private static final String APP_EVENT_NAME_PUSH_OPENED = "fb_mobile_push_opened";
    private static final String APP_EVENT_PUSH_PARAMETER_CAMPAIGN = "fb_push_campaign";
    private static final String APP_EVENT_PUSH_PARAMETER_ACTION = "fb_push_action";

    private static final String ACCOUNT_KIT_EVENT_NAME_PREFIX = "fb_ak";

    // Instance member variables
    private final String contextName;
    private final AccessTokenAppIdPair accessTokenAppId;

    private static ScheduledThreadPoolExecutor backgroundExecutor;
    private static FlushBehavior flushBehavior = FlushBehavior.AUTO;
    private static final Object staticLock = new Object();
    private static String anonymousAppDeviceGUID;
    private static boolean isActivateAppEventRequested;
    private static String pushNotificationsRegistrationId;

    private static final String APP_EVENT_PREFERENCES = "com.facebook.sdk.appEventPreferences";
    private static final String APP_EVENTS_KILLSWITCH = "app_events_killswitch";

    static void activateApp(Application application, String applicationId) {
        if (!FacebookSdk.isInitialized()) {
            throw new FacebookException("The Facebook sdk must be initialized before calling " +
                    "activateApp");
        }

        AnalyticsUserIDStore.initStore();
        UserDataStore.initStore();

        if (applicationId == null) {
            applicationId = FacebookSdk.getApplicationId();
        }

        // activateApp supersedes publishInstall in the public API, so we need to explicitly invoke
        // it, since the server can't reliably infer install state for all conditions of an app
        // activate.
        FacebookSdk.publishInstallAsync(application, applicationId);

        // Will do nothing in case AutoLogAppEventsEnabled is true, as we already started the
        // tracking as part of sdkInitialize() flow
        ActivityLifecycleTracker.startTracking(application, applicationId);
    }

    static void functionDEPRECATED(String extraMsg) {
        Log.w(TAG, "This function is deprecated. " + extraMsg);
    }

    static void initializeLib(final Context context, String applicationId) {
        if (!FacebookSdk.getAutoLogAppEventsEnabled()) {
            return;
        }
        final AppEventsLoggerImpl logger = new AppEventsLoggerImpl(context, applicationId, null);
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bundle params = new Bundle();

                String[] classes = {
                        // internal SDK Libraries
                        "com.facebook.core.Core",
                        "com.facebook.login.Login",
                        "com.facebook.share.Share",
                        "com.facebook.places.Places",
                        "com.facebook.messenger.Messenger",
                        "com.facebook.applinks.AppLinks",
                        "com.facebook.marketing.Marketing",
                        "com.facebook.all.All",
                        // external SDK Libraries
                        "com.android.billingclient.api.BillingClient",
                        "com.android.vending.billing.IInAppBillingService"
                };
                String[] keys = {
                        // internal SDK Libraries
                        "core_lib_included",
                        "login_lib_included",
                        "share_lib_included",
                        "places_lib_included",
                        "messenger_lib_included",
                        "applinks_lib_included",
                        "marketing_lib_included",
                        "all_lib_included",
                        // external SDK Libraries
                        "billing_client_lib_included",
                        "billing_service_lib_included"
                };

                if (classes.length != keys.length) {
                    throw new FacebookException("Number of class names and key names should match");
                }

                int bitmask = 0;

                for (int i = 0; i < classes.length; i++) {
                    String className = classes[i];
                    String keyName = keys[i];

                    try {
                        Class.forName(className);
                        params.putInt(keyName, 1);
                        bitmask |= 1 << i;
                    } catch (ClassNotFoundException ignored) { /* no op */ }
                }

                SharedPreferences preferences = context.getSharedPreferences(
                        APP_EVENT_PREFERENCES,
                        Context.MODE_PRIVATE);
                int previousBitmask = preferences.getInt("kitsBitmask", 0);
                if (previousBitmask != bitmask) {
                    preferences.edit().putInt("kitsBitmask", bitmask).apply();
                    logger.logEventImplicitly(AnalyticsEvents.EVENT_SDK_INITIALIZE, null, params);
                }
            }
        });
    }

    static FlushBehavior getFlushBehavior() {
        synchronized (staticLock) {
            return flushBehavior;
        }
    }

    static void setFlushBehavior(FlushBehavior flushBehavior) {
        synchronized (staticLock) {
            AppEventsLoggerImpl.flushBehavior = flushBehavior;
        }
    }

    void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    void logEvent(String eventName, double valueToSum) {
        logEvent(eventName, valueToSum, null);
    }

    void logEvent(String eventName, Bundle parameters) {
        logEvent(
                eventName,
                null,
                parameters,
                false,
                ActivityLifecycleTracker.getCurrentSessionGuid());
    }

    void logEvent(String eventName, double valueToSum, Bundle parameters) {
        logEvent(
                eventName,
                valueToSum,
                parameters,
                false,
                ActivityLifecycleTracker.getCurrentSessionGuid());
    }

    void logEventFromSE(String eventName) {
        Bundle parameters = new Bundle();
        parameters.putString("_is_suggested_event", "1");
        logEvent(eventName, parameters);
    }

    void logPurchase(
            BigDecimal purchaseAmount, Currency currency) {
        logPurchase(purchaseAmount, currency, null);
    }

    void logPurchase(
            BigDecimal purchaseAmount, Currency currency, Bundle parameters) {
        if (AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()) {
            Log.w(TAG, "You are logging purchase events while auto-logging of in-app purchase is " +
                    "enabled in the SDK. Make sure you don't log duplicate events");
        }
        logPurchase(purchaseAmount, currency, parameters, false);
    }

    void logPurchaseImplicitly(
            BigDecimal purchaseAmount, Currency currency, Bundle parameters) {
        logPurchase(purchaseAmount, currency, parameters, true);
    }

    void logPurchase(
            BigDecimal purchaseAmount,
            Currency currency,
            Bundle parameters,
            boolean isImplicitlyLogged) {

        if (purchaseAmount == null) {
            notifyDeveloperError("purchaseAmount cannot be null");
            return;
        } else if (currency == null) {
            notifyDeveloperError("currency cannot be null");
            return;
        }

        if (parameters == null) {
            parameters = new Bundle();
        }
        parameters.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency.getCurrencyCode());

        logEvent(
                AppEventsConstants.EVENT_NAME_PURCHASED,
                purchaseAmount.doubleValue(),
                parameters,
                isImplicitlyLogged,
                ActivityLifecycleTracker.getCurrentSessionGuid());
        eagerFlush();
    }

    void logPushNotificationOpen(Bundle payload, String action) {
        String campaignId = null;
        try {
            String payloadString = payload.getString(PUSH_PAYLOAD_KEY);
            if (Utility.isNullOrEmpty(payloadString)) {
                return; // Ignore the payload if no fb push payload is present.
            }

            JSONObject facebookPayload = new JSONObject(payloadString);
            campaignId = facebookPayload.getString(PUSH_PAYLOAD_CAMPAIGN_KEY);
        } catch (JSONException je) {
            // ignore
        }
        if (campaignId == null) {
            Logger.log(LoggingBehavior.DEVELOPER_ERRORS, TAG,
                    "Malformed payload specified for logging a push notification open.");
            return;
        }

        Bundle parameters = new Bundle();
        parameters.putString(APP_EVENT_PUSH_PARAMETER_CAMPAIGN, campaignId);
        if (action != null) {
            parameters.putString(APP_EVENT_PUSH_PARAMETER_ACTION, action);
        }
        logEvent(APP_EVENT_NAME_PUSH_OPENED, parameters);
    }

    void logProductItem(
            String itemID,
            ProductAvailability availability,
            ProductCondition condition,
            String description,
            String imageLink,
            String link,
            String title,
            BigDecimal priceAmount,
            Currency currency,
            String gtin,
            String mpn,
            String brand,
            Bundle parameters
    ) {
        if (itemID == null) {
            notifyDeveloperError("itemID cannot be null");
            return;
        } else if (availability == null) {
            notifyDeveloperError("availability cannot be null");
            return;
        } else if (condition == null) {
            notifyDeveloperError("condition cannot be null");
            return;
        } else if (description == null) {
            notifyDeveloperError("description cannot be null");
            return;
        } else if (imageLink == null) {
            notifyDeveloperError("imageLink cannot be null");
            return;
        } else if (link == null) {
            notifyDeveloperError("link cannot be null");
            return;
        } else if (title == null) {
            notifyDeveloperError("title cannot be null");
            return;
        } else if (priceAmount == null) {
            notifyDeveloperError("priceAmount cannot be null");
            return;
        } else if (currency == null) {
            notifyDeveloperError("currency cannot be null");
            return;
        } else if (gtin == null && mpn == null && brand == null) {
            notifyDeveloperError("Either gtin, mpn or brand is required");
            return;
        }

        if (parameters == null) {
            parameters = new Bundle();
        }
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_ITEM_ID, itemID);
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_AVAILABILITY, availability.name());
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_CONDITION, condition.name());
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, description);
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, imageLink);
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, link);
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, title);
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
                priceAmount.setScale(3, BigDecimal.ROUND_HALF_UP).toString());
        parameters.putString(
                Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY, currency.getCurrencyCode());
        if (gtin != null) {
            parameters.putString(Constants.EVENT_PARAM_PRODUCT_GTIN, gtin);
        }
        if (mpn != null) {
            parameters.putString(Constants.EVENT_PARAM_PRODUCT_MPN, mpn);
        }
        if (brand != null) {
            parameters.putString(Constants.EVENT_PARAM_PRODUCT_BRAND, brand);
        }

        logEvent(
                AppEventsConstants.EVENT_NAME_PRODUCT_CATALOG_UPDATE,
                parameters);
        eagerFlush();
    }

    void flush() {
        AppEventQueue.flush(FlushReason.EXPLICIT);
    }

    static void onContextStop() {
        // TODO: (v4) add onContextStop() to samples that use the logger.
        AppEventQueue.persistToDisk();
    }

    boolean isValidForAccessToken(AccessToken accessToken) {
        AccessTokenAppIdPair other = new AccessTokenAppIdPair(accessToken);
        return accessTokenAppId.equals(other);
    }

    static void setPushNotificationsRegistrationId(String registrationId) {
        synchronized (staticLock) {
            if (!Utility.stringsEqualOrEmpty(pushNotificationsRegistrationId, registrationId))
            {
                pushNotificationsRegistrationId = registrationId;

                AppEventsLoggerImpl logger = new AppEventsLoggerImpl(
                        FacebookSdk.getApplicationContext(), null, null);
                // Log implicit push token event and flush logger immediately
                logger.logEvent(AppEventsConstants.EVENT_NAME_PUSH_TOKEN_OBTAINED);
                if (AppEventsLoggerImpl.getFlushBehavior() !=
                        FlushBehavior.EXPLICIT_ONLY) {
                    logger.flush();
                }
            }
        }
    }

    static String getPushNotificationsRegistrationId() {
        synchronized (staticLock) {
            return pushNotificationsRegistrationId;
        }
    }

    static void setInstallReferrer(String referrer) {
        Context ctx = FacebookSdk.getApplicationContext();
        SharedPreferences preferences = ctx.getSharedPreferences(
                APP_EVENT_PREFERENCES,
                Context.MODE_PRIVATE);
        if (referrer != null) {
            preferences.edit().putString("install_referrer", referrer).apply();
        }
    }

    @Nullable
    static String getInstallReferrer() {
        Context ctx = FacebookSdk.getApplicationContext();
        SharedPreferences preferences = ctx.getSharedPreferences(
                APP_EVENT_PREFERENCES,
                Context.MODE_PRIVATE);
        return preferences.getString("install_referrer", null);
    }

    static void augmentWebView(WebView webView, Context context) {
        String[] parts = Build.VERSION.RELEASE.split("\\.");
        int majorRelease = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        int minorRelease = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ||
                majorRelease < 4 || (majorRelease == 4 && minorRelease <= 1)) {
            Logger.log(LoggingBehavior.DEVELOPER_ERRORS, TAG,
                    "augmentWebView is only available for Android SDK version >= 17 on devices " +
                            "running Android >= 4.2");
            return;
        }
        webView.addJavascriptInterface(new FacebookSDKJSInterface(context),
                "fbmq_" + FacebookSdk.getApplicationId());
    }

    static void updateUserProperties(
            final Bundle parameters,
            final String applicationID,
            final GraphRequest.Callback callback) {
        getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final String userID = AnalyticsUserIDStore.getUserID();
                if (userID == null || userID.isEmpty()) {
                    Logger.log(
                            LoggingBehavior.APP_EVENTS,
                            TAG,
                            "AppEventsLogger userID cannot be null or empty");
                    return;
                }

                Bundle userPropertiesParams = new Bundle();
                userPropertiesParams.putString("user_unique_id", userID);
                userPropertiesParams.putBundle("custom_data", parameters);
                // This call must be run on the background thread
                AttributionIdentifiers identifiers =
                        AttributionIdentifiers.getAttributionIdentifiers(
                                FacebookSdk.getApplicationContext());
                if (identifiers != null && identifiers.getAndroidAdvertiserId() != null) {
                    userPropertiesParams.putString(
                            "advertiser_id",
                            identifiers.getAndroidAdvertiserId());
                }

                Bundle data = new Bundle();
                try {
                    JSONObject userData = BundleJSONConverter.convertToJSON(userPropertiesParams);
                    JSONArray dataArray = new JSONArray();
                    dataArray.put(userData);

                    data.putString(
                            "data", dataArray.toString());
                } catch (JSONException ex) {
                    throw new FacebookException("Failed to construct request", ex);
                }

                GraphRequest request = new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        String.format(Locale.US, "%s/user_properties", applicationID),
                        data,
                        HttpMethod.POST,
                        callback);
                request.setSkipClientToken(true);
                request.executeAsync();
            }
        });
    }


    void logSdkEvent(String eventName, Double valueToSum, Bundle parameters) {
        if (!eventName.startsWith(ACCOUNT_KIT_EVENT_NAME_PREFIX)) {
            Log.e(TAG, "logSdkEvent is deprecated and only supports account kit for legacy, " +
                    "please use logEvent instead");
            return;
        }

        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            logEvent(
                    eventName,
                    valueToSum,
                    parameters,
                    true,
                    ActivityLifecycleTracker.getCurrentSessionGuid());
        }
    }

    /**
     * Returns the app ID this logger was configured to log to.
     *
     * @return the Facebook app ID
     */
    public String getApplicationId() {
        return accessTokenAppId.getApplicationId();
    }

    AppEventsLoggerImpl(
            Context context,
            String applicationId,
            AccessToken accessToken) {
        this(
                Utility.getActivityName(context),
                applicationId,
                accessToken);
    }

    AppEventsLoggerImpl(
            String activityName,
            String applicationId,
            AccessToken accessToken) {
        Validate.sdkInitialized();
        this.contextName = activityName;

        if (accessToken == null) {
            accessToken = AccessToken.getCurrentAccessToken();
        }

        // If we have a session and the appId passed is null or matches the session's app ID:
        if (AccessToken.isCurrentAccessTokenActive() &&
                (applicationId == null || applicationId.equals(accessToken.getApplicationId()))
                ) {
            accessTokenAppId = new AccessTokenAppIdPair(accessToken);
        } else {
            // If no app ID passed, get it from the manifest:
            if (applicationId == null) {
                applicationId = Utility.getMetadataApplicationId(
                        FacebookSdk.getApplicationContext());
            }
            accessTokenAppId = new AccessTokenAppIdPair(null, applicationId);
        }

        initializeTimersIfNeeded();
    }

    private static void initializeTimersIfNeeded() {
        synchronized (staticLock) {
            if (backgroundExecutor != null) {
                return;
            }
            // Having single runner thread enforces ordered execution of tasks,
            // which matters in some cases e.g. making sure user id is set before
            // trying to update user properties for a given id
            backgroundExecutor = new ScheduledThreadPoolExecutor(1);
        }

        final Runnable attributionRecheckRunnable = new Runnable() {
            @Override
            public void run() {
                Set<String> applicationIds = new HashSet<>();
                for (AccessTokenAppIdPair accessTokenAppId : AppEventQueue.getKeySet()) {
                    applicationIds.add(accessTokenAppId.getApplicationId());
                }

                for (String applicationId : applicationIds) {
                    FetchedAppSettingsManager.queryAppSettings(applicationId, true);
                }
            }
        };

        backgroundExecutor.scheduleAtFixedRate(
                attributionRecheckRunnable,
                0,
                APP_SUPPORTS_ATTRIBUTION_ID_RECHECK_PERIOD_IN_SECONDS,
                TimeUnit.SECONDS
        );
    }

    void logEventImplicitly(String eventName, Double valueToSum, Bundle parameters) {
        logEvent(
                eventName,
                valueToSum,
                parameters,
                true,
                ActivityLifecycleTracker.getCurrentSessionGuid());
    }

    void logEventImplicitly(String eventName,
                            BigDecimal purchaseAmount,
                            Currency currency,
                            Bundle parameters) {
        if (purchaseAmount == null || currency == null) {
            Utility.logd(TAG, "purchaseAmount and currency cannot be null");
            return;
        }

        if (parameters == null) {
            parameters = new Bundle();
        }
        parameters.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency.getCurrencyCode());
        logEvent(
                eventName,
                purchaseAmount.doubleValue(),
                parameters,
                true,
                ActivityLifecycleTracker.getCurrentSessionGuid());
    }

    void logEvent(
            String eventName,
            Double valueToSum,
            Bundle parameters,
            boolean isImplicitlyLogged,
            @Nullable final UUID currentSessionId) {
        if (eventName == null || eventName.isEmpty()) {
            return;
        }

        // Kill events if kill-switch is enabled
        if (FetchedAppGateKeepersManager.getGateKeeperForKey(
                APP_EVENTS_KILLSWITCH,
                FacebookSdk.getApplicationId(),
                false)) {
            Logger.log(LoggingBehavior.APP_EVENTS, "AppEvents",
                    "KillSwitch is enabled and fail to log app event: %s", eventName);
            return;
        }

        try {
            AppEvent event = new AppEvent(
                    this.contextName,
                    eventName,
                    valueToSum,
                    parameters,
                    isImplicitlyLogged,
                    ActivityLifecycleTracker.isInBackground(),
                    currentSessionId);
            logEvent(event, accessTokenAppId);
        } catch (JSONException jsonException) {
            // If any of the above failed, just consider this an illegal event.
            Logger.log(LoggingBehavior.APP_EVENTS, "AppEvents",
                    "JSON encoding for app event failed: '%s'", jsonException.toString());

        } catch (FacebookException e) {
            // If any of the above failed, just consider this an illegal event.
            Logger.log(LoggingBehavior.APP_EVENTS, "AppEvents",
                    "Invalid app event: %s", e.toString());
        }

    }

    private static void logEvent(final AppEvent event,
                                 final AccessTokenAppIdPair accessTokenAppId) {
        AppEventQueue.add(accessTokenAppId, event);

        // Make sure Activated_App is always before other app events
        if (!event.getIsImplicit() && !isActivateAppEventRequested) {
            if (event.getName().equals(AppEventsConstants.EVENT_NAME_ACTIVATED_APP)) {
                isActivateAppEventRequested = true;
            } else {
                Logger.log(LoggingBehavior.APP_EVENTS, "AppEvents",
                        "Warning: Please call AppEventsLogger.activateApp(...)" +
                                "from the long-lived activity's onResume() method" +
                                "before logging other app events."
                );
            }
        }
    }

    static void eagerFlush() {
        if (getFlushBehavior() != FlushBehavior.EXPLICIT_ONLY) {
            AppEventQueue.flush(FlushReason.EAGER_FLUSHING_EVENT);
        }
    }

    /**
     * Invoke this method, rather than throwing an Exception, for situations where user/server input
     * might reasonably cause this to occur, and thus don't want an exception thrown at production
     * time, but do want logging notification.
     */
    private static void notifyDeveloperError(String message) {
        Logger.log(LoggingBehavior.DEVELOPER_ERRORS, "AppEvents", message);
    }

    static Executor getAnalyticsExecutor() {
        if (backgroundExecutor == null) {
            initializeTimersIfNeeded();
        }

        return backgroundExecutor;
    }

    static String getAnonymousAppDeviceGUID(Context context) {
        if (anonymousAppDeviceGUID == null) {
            synchronized (staticLock) {
                if (anonymousAppDeviceGUID == null) {

                    SharedPreferences preferences = context.getSharedPreferences(
                            APP_EVENT_PREFERENCES,
                            Context.MODE_PRIVATE);
                    anonymousAppDeviceGUID = preferences.getString("anonymousAppDeviceGUID", null);
                    if (anonymousAppDeviceGUID == null) {
                        // Arbitrarily prepend XZ to distinguish from device supplied identifiers.
                        anonymousAppDeviceGUID = "XZ" + UUID.randomUUID().toString();

                        context.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
                                .edit()
                                .putString("anonymousAppDeviceGUID", anonymousAppDeviceGUID)
                                .apply();
                    }
                }
            }
        }

        return anonymousAppDeviceGUID;
    }
}
