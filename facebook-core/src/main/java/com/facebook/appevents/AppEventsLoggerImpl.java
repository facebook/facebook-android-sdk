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

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;
import bolts.AppLinks;

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
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class AppEventsLoggerImpl {


    // Constants
    private static final String TAG = AppEventsLoggerImpl.class.getCanonicalName();

    private static final int APP_SUPPORTS_ATTRIBUTION_ID_RECHECK_PERIOD_IN_SECONDS = 60 * 60 * 24;
    private static final int FLUSH_APP_SESSION_INFO_IN_SECONDS = 30;

    private static final String SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT =
            "_fbSourceApplicationHasBeenSet";

    private static final String PUSH_PAYLOAD_KEY = "fb_push_payload";
    private static final String PUSH_PAYLOAD_CAMPAIGN_KEY = "campaign";

    private static final String APP_EVENT_NAME_PUSH_OPENED = "fb_mobile_push_opened";
    private static final String APP_EVENT_PUSH_PARAMETER_CAMPAIGN = "fb_push_campaign";
    private static final String APP_EVENT_PUSH_PARAMETER_ACTION = "fb_push_action";

    // Instance member variables
    private final String contextName;
    private final AccessTokenAppIdPair accessTokenAppId;

    private static ScheduledThreadPoolExecutor backgroundExecutor;
    private static FlushBehavior flushBehavior = FlushBehavior.AUTO;
    private static Object staticLock = new Object();
    private static String anonymousAppDeviceGUID;
    private static String sourceApplication;
    private static boolean isOpenedByAppLink;
    private static boolean isActivateAppEventRequested;
    private static String pushNotificationsRegistrationId;

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

    static void activateApp(Context context) {
        if (ActivityLifecycleTracker.isTracking()) {
            Log.w(TAG, "activateApp events are being logged automatically. " +
                    "There's no need to call activateApp explicitly, this is safe to remove.");
            return;
        }

        FacebookSdk.sdkInitialize(context);
        activateApp(context, Utility.getMetadataApplicationId(context));
    }

    static void activateApp(Context context, String applicationId) {
        if (ActivityLifecycleTracker.isTracking()) {
            Log.w(TAG, "activateApp events are being logged automatically. " +
                    "There's no need to call activateApp explicitly, this is safe to remove.");
            return;
        }

        if (context == null || applicationId == null) {
            throw new IllegalArgumentException("Both context and applicationId must be non-null");
        }

        AnalyticsUserIDStore.initStore();
        UserDataStore.initStore();

        if ((context instanceof Activity)) {
            setSourceApplication((Activity) context);
        } else {
            // If context is not an Activity, we cannot get intent nor calling activity.
            resetSourceApplication();
            Utility.logd(TAG,
                    "To set source application the context of activateApp must be an instance of" +
                            " Activity");
        }

        // activateApp supersedes publishInstall in the public API, so we need to explicitly invoke
        // it, since the server can't reliably infer install state for all conditions of an app
        // activate.
        FacebookSdk.publishInstallAsync(context, applicationId);

        final AppEventsLoggerImpl logger = new AppEventsLoggerImpl(context, applicationId, null);
        final long eventTime = System.currentTimeMillis();
        final String sourceApplicationInfo = getSourceApplication();
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                logger.logAppSessionResumeEvent(eventTime, sourceApplicationInfo);
            }
        });
    }

    static void deactivateApp(Context context) {
        if (ActivityLifecycleTracker.isTracking()) {
            Log.w(TAG, "deactivateApp events are being logged automatically. " +
                    "There's no need to call deactivateApp, this is safe to remove.");
            return;
        }

        deactivateApp(context, Utility.getMetadataApplicationId(context));
    }

    @Deprecated
    static void deactivateApp(Context context, String applicationId) {
        if (ActivityLifecycleTracker.isTracking()) {
            Log.w(TAG, "deactivateApp events are being logged automatically. " +
                    "There's no need to call deactivateApp, this is safe to remove.");
            return;
        }

        if (context == null || applicationId == null) {
            throw new IllegalArgumentException("Both context and applicationId must be non-null");
        }

        resetSourceApplication();

        final AppEventsLoggerImpl logger = new AppEventsLoggerImpl(context, applicationId, null);
        final long eventTime = System.currentTimeMillis();
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                logger.logAppSessionSuspendEvent(eventTime);
            }
        });
    }

    private void logAppSessionResumeEvent(long eventTime, String sourceApplicationInfo) {
        PersistedAppSessionInfo.onResume(
                FacebookSdk.getApplicationContext(),
                accessTokenAppId,
                this,
                eventTime,
                sourceApplicationInfo);
    }

    private void logAppSessionSuspendEvent(long eventTime) {
        PersistedAppSessionInfo.onSuspend(
                FacebookSdk.getApplicationContext(),
                accessTokenAppId,
                this,
                eventTime);
    }

    static void initializeLib(Context context, String applicationId) {
        if (!FacebookSdk.getAutoLogAppEventsEnabled()) {
            return;
        }
        final AppEventsLoggerImpl logger = new AppEventsLoggerImpl(context, applicationId, null);
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bundle params = new Bundle();

                // internal SDK Libraries
                try {
                    Class.forName("com.facebook.core.Core");
                    params.putInt("core_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.facebook.login.Login");
                    params.putInt("login_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.facebook.share.Share");
                    params.putInt("share_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.facebook.places.Places");
                    params.putInt("places_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.facebook.messenger.Messenger");
                    params.putInt("messenger_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.facebook.applinks.AppLinks");
                    params.putInt("applinks_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.facebook.marketing.Marketing");
                    params.putInt("marketing_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.facebook.all.All");
                    params.putInt("all_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }

                //  external SDK Libraries
                try {
                    Class.forName("com.android.billingclient.api.BillingClient");
                    params.putInt("billing_client_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }
                try {
                    Class.forName("com.android.vending.billing.IInAppBillingService");
                    params.putInt("billing_service_lib_included", 1);
                } catch (ClassNotFoundException ignored) { /* no op */ }

                logger.logEventImplicitly(AnalyticsEvents.EVENT_SDK_INITIALIZE, null, params);
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

    /**
     * Source Application setters and getters
     */
    private static void setSourceApplication(Activity activity) {

        ComponentName callingApplication = activity.getCallingActivity();
        if (callingApplication != null) {
            String callingApplicationPackage = callingApplication.getPackageName();
            if (callingApplicationPackage.equals(activity.getPackageName())) {
                // open by own app.
                resetSourceApplication();
                return;
            }
            sourceApplication = callingApplicationPackage;
        }

        // Tap icon to open an app will still get the old intent if the activity was opened by an
        // intent before. Introduce an extra field in the intent to force clear the
        // sourceApplication.
        Intent openIntent = activity.getIntent();
        if (openIntent == null ||
                openIntent.getBooleanExtra(SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT, false)) {
            resetSourceApplication();
            return;
        }

        Bundle appLinkData = AppLinks.getAppLinkData(openIntent);

        if (appLinkData == null) {
            resetSourceApplication();
            return;
        }

        isOpenedByAppLink = true;

        Bundle appLinkReferrerData = appLinkData.getBundle("referer_app_link");

        if (appLinkReferrerData == null) {
            sourceApplication = null;
            return;
        }

        String appLinkReferrerPackage = appLinkReferrerData.getString("package");
        sourceApplication = appLinkReferrerPackage;

        // Mark this intent has been used to avoid use this intent again and again.
        openIntent.putExtra(SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT, true);

        return;
    }

    static void setSourceApplication(String applicationPackage, boolean openByAppLink) {
        sourceApplication = applicationPackage;
        isOpenedByAppLink = openByAppLink;
    }

    static String getSourceApplication() {
        String openType = "Unclassified";
        if (isOpenedByAppLink) {
            openType = "Applink";
        }
        if (sourceApplication != null) {
            return openType + "(" + sourceApplication + ")";
        }
        return openType;
    }

    static void resetSourceApplication() {
        sourceApplication = null;
        isOpenedByAppLink = false;
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
                            AppEventsLogger.APP_EVENT_PREFERENCES,
                            Context.MODE_PRIVATE);
                    anonymousAppDeviceGUID = preferences.getString("anonymousAppDeviceGUID", null);
                    if (anonymousAppDeviceGUID == null) {
                        // Arbitrarily prepend XZ to distinguish from device supplied identifiers.
                        anonymousAppDeviceGUID = "XZ" + UUID.randomUUID().toString();

                        context.getSharedPreferences(AppEventsLogger.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
                                .edit()
                                .putString("anonymousAppDeviceGUID", anonymousAppDeviceGUID)
                                .apply();
                    }
                }
            }
        }

        return anonymousAppDeviceGUID;
    }

    //
    // Deprecated Stuff
    //

    // Since we moved some private classes to internal classes outside the AppEventsLogger class
    // for backwards compatibility we can override the classDescriptor to resolve to the correct
    // class


    static class PersistedAppSessionInfo {
        private static final String PERSISTED_SESSION_INFO_FILENAME =
                "AppEventsLogger.persistedsessioninfo";

        private static final Object staticLock = new Object();
        private static boolean hasChanges = false;
        private static boolean isLoaded = false;
        private static Map<AccessTokenAppIdPair, FacebookTimeSpentData> appSessionInfoMap;

        private static final Runnable appSessionInfoFlushRunnable = new Runnable() {
            @Override
            public void run() {
                PersistedAppSessionInfo.saveAppSessionInformation(
                        FacebookSdk.getApplicationContext());
            }
        };

        @SuppressWarnings("unchecked")
        private static void restoreAppSessionInformation(Context context) {
            ObjectInputStream ois = null;

            synchronized (staticLock) {
                if (!isLoaded) {
                    try {
                        ois = new ObjectInputStream(
                                context.openFileInput(PERSISTED_SESSION_INFO_FILENAME));
                        appSessionInfoMap = (HashMap<AccessTokenAppIdPair, FacebookTimeSpentData>)
                                ois.readObject();
                        Logger.log(
                                LoggingBehavior.APP_EVENTS,
                                "AppEvents",
                                "App session info loaded");
                    } catch (FileNotFoundException fex) {
                    } catch (Exception e) {
                        Log.w(
                                TAG,
                                "Got unexpected exception restoring app session info: "
                                        + e.toString());
                    } finally {
                        Utility.closeQuietly(ois);
                        context.deleteFile(PERSISTED_SESSION_INFO_FILENAME);
                        if (appSessionInfoMap == null) {
                            appSessionInfoMap =
                                    new HashMap<AccessTokenAppIdPair, FacebookTimeSpentData>();
                        }
                        // Regardless of the outcome of the load, the session information cache
                        // is always deleted. Therefore, always treat the session information cache
                        // as loaded
                        isLoaded = true;
                        hasChanges = false;
                    }
                }
            }
        }

        static void saveAppSessionInformation(Context context) {
            ObjectOutputStream oos = null;

            synchronized (staticLock) {
                if (hasChanges) {
                    try {
                        oos = new ObjectOutputStream(
                                new BufferedOutputStream(
                                        context.openFileOutput(
                                                PERSISTED_SESSION_INFO_FILENAME,
                                                Context.MODE_PRIVATE)
                                )
                        );
                        oos.writeObject(appSessionInfoMap);
                        hasChanges = false;
                        Logger.log(
                                LoggingBehavior.APP_EVENTS,
                                "AppEvents",
                                "App session info saved");
                    } catch (Exception e) {
                        Log.w(
                                TAG,
                                "Got unexpected exception while writing app session info: "
                                        + e.toString());
                    } finally {
                        Utility.closeQuietly(oos);
                    }
                }
            }
        }

        static void onResume(
                Context context,
                AccessTokenAppIdPair accessTokenAppId,
                AppEventsLoggerImpl logger,
                long eventTime,
                String sourceApplicationInfo
        ) {
            synchronized (staticLock) {
                FacebookTimeSpentData timeSpentData = getTimeSpentData(context, accessTokenAppId);
                timeSpentData.onResume(logger, eventTime, sourceApplicationInfo);
                onTimeSpentDataUpdate();
            }
        }

        static void onSuspend(
                Context context,
                AccessTokenAppIdPair accessTokenAppId,
                AppEventsLoggerImpl logger,
                long eventTime
        ) {
            synchronized (staticLock) {
                FacebookTimeSpentData timeSpentData = getTimeSpentData(context, accessTokenAppId);
                timeSpentData.onSuspend(logger, eventTime);
                onTimeSpentDataUpdate();
            }
        }

        private static FacebookTimeSpentData getTimeSpentData(
                Context context,
                AccessTokenAppIdPair accessTokenAppId
        ) {
            restoreAppSessionInformation(context);
            FacebookTimeSpentData result = null;

            result = appSessionInfoMap.get(accessTokenAppId);
            if (result == null) {
                result = new FacebookTimeSpentData();
                appSessionInfoMap.put(accessTokenAppId, result);
            }

            return result;
        }

        private static void onTimeSpentDataUpdate() {
            if (!hasChanges) {
                hasChanges = true;
                backgroundExecutor.schedule(
                        appSessionInfoFlushRunnable,
                        FLUSH_APP_SESSION_INFO_IN_SECONDS,
                        TimeUnit.SECONDS);
            }
        }
    }
}
