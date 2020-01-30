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

package com.facebook;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Base64;
import android.util.Log;

import com.facebook.appevents.AppEventsLogger;
import com.facebook.appevents.AppEventsManager;
import com.facebook.appevents.internal.ActivityLifecycleTracker;
import com.facebook.core.BuildConfig;
import com.facebook.appevents.internal.AppEventsLoggerUtility;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.LockOnGetVariable;
import com.facebook.internal.BoltsMeasurementEventListener;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.internal.instrument.InstrumentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class allows some customization of Facebook SDK behavior.
 */
public final class FacebookSdk {
    private static final String TAG = FacebookSdk.class.getCanonicalName();

    private static final HashSet<LoggingBehavior> loggingBehaviors =
            new HashSet<LoggingBehavior>(Arrays.asList(LoggingBehavior.DEVELOPER_ERRORS));
    private static final int DEFAULT_CALLBACK_REQUEST_CODE_OFFSET = 0xface;

    private static Executor executor;
    private static volatile String applicationId;
    private static volatile @Nullable String applicationName;
    private static volatile String appClientToken;
    private static volatile Boolean codelessDebugLogEnabled;
    private static final String FACEBOOK_COM = "facebook.com";
    private static final String FB_GG = "fb.gg";
    private static volatile String facebookDomain = FACEBOOK_COM;
    private static AtomicLong onProgressThreshold = new AtomicLong(65536);
    private static volatile boolean isDebugEnabled = BuildConfig.DEBUG;
    private static boolean isLegacyTokenUpgradeSupported = false;
    private static LockOnGetVariable<File> cacheDir;
    private static Context applicationContext;
    private static int callbackRequestCodeOffset = DEFAULT_CALLBACK_REQUEST_CODE_OFFSET;
    private static final Object LOCK = new Object();
    private static String graphApiVersion = ServerProtocol.getDefaultAPIVersion();
    public static boolean hasCustomTabsUpdate = false;

    private static final int MAX_REQUEST_CODE_RANGE = 100;

    private static final String ATTRIBUTION_PREFERENCES = "com.facebook.sdk.attributionTracking";
    private static final String PUBLISH_ACTIVITY_PATH = "%s/activities";

    static final String CALLBACK_OFFSET_CHANGED_AFTER_INIT =
            "The callback request code offset can't be updated once the SDK is initialized. " +
            "Call FacebookSdk.setCallbackRequestCodeOffset inside your Application.onCreate method";

    static final String CALLBACK_OFFSET_NEGATIVE =
            "The callback request code offset can't be negative.";

    /**
     * The key for AppEvent perfernece setting.
     */
    public static final String APP_EVENT_PREFERENCES = "com.facebook.sdk.appEventPreferences";

    /**
     * The key for the application ID in the Android manifest.
     */
    public static final String APPLICATION_ID_PROPERTY = "com.facebook.sdk.ApplicationId";

    /**
     * The key for the application name in the Android manifest.
     */
    public static final String APPLICATION_NAME_PROPERTY = "com.facebook.sdk.ApplicationName";

    /**
     * The key for the client token in the Android manifest.
     */
    public static final String CLIENT_TOKEN_PROPERTY = "com.facebook.sdk.ClientToken";

    /**
     * The key for the web dialog theme in the Android manifest.
     */
    public static final String WEB_DIALOG_THEME = "com.facebook.sdk.WebDialogTheme";

    /**
     * The key for the auto init SDK in the Android manifest.
     */
    public static final String AUTO_INIT_ENABLED_PROPERTY =
            "com.facebook.sdk.AutoInitEnabled";

    /**
     * The key for the auto logging app events in the Android manifest.
     */
    public static final String AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY =
            "com.facebook.sdk.AutoLogAppEventsEnabled";

    /**
     * The key for the auto log codeless in the Android manifest.
     */
    public static final String CODELESS_DEBUG_LOG_ENABLED_PROPERTY =
            "com.facebook.sdk.CodelessDebugLogEnabled";

