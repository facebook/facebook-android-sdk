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

package com.facebook;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.BoltsMeasurementEventListener;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class allows some customization of Facebook SDK behavior.
 */
public final class FacebookSdk {
    private static final String TAG = FacebookSdk.class.getCanonicalName();
    private static final HashSet<LoggingBehavior> loggingBehaviors =
            new HashSet<LoggingBehavior>(Arrays.asList(LoggingBehavior.DEVELOPER_ERRORS));
    private static volatile Executor executor;
    private static volatile String applicationId;
    private static volatile String applicationName;
    private static volatile String appClientToken;
    private static final String FACEBOOK_COM = "facebook.com";
    private static volatile String facebookDomain = FACEBOOK_COM;
    private static AtomicLong onProgressThreshold = new AtomicLong(65536);
    private static volatile boolean isDebugEnabled = BuildConfig.DEBUG;
    private static boolean isLegacyTokenUpgradeSupported = false;
    private static File cacheDir;
    private static Context applicationContext;
    private static final int DEFAULT_CORE_POOL_SIZE = 5;
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 128;
    private static final int DEFAULT_KEEP_ALIVE = 1;
    private static int callbackRequestCodeOffset = 0xface;
    private static final Object LOCK = new Object();

    private static final int MAX_REQUEST_CODE_RANGE = 100;

    private static final Uri ATTRIBUTION_ID_CONTENT_URI =
            Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
    private static final String ATTRIBUTION_ID_COLUMN_NAME = "aid";

    private static final String ATTRIBUTION_PREFERENCES = "com.facebook.sdk.attributionTracking";
    private static final String PUBLISH_ACTIVITY_PATH = "%s/activities";
    private static final String MOBILE_INSTALL_EVENT = "MOBILE_APP_INSTALL";
    private static final String ANALYTICS_EVENT = "event";