    /**
     * The key for the advertiserID collection in the Android manifest.
     */
    public static final String ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY =
            "com.facebook.sdk.AdvertiserIDCollectionEnabled";

    /**
     * The key for the callback off set in the Android manifest.
     */
    public static final String CALLBACK_OFFSET_PROPERTY = "com.facebook.sdk.CallbackOffset";

    private static Boolean sdkInitialized = false;
    private static Boolean sdkFullyInitialized = false;

    /**
     * This function initializes the Facebook SDK. This function is called automatically on app
     * start up if the proper entries are listed in the AndroidManifest, such as the facebook
     * app id. This method can be called manually if needed.
     * The behavior of Facebook SDK functions are undetermined if this function is not called.
     * It should be called as early as possible.
     * As part of SDK initialization basic auto logging of app events will occur, this can be
     * controlled via 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
     * @param applicationContext The application context
     * @param callbackRequestCodeOffset The request code offset that Facebook activities will be
     *                                  called with. Please do not use the range between the
     *                                  value you set and another 100 entries after it in your
     *                                  other requests.
     * @Deprecated {@link #sdkInitialize(Context)} and
     * {@link AppEventsLogger#activateApp(Application)} are called automatically on application
     * start. Automatic event logging from 'activateApp' can be controlled via the
     * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting. The callbackRequestCodeOffset
     * can be set in the AndroidManifest as a meta data entry with the name
     * {@link #CALLBACK_OFFSET_PROPERTY}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static synchronized void sdkInitialize(
            Context applicationContext,
            int callbackRequestCodeOffset) {
        sdkInitialize(applicationContext, callbackRequestCodeOffset, null);
    }

    /**
     * This function initializes the Facebook SDK. This function is called automatically on app
     * start up if the proper entries are listed in the AndroidManifest, such as the facebook
     * app id. This method can be called manually if needed.
     * The behavior of Facebook SDK functions are undetermined if this function is not called.
     * It should be called as early as possible.
     * As part of SDK initialization basic auto logging of app events will occur, this can be
     * controlled via 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
     * @param applicationContext The application context
     * @param callbackRequestCodeOffset The request code offset that Facebook activities will be
     *                                  called with. Please do not use the range between the
     *                                  value you set and another 100 entries after it in your
     *                                  other requests.
     * @param callback A callback called when initialize finishes. This will be called even if the
     *                 sdk is already initialized.
     * @Deprecated {@link #sdkInitialize(Context)} and
     * {@link AppEventsLogger#activateApp(Application)} are called automatically on application
     * start. Automatic event logging from 'activateApp' can be controlled via the
     * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting. The callbackRequestCodeOffset
     * can be set in the AndroidManifest as a meta data entry with the name
     * {@link #CALLBACK_OFFSET_PROPERTY}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static synchronized void sdkInitialize(
            Context applicationContext,
            int callbackRequestCodeOffset,
            final InitializeCallback callback) {
        if (sdkInitialized && callbackRequestCodeOffset != FacebookSdk.callbackRequestCodeOffset) {
            throw new FacebookException(CALLBACK_OFFSET_CHANGED_AFTER_INIT);
        }
        if (callbackRequestCodeOffset < 0) {
            throw new FacebookException(CALLBACK_OFFSET_NEGATIVE);
        }

        FacebookSdk.callbackRequestCodeOffset = callbackRequestCodeOffset;
        sdkInitialize(applicationContext, callback);
    }

    /**
     * This function initializes the Facebook SDK. This function is called automatically on app
     * start up if the proper entries are listed in the AndroidManifest, such as the facebook
     * app id. This method can bee called manually if needed.
     * The behavior of Facebook SDK functions are undetermined if this function is not called.
     * It should be called as early as possible.
     * As part of SDK initialization basic auto logging of app events will occur, this can be
     * controlled via 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
     * @param applicationContext The application context
     * @Deprecated {@link #sdkInitialize(Context)} and
     * {@link AppEventsLogger#activateApp(Application)} are called automatically on application
     * start. Automatic event logging from 'activateApp' can be controlled via the
     * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static synchronized void sdkInitialize(Context applicationContext) {
        FacebookSdk.sdkInitialize(applicationContext, null);
    }

    /**
     * This function initializes the Facebook SDK. This function is called automatically on app
     * start up if the proper entries are listed in the AndroidManifest, such as the facebook
     * app id. This method can bee called manually if needed.
     * The behavior of Facebook SDK functions are undetermined if this function is not called.
     * It should be called as early as possible.
     * As part of SDK initialization basic auto logging of app events will occur, this can be
     * controlled via 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting
     * @param applicationContext The application context
     * @param callback A callback called when initialize finishes. This will be called even if the
     *                 sdk is already initialized.
     * @Deprecated {@link #sdkInitialize(Context)} and
     * {@link AppEventsLogger#activateApp(Application)} are called automatically on application
     * start. Automatic event logging from 'activateApp' can be controlled via the
     * 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest setting.
     */
    @Deprecated
    public static synchronized void sdkInitialize(
            final Context applicationContext,
            final InitializeCallback callback) {
        if (sdkInitialized) {
            if (callback != null) {
                callback.onInitialized();
            }
            return;
        }

        Validate.notNull(applicationContext, "applicationContext");

        // Don't throw for these validations here, just log an error. We'll throw when we actually
        // need them
        Validate.hasFacebookActivity(applicationContext, false);
        Validate.hasInternetPermissions(applicationContext, false);

        FacebookSdk.applicationContext = applicationContext.getApplicationContext();

        // Make sure anon_id doesn't get overridden
        AppEventsLogger.getAnonymousAppDeviceGUID(applicationContext);

        // Make sure we've loaded default settings if we haven't already.
        FacebookSdk.loadDefaultsFromMetadata(FacebookSdk.applicationContext);

        // We should have an application id by now if not throw
        if (Utility.isNullOrEmpty(applicationId)) {
            throw new FacebookException("A valid Facebook app id must be set in the " +
                    "AndroidManifest.xml or set by calling FacebookSdk.setApplicationId " +
                    "before initializing the sdk.");
        }

        // Set sdkInitialized to true now so the bellow async tasks don't throw not initialized
        // exceptions.
        sdkInitialized = true;

        // Set sdkFullyInitialzed if auto init enabled.
        if (getAutoInitEnabled()) {
            fullyInitialize();
        }

        // Register ActivityLifecycleTracker callbacks now, so will log activate app event properly
        if ((FacebookSdk.applicationContext instanceof Application)
                && UserSettingsManager.getAutoLogAppEventsEnabled()) {
            ActivityLifecycleTracker.startTracking(
                    (Application) FacebookSdk.applicationContext,
                    applicationId
            );
        }

        // Load app settings from network so that dialog configs are available
        FetchedAppSettingsManager.loadAppSettingsAsync();

        // Fetch available protocol versions from the apps on the device
        NativeProtocol.updateAllAvailableProtocolVersionsAsync();

        UserSettingsManager.logIfAutoAppLinkEnabled();

        BoltsMeasurementEventListener.getInstance(FacebookSdk.applicationContext);

        cacheDir = new LockOnGetVariable<File>(
                new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return FacebookSdk.applicationContext.getCacheDir();
                    }
                });

        FeatureManager.checkFeature(FeatureManager.Feature.Instrument,
                new FeatureManager.Callback() {
            @Override
            public void onCompleted(boolean enabled) {
                if (enabled) {
                    InstrumentManager.start();
                }
            }
        });

        FeatureManager.checkFeature(FeatureManager.Feature.AppEvents,
                new FeatureManager.Callback() {
                    @Override
                    public void onCompleted(boolean enabled) {
                        if (enabled) {
                            AppEventsManager.start();
                        }
                    }
                });

        FeatureManager.checkFeature(FeatureManager.Feature.ChromeCustomTabsUpdate,
                new FeatureManager.Callback() {
                    @Override
                    public void onCompleted(boolean enabled) {
                        if (enabled) {
                            hasCustomTabsUpdate = true;
                        }
                    }
                });

        FutureTask<Void> futureTask =
                new FutureTask<>(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        AccessTokenManager.getInstance().loadCurrentAccessToken();
                        ProfileManager.getInstance().loadCurrentProfile();
                        if (AccessToken.isCurrentAccessTokenActive() &&
                                Profile.getCurrentProfile() == null) {
                            // Access token and profile went out of sync due to a network or caching
                            // issue, retry
                            Profile.fetchProfileForCurrentAccessToken();
                        }

                        if (callback != null) {
                            callback.onInitialized();
                        }

                        AppEventsLogger.initializeLib(
                                FacebookSdk.applicationContext,
                                applicationId);

                        // Flush any app events that might have been persisted during last run.
                        AppEventsLogger.newLogger(
                                applicationContext.getApplicationContext()).flush();

                        return null;
                    }
                });
        getExecutor().execute(futureTask);
    }

    /**
     * Indicates whether the Facebook SDK has been initialized.
     * @return true if initialized, false if not
     */
    public static synchronized boolean isInitialized() {
        return sdkInitialized;
    }

    /**
     * Indicates whether the Facebook SDK has been fully initialized.
     *
     * Facebook SDK won't work before fully initialized.
     *
     * @return true if fully initialized, false if not
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static synchronized boolean isFullyInitialized() {
        return sdkFullyInitialized;
    }

    /**
     * Mark Facebook SDK fully intialized to make it works as expected.
     */
    public static void fullyInitialize() {
        sdkFullyInitialized = true;
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Returns the types of extended logging that are currently enabled.
     *
     * @return a set containing enabled logging behaviors
     */
    public static Set<LoggingBehavior> getLoggingBehaviors() {
        synchronized (loggingBehaviors) {
            return Collections.unmodifiableSet(new HashSet<LoggingBehavior>(loggingBehaviors));
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Enables a particular extended logging in the SDK.
     *
     * @param behavior
     *          The LoggingBehavior to enable
     */
    public static void addLoggingBehavior(LoggingBehavior behavior) {
        synchronized (loggingBehaviors) {
            loggingBehaviors.add(behavior);
            updateGraphDebugBehavior();
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Disables a particular extended logging behavior in the SDK.
     *
     * @param behavior
     *          The LoggingBehavior to disable
     */
    public static void removeLoggingBehavior(LoggingBehavior behavior) {
        synchronized (loggingBehaviors) {
            loggingBehaviors.remove(behavior);
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Disables all extended logging behaviors.
     */
    public static void clearLoggingBehaviors() {
        synchronized (loggingBehaviors) {
            loggingBehaviors.clear();
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Checks if a particular extended logging behavior is enabled.
     *
     * @param behavior
     *          The LoggingBehavior to check
     * @return whether behavior is enabled
     */
    public static boolean isLoggingBehaviorEnabled(LoggingBehavior behavior) {
        synchronized (loggingBehaviors) {
            return FacebookSdk.isDebugEnabled() && loggingBehaviors.contains(behavior);
        }
    }

    /**
     * Indicates if we are in debug mode.
     */
    public static boolean isDebugEnabled() {
        return isDebugEnabled;
    }

    /**
     * Used to enable or disable logging, and other debug features. Defaults to BuildConfig.DEBUG.
     * @param enabled Debug features (like logging) are enabled if true, disabled if false.
     */
    public static void setIsDebugEnabled(boolean enabled) {
        isDebugEnabled = enabled;
    }

    /**
     * Indicates if the SDK should fallback and read the legacy token. This is turned off by default
     * for performance.
     * @return if the legacy token upgrade is supported.
     */
    public static boolean isLegacyTokenUpgradeSupported() {
        return isLegacyTokenUpgradeSupported;
    }

    private static void updateGraphDebugBehavior() {
        if (loggingBehaviors.contains(LoggingBehavior.GRAPH_API_DEBUG_INFO)
           && !loggingBehaviors.contains(LoggingBehavior.GRAPH_API_DEBUG_WARNING)) {
            loggingBehaviors.add(LoggingBehavior.GRAPH_API_DEBUG_WARNING);
        }
    }

    /**
     * Setter for legacy token upgrade.
     * @param supported True if upgrade should be supported.
     */
    public static void setLegacyTokenUpgradeSupported(boolean supported) {
        isLegacyTokenUpgradeSupported = supported;
    }

    /**
     * Returns the Executor used by the SDK for non-AsyncTask background work.
     *
     * By default this uses AsyncTask Executor via reflection if the API level is high enough.
     * Otherwise this creates a new Executor with defaults similar to those used in AsyncTask.
     *
     * @return an Executor used by the SDK.  This will never be null.
     */
    public static Executor getExecutor() {
        synchronized (LOCK) {
            if (FacebookSdk.executor == null) {
                FacebookSdk.executor = AsyncTask.THREAD_POOL_EXECUTOR;
            }
        }
        return FacebookSdk.executor;
    }

    /**
     * Sets the Executor used by the SDK for non-AsyncTask background work.
     *
     * @param executor
     *          the Executor to use; must not be null.
     */
    public static void setExecutor(Executor executor) {
        Validate.notNull(executor, "executor");
        synchronized (LOCK) {
            FacebookSdk.executor = executor;
        }
    }

    /**
     * Gets the base Facebook domain to use when making Web requests; in production code this will
     * normally be "facebook.com". However certain Access Tokens are meant to be used with other
     * domains. Currently gaming -> fb.gg
     *
     * This checks the current Access Token (if any) and returns the correct domain to use.
     *
     * @return the Facebook domain
     */
    public static String getFacebookDomain() {
        AccessToken currentToken = AccessToken.getCurrentAccessToken();
        String tokenGraphDomain = null;
        String graphDomain;

        if (currentToken != null) {
            tokenGraphDomain = currentToken.getGraphDomain();
        }

        if (tokenGraphDomain == null) {
            graphDomain = facebookDomain;
        } else if (tokenGraphDomain.equals("gaming")) {
            graphDomain = facebookDomain.replace(FACEBOOK_COM, FB_GG);
        } else {
            graphDomain = facebookDomain;
        }

        return graphDomain;
    }

    /**
     * Sets the base Facebook domain to use when making Web requests. This defaults to
     * "facebook.com", but may be overridden to, e.g., "beta.facebook.com" to direct requests at a
     * different domain. This method should never be called from production code.
     *
     * @param facebookDomain the base domain to use instead of "facebook.com"
     */
    public static void setFacebookDomain(String facebookDomain) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "WARNING: Calling setFacebookDomain from non-DEBUG code.");
        }

        FacebookSdk.facebookDomain = facebookDomain;
    }

    /**
     * The getter for the context of the current application.
     * @return The context of the current application.
     */
    public static Context getApplicationContext() {
        Validate.sdkInitialized();
        return applicationContext;
    }

    /**
     * Sets the Graph API version to use when making Graph requests. This defaults to the latest
     * Graph API version at the time when the Facebook SDK is shipped.
     *
     * @param graphApiVersion the Graph API version, it should be of the form
     * ServerProtocol.getDefaultAPIVersion()
     */
    public static void setGraphApiVersion(String graphApiVersion) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "WARNING: Calling setGraphApiVersion from non-DEBUG code.");
        }

        if (!Utility.isNullOrEmpty(graphApiVersion) &&
                !FacebookSdk.graphApiVersion.equals(graphApiVersion)) {
            FacebookSdk.graphApiVersion = graphApiVersion;
        }
    }

    /**
     * Returns the Graph API version to use when making Graph requests. This defaults to the latest
     * Graph API version at the time when the Facebook SDK is shipped.
     *
     * @return the Graph API version to use.
     */
    public static String getGraphApiVersion() {
        Utility.logd(TAG, String.format("getGraphApiVersion: %s", graphApiVersion));
        return graphApiVersion;
    }

    /**
     * This method is public in order to be used by app events, please don't use directly.
     * @param context       The application context.
     * @param applicationId The application id.
     */
    public static void publishInstallAsync(final Context context, final String applicationId) {
        // grab the application context ahead of time, since we will return to the caller
        // immediately.
        final Context applicationContext = context.getApplicationContext();
        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                FacebookSdk.publishInstallAndWaitForResponse(applicationContext, applicationId);
            }
        });
    }

    static void publishInstallAndWaitForResponse(
            final Context context,
            final String applicationId) {
        try {
            if (context == null || applicationId == null) {
                throw new IllegalArgumentException("Both context and applicationId must be non-null");
            }
            AttributionIdentifiers identifiers = AttributionIdentifiers.getAttributionIdentifiers(context);
            SharedPreferences preferences = context.getSharedPreferences(ATTRIBUTION_PREFERENCES, Context.MODE_PRIVATE);
            String pingKey = applicationId+"ping";
            long lastPing = preferences.getLong(pingKey, 0);

            JSONObject publishParams;
            try {
                publishParams = AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
                        AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
                        identifiers,
                        AppEventsLogger.getAnonymousAppDeviceGUID(context),
                        getLimitEventAndDataUsage(context),
                        context);
            } catch (JSONException e) {
                throw new FacebookException("An error occurred while publishing install.", e);
            }

            String publishUrl = String.format(PUBLISH_ACTIVITY_PATH, applicationId);
            GraphRequest publishRequest = GraphRequest.newPostRequest(null, publishUrl, publishParams, null);

            if (lastPing == 0) {
                // send install event only if have not sent before
                GraphResponse publishResponse = publishRequest.executeAndWait();

                if (publishResponse.getError() == null) {
                    // denote success since there is no error in response of the post.
                    SharedPreferences.Editor editor = preferences.edit();
                    lastPing = System.currentTimeMillis();
                    editor.putLong(pingKey, lastPing);
                    editor.apply();
                }
            }
        } catch (Exception e) {
            // if there was an error, fall through to the failure case.
            Utility.logd("Facebook-publish", e);
        }
    }

    /**
     * Returns the current version of the Facebook SDK for Android as a string.
     *
     * @return the current version of the SDK
     */
    public static String getSdkVersion() {
        return FacebookSdkVersion.BUILD;
    }

    /**
     * Returns whether data such as those generated through AppEventsLogger and sent to Facebook
     * should be restricted from being used for purposes other than analytics and conversions, such
     * as targeting ads to this user.  Defaults to false.  This value is stored on the device and
     * persists across app launches.
     *
     * @param context  Used to read the value.
     */
    public static boolean getLimitEventAndDataUsage(Context context) {
        Validate.sdkInitialized();
        SharedPreferences preferences = context.getSharedPreferences(
                APP_EVENT_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getBoolean("limitEventUsage", false);
    }

    /**
     * Sets whether data such as those generated through AppEventsLogger and sent to Facebook should
     * be restricted from being used for purposes other than analytics and conversions, such as
     * targeting ads to this user.  Defaults to false.  This value is stored on the device and
     * persists across app launches.  Changes to this setting will apply to app events currently
     * queued to be flushed.
     *
     * @param context Used to persist this value across app runs.
     */
    public static void setLimitEventAndDataUsage(Context context, boolean limitEventUsage) {
        context.getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("limitEventUsage", limitEventUsage)
            .apply();
    }

    /**
     * Gets the threshold used to report progress on requests.
     */
    public static long getOnProgressThreshold() {
        Validate.sdkInitialized();
        return onProgressThreshold.get();
    }

    /**
     * Sets the threshold used to report progress on requests. Note that the value will be read when
     * the request is started and cannot be changed during a request (or batch) execution.
     *
     * @param threshold The number of bytes progressed to force a callback.
     */
    public static void setOnProgressThreshold(long threshold) {
        onProgressThreshold.set(threshold);
    }

    // Package private for testing only
    static void loadDefaultsFromMetadata(Context context) {
        if (context == null) {
            return;
        }

        ApplicationInfo ai = null;
        try {
            ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }

        if (ai == null || ai.metaData == null) {
            return;
        }

        if (applicationId == null) {
            Object appId = ai.metaData.get(APPLICATION_ID_PROPERTY);
            if (appId instanceof String) {
                String appIdString = (String) appId;
                if (appIdString.toLowerCase(Locale.ROOT).startsWith("fb")) {
                    applicationId = appIdString.substring(2);
                } else {
                    applicationId = appIdString;
                }
            } else if (appId instanceof Integer) {
                throw new FacebookException(
                        "App Ids cannot be directly placed in the manifest." +
                        "They must be prefixed by 'fb' or be placed in the string resource file.");
            }
        }

        if (applicationName == null) {
            applicationName = ai.metaData.getString(APPLICATION_NAME_PROPERTY);
        }

        if (appClientToken == null) {
            appClientToken = ai.metaData.getString(CLIENT_TOKEN_PROPERTY);
        }

        if (callbackRequestCodeOffset == DEFAULT_CALLBACK_REQUEST_CODE_OFFSET) {
            callbackRequestCodeOffset = ai.metaData.getInt(
                    CALLBACK_OFFSET_PROPERTY,
                    DEFAULT_CALLBACK_REQUEST_CODE_OFFSET);
        }

        if (codelessDebugLogEnabled == null) {
            codelessDebugLogEnabled = ai.metaData.getBoolean(
                    CODELESS_DEBUG_LOG_ENABLED_PROPERTY,
                    false);
        }
    }

    /**
     * Internal call please don't use directly.
     * @param context The application context.
     * @return The application signature.
     */
    public static String getApplicationSignature(Context context) {
        Validate.sdkInitialized();
        if (context == null) {
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return null;
        }

        String packageName = context.getPackageName();
        PackageInfo pInfo;
        try {
            pInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }

        Signature[] signatures = pInfo.signatures;
        if (signatures == null || signatures.length == 0) {
            return null;
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        md.update(pInfo.signatures[0].toByteArray());
        return Base64.encodeToString(md.digest(),  Base64.URL_SAFE | Base64.NO_PADDING);
    }

    /**
     * Gets the Facebook application ID for the current app. This should only be called after the
     * SDK has been initialized by calling FacebookSdk.sdkInitialize().
     *
     * @return the application ID
     */
    public static String getApplicationId() {
        Validate.sdkInitialized();
        return applicationId;
    }

    /**
     * Sets the Facebook application ID for the current app.
     * @param applicationId the application ID
     */
    public static void setApplicationId(String applicationId) {
        FacebookSdk.applicationId = applicationId;
    }

    /**
     * Gets the Facebook application name of the current app. This should only be called after the
     * SDK has been initialized by calling FacebookSdk.sdkInitialize().
     *
     * @return the application name
     */
    @Nullable
    public static String getApplicationName() {
        Validate.sdkInitialized();
        return applicationName;
    }

    /**
     * Sets the Facebook application name for the current app.
     * @param applicationName the application name
     */
    public static void setApplicationName(String applicationName) {
        FacebookSdk.applicationName = applicationName;
    }

    /**
     * Gets the client token for the current app. This will be null unless explicitly set or unless
     * loadDefaultsFromMetadata has been called.
     * @return the client token
     */
    public static String getClientToken() {
        Validate.sdkInitialized();
        return appClientToken;
    }

    /**
     * Sets the Facebook client token for the current app.
     * @param clientToken the client token
     */
    public static void setClientToken(String clientToken) {
        appClientToken = clientToken;
    }

    /**
     * @return the auto init SDK flag for the application
     */
    public static boolean getAutoInitEnabled() {
        return UserSettingsManager.getAutoInitEnabled();
    }

    /**
     * Sets the auto init SDK flag for the application
     * @param flag true or false
     *
     * When flag is false, SDK is not fully initialized.
     */
    public static void setAutoInitEnabled(boolean flag) {
        UserSettingsManager.setAutoInitEnabled(flag);
        if (flag) {
            FacebookSdk.fullyInitialize();
        }
    }

    /**
     * Gets the flag used by {@link com.facebook.appevents.AppEventsLogger}
     * @return the auto logging events flag for the application
     */
    public static boolean getAutoLogAppEventsEnabled() {
        return UserSettingsManager.getAutoLogAppEventsEnabled();
    }

    /**
     * Sets the auto logging events flag for the application
     * {@link com.facebook.appevents.AppEventsLogger}
     * @param flag true or false
     *
     * When flag is false, events will not be logged, see
     * {@link com.facebook.appevents.internal.AutomaticAnalyticsLogger}
     */
    public static void setAutoLogAppEventsEnabled(boolean flag) {
        UserSettingsManager.setAutoLogAppEventsEnabled(flag);
        if (flag) {
            ActivityLifecycleTracker.startTracking(
                    (Application) FacebookSdk.applicationContext,
                    applicationId
            );
        }
    }

    /**
     * @return the codeless debug flag for the application
     */
    public static boolean getCodelessDebugLogEnabled() {
        Validate.sdkInitialized();
        return codelessDebugLogEnabled;
    }

    /**
     * @return the codeless enabled flag for the application
     */
    public static boolean getCodelessSetupEnabled() {
        return UserSettingsManager.getCodelessSetupEnabled();
    }

    /**
     * Sets the advertiserID collection flag for the application
     * @param flag true or false
     */
    public static void setAdvertiserIDCollectionEnabled(boolean flag) {
        UserSettingsManager.setAdvertiserIDCollectionEnabled(flag);
    }

    /**
     * @return the advertiserID collection flag for the application
     */
    public static boolean getAdvertiserIDCollectionEnabled() {
        return UserSettingsManager.getAdvertiserIDCollectionEnabled();
    }

    /**
     * Sets the codeless debug flag for the application
     * @param flag true or false
     */
    public static void setCodelessDebugLogEnabled(boolean flag) {
        codelessDebugLogEnabled = flag;
    }

    /**
     * Gets the cache directory to use for caching responses, etc. The default will be the value
     * returned by Context.getCacheDir() when the SDK was initialized, but it can be overridden.
     *
     * @return the cache directory
     */
    public static File getCacheDir() {
        Validate.sdkInitialized();
        return cacheDir.getValue();
    }

    /**
     * Sets the cache directory to use for caching responses, etc.
     * @param cacheDir the cache directory
     */
    public static void setCacheDir(File cacheDir) {
        FacebookSdk.cacheDir = new LockOnGetVariable<File>(cacheDir);
    }

    /**
     * Getter for the callback request code offset. The request codes starting at this offset and
     * the next 100 values are used by the Facebook SDK.
     *
     * @return The callback request code offset.
     */
    public static int getCallbackRequestCodeOffset() {
        Validate.sdkInitialized();
        return callbackRequestCodeOffset;
    }

    /**
     * Returns true if the request code is within the range used by Facebook SDK requests. This does
     * not include request codes that you explicitly set on the dialogs, buttons or LoginManager.
     * The range of request codes that the SDK uses starts at the callbackRequestCodeOffset and
     * continues for the next 100 values.
     *
     * @param requestCode the request code to check.
     * @return true if the request code is within the range used by the Facebook SDK.
     */
    public static boolean isFacebookRequestCode(int requestCode) {
        return requestCode >= callbackRequestCodeOffset
                && requestCode < callbackRequestCodeOffset + MAX_REQUEST_CODE_RANGE;
    }

    /**
     * Callback passed to the sdkInitialize function.
     */
    public interface InitializeCallback {
        /**
         * Called when the sdk has been initialized.
         */
        void onInitialized();
    }
}