    private static final BlockingQueue<Runnable> DEFAULT_WORK_QUEUE =
            new LinkedBlockingQueue<Runnable>(10);

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "FacebookSdk #" + counter.incrementAndGet());
        }
    };

    static final String CALLBACK_OFFSET_CHANGED_AFTER_INIT =
            "The callback request code offset can't be updated once the SDK is initialized.";

    static final String CALLBACK_OFFSET_NEGATIVE =
            "The callback request code offset can't be negative.";


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

    private static Boolean sdkInitialized = false;

    /**
     * This function initializes the Facebook SDK, the behavior of Facebook SDK functions are
     * undetermined if this function is not called. It should be called as early as possible.
     * @param applicationContext The application context
     * @param callbackRequestCodeOffset The request code offset that Facebook activities will be
     *                                  called with. Please do not use the range between the
     *                                  value you set and another 100 entries after it in your
     *                                  other requests.
     */
    public static synchronized void sdkInitialize(
            Context applicationContext,
            int callbackRequestCodeOffset) {
        if (sdkInitialized && callbackRequestCodeOffset != FacebookSdk.callbackRequestCodeOffset) {
            throw new FacebookException(CALLBACK_OFFSET_CHANGED_AFTER_INIT);
        }
        if (callbackRequestCodeOffset < 0) {
            throw new FacebookException(CALLBACK_OFFSET_NEGATIVE);
        }
        FacebookSdk.callbackRequestCodeOffset = callbackRequestCodeOffset;
        sdkInitialize(applicationContext);
    }


    /**
     * This function initializes the Facebook SDK, the behavior of Facebook SDK functions are
     * undetermined if this function is not called. It should be called as early as possible.
     * @param applicationContext The application context
     */
    public static synchronized void sdkInitialize(Context applicationContext) {
        if (sdkInitialized == true) {
          return;
        }

        Validate.notNull(applicationContext, "applicationContext");

        FacebookSdk.applicationContext = applicationContext.getApplicationContext();

        // Make sure we've loaded default settings if we haven't already.
        FacebookSdk.loadDefaultsFromMetadata(FacebookSdk.applicationContext);
        // Load app settings from network so that dialog configs are available
        Utility.loadAppSettingsAsync(FacebookSdk.applicationContext, applicationId);

        BoltsMeasurementEventListener.getInstance(FacebookSdk.applicationContext);

        cacheDir = FacebookSdk.applicationContext.getCacheDir();

        FutureTask<Void> accessTokenLoadFutureTask =
                new FutureTask<Void>(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        AccessTokenManager.getInstance().loadCurrentAccessToken();
                        ProfileManager.getInstance().loadCurrentProfile();
                        if (AccessToken.getCurrentAccessToken() != null &&
                                Profile.getCurrentProfile() == null) {
                            // Access token and profile went out of sync due to a network or caching
                            // issue, retry
                            Profile.fetchProfileForCurrentAccessToken();
                        }
                        return null;
                    }
                });
        getExecutor().execute(accessTokenLoadFutureTask);

        sdkInitialized = true;
    }

    /**
     * Indicates whether the Facebook SDK has been initialized.
     * @return true if initialized, false if not
     */
    public static synchronized boolean isInitialized() {
        return sdkInitialized;
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
                Executor executor = getAsyncTaskExecutor();
                if (executor == null) {
                    executor = new ThreadPoolExecutor(
                            DEFAULT_CORE_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE, DEFAULT_KEEP_ALIVE,
                            TimeUnit.SECONDS, DEFAULT_WORK_QUEUE, DEFAULT_THREAD_FACTORY);
                }
                FacebookSdk.executor = executor;
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
     * always be "facebook.com".
     *
     * @return the Facebook domain
     */
    public static String getFacebookDomain() {
        return facebookDomain;
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

    private static Executor getAsyncTaskExecutor() {
        Field executorField = null;
        try {
            executorField = AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
        } catch (NoSuchFieldException e) {
            return null;
        }

        Object executorObject = null;
        try {
            executorObject = executorField.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }

        if (executorObject == null) {
            return null;
        }

        if (!(executorObject instanceof Executor)) {
            return null;
        }

        return (Executor) executorObject;
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

    static GraphResponse publishInstallAndWaitForResponse(
            final Context context,
            final String applicationId) {
        try {
            if (context == null || applicationId == null) {
                throw new IllegalArgumentException("Both context and applicationId must be non-null");
            }
            AttributionIdentifiers identifiers = AttributionIdentifiers.getAttributionIdentifiers(context);
            SharedPreferences preferences = context.getSharedPreferences(ATTRIBUTION_PREFERENCES, Context.MODE_PRIVATE);
            String pingKey = applicationId+"ping";
            String jsonKey = applicationId+"json";
            long lastPing = preferences.getLong(pingKey, 0);
            String lastResponseJSON = preferences.getString(jsonKey, null);

            JSONObject publishParams = new JSONObject();
            try {
                publishParams.put(ANALYTICS_EVENT, MOBILE_INSTALL_EVENT);

                Utility.setAppEventAttributionParameters(publishParams,
                        identifiers,
                        AppEventsLogger.getAnonymousAppDeviceGUID(context),
                        getLimitEventAndDataUsage(context));
                publishParams.put("application_package_name", context.getPackageName());
            } catch (JSONException e) {
                throw new FacebookException("An error occurred while publishing install.", e);
            }

            String publishUrl = String.format(PUBLISH_ACTIVITY_PATH, applicationId);
            GraphRequest publishRequest = GraphRequest.newPostRequest(null, publishUrl, publishParams, null);

            if (lastPing != 0) {
                JSONObject graphObject = null;
                try {
                    if (lastResponseJSON != null) {
                        graphObject = new JSONObject(lastResponseJSON);
                    }
                }
                catch (JSONException je) {
                    // return the default graph object if there is any problem reading the data.
                }
                if (graphObject == null) {
                    return GraphResponse.createResponsesFromString(
                            "true",
                            null,
                            new GraphRequestBatch(publishRequest)
                    ).get(0);
                } else {
                    return new GraphResponse(null, null, null, graphObject);
                }

            } else {

                GraphResponse publishResponse = publishRequest.executeAndWait();

                // denote success since no error threw from the post.
                SharedPreferences.Editor editor = preferences.edit();
                lastPing = System.currentTimeMillis();
                editor.putLong(pingKey, lastPing);

                // if we got an object response back, cache the string of the JSON.
                if (publishResponse.getJSONObject() != null) {
                    editor.putString(jsonKey, publishResponse.getJSONObject().toString());
                }
                editor.apply();

                return publishResponse;
            }
        } catch (Exception e) {
            // if there was an error, fall through to the failure case.
            Utility.logd("Facebook-publish", e);
            return new GraphResponse(null, null, new FacebookRequestError(null, e));
        }
    }

    /**
     * Returns the current attribution id from the facebook app.
     *
     * @return null if the facebook app is not present on the phone.
     */
    public static String getAttributionId(ContentResolver contentResolver) {
        Validate.sdkInitialized();
        Cursor c = null;
        try {
            String [] projection = {ATTRIBUTION_ID_COLUMN_NAME};
            c = contentResolver.query(ATTRIBUTION_ID_CONTENT_URI, projection, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            String attributionId = c.getString(c.getColumnIndex(ATTRIBUTION_ID_COLUMN_NAME));
            return attributionId;
        } catch (Exception e) {
            Log.d(TAG, "Caught unexpected exception in getAttributionId(): " + e.toString());
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Returns the current version of the Facebook SDK for Android as a string.
     *
     * @return the current version of the SDK
     */
    public static String getSdkVersion() {
        Validate.sdkInitialized();
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
                AppEventsLogger.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE);
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
        context.getSharedPreferences(AppEventsLogger.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
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
            applicationId = ai.metaData.getString(APPLICATION_ID_PROPERTY);
        }
        if (applicationName == null) {
            applicationName = ai.metaData.getString(APPLICATION_NAME_PROPERTY);
        }
        if (appClientToken == null) {
            appClientToken = ai.metaData.getString(CLIENT_TOKEN_PROPERTY);
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
     * Gets the cache directory to use for caching responses, etc. The default will be the value
     * returned by Context.getCacheDir() when the SDK was initialized, but it can be overridden.
     *
     * @return the cache directory
     */
    public static File getCacheDir() {
        Validate.sdkInitialized();
        return cacheDir;
    }

    /**
     * Sets the cache directory to use for caching responses, etc.
     * @param cacheDir the cache directory
     */
    public static void setCacheDir(File cacheDir) {
        FacebookSdk.cacheDir = cacheDir;
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
}
