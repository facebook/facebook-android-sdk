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

package com.facebook.internal;

import static com.facebook.internal.FetchedAppSettingsManager.FetchAppSettingState.ERROR;
import static com.facebook.internal.FetchedAppSettingsManager.FetchAppSettingState.LOADING;
import static com.facebook.internal.FetchedAppSettingsManager.FetchAppSettingState.NOT_LOADED;
import static com.facebook.internal.FetchedAppSettingsManager.FetchAppSettingState.SUCCESS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.codeless.internal.UnityReflection;
import com.facebook.appevents.internal.AutomaticAnalyticsLogger;
import com.facebook.appevents.internal.Constants;
import com.facebook.appevents.internal.InAppPurchaseActivityLifecycleTracker;
import com.facebook.core.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public final class FetchedAppSettingsManager {

    enum FetchAppSettingState {
        NOT_LOADED,
        LOADING,
        SUCCESS,
        ERROR,
    }
    private static final String TAG = FetchedAppSettingsManager.class.getSimpleName();
    private static final String APP_SETTINGS_PREFS_STORE =
            "com.facebook.internal.preferences.APP_SETTINGS";
    private static final String APP_SETTINGS_PREFS_KEY_FORMAT =
            "com.facebook.internal.APP_SETTINGS.%s";
    private static final String APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING =
            "supports_implicit_sdk_logging";
    private static final String APP_SETTING_NUX_CONTENT = "gdpv4_nux_content";
    private static final String APP_SETTING_NUX_ENABLED = "gdpv4_nux_enabled";
    private static final String APP_SETTING_CUSTOM_TABS_ENABLED =
            "gdpv4_chrome_custom_tabs_enabled";
    private static final String APP_SETTING_DIALOG_CONFIGS = "android_dialog_configs";
    private static final String APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES =
            "android_sdk_error_categories";
    private static final String APP_SETTING_APP_EVENTS_SESSION_TIMEOUT =
            "app_events_session_timeout";
    private static final String APP_SETTING_APP_EVENTS_FEATURE_BITMASK =
            "app_events_feature_bitmask";
    private static final String APP_SETTING_APP_EVENTS_EVENT_BINDINGS =
            "auto_event_mapping_android";
    private static final int AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD = 1 << 3;
    // The second bit of app_events_feature_bitmask is used for iOS in-app purchase automatic
    // logging, while the fourth bit is used for Android in-app purchase automatic logging.
    private static final int IAP_AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD = 1 << 4;
    private static final int CODELESS_EVENTS_ENABLED_BITMASK_FIELD = 1 << 5;
    private static final int TRACK_UNINSTALL_ENABLED_BITMASK_FIELD = 1 << 8;
    private static final String APP_SETTING_SMART_LOGIN_OPTIONS =
            "seamless_login";
    private static final String SMART_LOGIN_BOOKMARK_ICON_URL = "smart_login_bookmark_icon_url";
    private static final String SMART_LOGIN_MENU_ICON_URL = "smart_login_menu_icon_url";
    private static final String SDK_UPDATE_MESSAGE = "sdk_update_message";

    private static final String[] APP_SETTING_FIELDS = new String[]{
            APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING,
            APP_SETTING_NUX_CONTENT,
            APP_SETTING_NUX_ENABLED,
            APP_SETTING_CUSTOM_TABS_ENABLED,
            APP_SETTING_DIALOG_CONFIGS,
            APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES,
            APP_SETTING_APP_EVENTS_SESSION_TIMEOUT,
            APP_SETTING_APP_EVENTS_FEATURE_BITMASK,
            APP_SETTING_APP_EVENTS_EVENT_BINDINGS,
            APP_SETTING_SMART_LOGIN_OPTIONS,
            SMART_LOGIN_BOOKMARK_ICON_URL,
            SMART_LOGIN_MENU_ICON_URL
    };
    private static final String APPLICATION_FIELDS = "fields";

    private static final Map<String, FetchedAppSettings> fetchedAppSettings =
            new ConcurrentHashMap<>();
    private static final AtomicReference<FetchAppSettingState> loadingState =
            new AtomicReference<>(NOT_LOADED);
    private static final ConcurrentLinkedQueue<FetchedAppSettingsCallback>
            fetchedAppSettingsCallbacks = new ConcurrentLinkedQueue<>();

    private static boolean printedSDKUpdatedMessage = false;

    private static boolean isUnityInit = false;
    @Nullable private static JSONArray unityEventBindings = null;

    public static void loadAppSettingsAsync() {
        final Context context = FacebookSdk.getApplicationContext();
        final String applicationId = FacebookSdk.getApplicationId();

        if (Utility.isNullOrEmpty(applicationId)) {
            loadingState.set(ERROR);
            pollCallbacks();
            return;
        } else if (fetchedAppSettings.containsKey(applicationId)) {
            loadingState.set(SUCCESS);
            pollCallbacks();
            return;
        }

        boolean canStartLoading = loadingState.compareAndSet(NOT_LOADED, LOADING)
                || loadingState.compareAndSet(ERROR, LOADING);

        if (!canStartLoading) {
            pollCallbacks();
            return;
        }

        final String settingsKey = String.format(APP_SETTINGS_PREFS_KEY_FORMAT, applicationId);

        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // See if we had a cached copy and use that immediately.
                SharedPreferences sharedPrefs = context.getSharedPreferences(
                        APP_SETTINGS_PREFS_STORE,
                        Context.MODE_PRIVATE);
                String settingsJSONString = sharedPrefs.getString(settingsKey, null);
                FetchedAppSettings appSettings = null;
                if (!Utility.isNullOrEmpty(settingsJSONString)) {
                    JSONObject settingsJSON = null;
                    try {
                        settingsJSON = new JSONObject(settingsJSONString);
                    } catch (JSONException je) {
                        Utility.logd(Utility.LOG_TAG, je);
                    }
                    if (settingsJSON != null) {
                        appSettings = parseAppSettingsFromJSON(applicationId, settingsJSON);
                    }
                }

                JSONObject resultJSON = getAppSettingsQueryResponse(applicationId);
                if (resultJSON != null) {
                    parseAppSettingsFromJSON(applicationId, resultJSON);

                    sharedPrefs.edit()
                            .putString(settingsKey, resultJSON.toString())
                            .apply();
                }

                // Print log to notify developers to upgrade SDK when version is too old
                if (appSettings != null) {
                    String updateMessage = appSettings.getSdkUpdateMessage();
                    if (!printedSDKUpdatedMessage
                            && updateMessage != null
                            && updateMessage.length() > 0) {
                        printedSDKUpdatedMessage = true;
                        Log.w(TAG, updateMessage);
                    }
                }

                // Fetch GateKeepers
                FetchedAppGateKeepersManager.queryAppGateKeepers(applicationId, true);

                // Start log activate & deactivate app events, in case autoLogAppEvents flag is set
                AutomaticAnalyticsLogger.logActivateAppEvent();

                // Automatically log In App Purchase events
                InAppPurchaseActivityLifecycleTracker.update();

                loadingState.set(fetchedAppSettings.containsKey(applicationId) ? SUCCESS : ERROR);
                pollCallbacks();
            }
        });
    }

    // This call only gets the app settings if they're already fetched
    @Nullable
    public static FetchedAppSettings getAppSettingsWithoutQuery(final String applicationId) {
        return applicationId != null ? fetchedAppSettings.get(applicationId) : null;
    }

    /**
     * Run callback with app settings if available. It is possible that app settings take a while
     * to load due to latency or it is requested too early in the application lifecycle.
     *
     * @param callback Callback to be run after app settings are available
     */
    public static void getAppSettingsAsync(final FetchedAppSettingsCallback callback) {
        fetchedAppSettingsCallbacks.add(callback);
        loadAppSettingsAsync();
    }

    /**
     * Run all available callbacks and remove them. If app settings are available, run the success
     * callback, error otherwise.
     */
    private synchronized static void pollCallbacks() {
        FetchAppSettingState currentState = loadingState.get();
        if (NOT_LOADED.equals(currentState) || LOADING.equals(currentState)) {
            return;
        }

        final String applicationId = FacebookSdk.getApplicationId();
        final FetchedAppSettings appSettings = fetchedAppSettings.get(applicationId);
        final Handler handler = new Handler(Looper.getMainLooper());

        if (ERROR.equals(currentState)) {
            while (!fetchedAppSettingsCallbacks.isEmpty()) {
                final FetchedAppSettingsCallback callback = fetchedAppSettingsCallbacks.poll();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError();
                    }
                });
            }
            return;
        }

        while (!fetchedAppSettingsCallbacks.isEmpty()) {
            final FetchedAppSettingsCallback callback = fetchedAppSettingsCallbacks.poll();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(appSettings);
                }
            });
        }
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread. This call can block for long time if network is not available and network
    // timeout is long.
    @Nullable
    public static FetchedAppSettings queryAppSettings(
            final String applicationId,
            final boolean forceRequery) {
        // Cache the last app checked results.
        if (!forceRequery && fetchedAppSettings.containsKey(applicationId)) {
            return fetchedAppSettings.get(applicationId);
        }

        JSONObject response = getAppSettingsQueryResponse(applicationId);
        if (response == null) {
            return null;
        }

        FetchedAppSettings fetchedAppSettings = parseAppSettingsFromJSON(applicationId, response);

        if (applicationId.equals(FacebookSdk.getApplicationId())) {
            loadingState.set(SUCCESS);
            pollCallbacks();
        }

        return fetchedAppSettings;
    }

    private static FetchedAppSettings parseAppSettingsFromJSON(
            String applicationId,
            JSONObject settingsJSON) {
        JSONArray errorClassificationJSON =
                settingsJSON.optJSONArray(APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES);
        FacebookRequestErrorClassification errorClassification =
                errorClassificationJSON == null
                        ? FacebookRequestErrorClassification.getDefaultErrorClassification()
                        : FacebookRequestErrorClassification.createFromJSON(
                        errorClassificationJSON
                );
        int featureBitmask = settingsJSON.optInt(APP_SETTING_APP_EVENTS_FEATURE_BITMASK,0);
        boolean automaticLoggingEnabled =
                (featureBitmask & AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD) != 0;
        boolean inAppPurchaseAutomaticLoggingEnabled =
                (featureBitmask & IAP_AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD) != 0;
        boolean codelessEventsEnabled =
                (featureBitmask & CODELESS_EVENTS_ENABLED_BITMASK_FIELD) != 0;
        boolean trackUninstallEnabled =
                (featureBitmask & TRACK_UNINSTALL_ENABLED_BITMASK_FIELD) != 0;
        JSONArray eventBindings = settingsJSON.optJSONArray(APP_SETTING_APP_EVENTS_EVENT_BINDINGS);

        unityEventBindings = eventBindings;
        if (unityEventBindings != null && InternalSettings.isUnityApp()) {
            UnityReflection.sendEventMapping(eventBindings.toString());
        }

        FetchedAppSettings result = new FetchedAppSettings(
                settingsJSON.optBoolean(APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING, false),
                settingsJSON.optString(APP_SETTING_NUX_CONTENT, ""),
                settingsJSON.optBoolean(APP_SETTING_NUX_ENABLED, false),
                settingsJSON.optBoolean(APP_SETTING_CUSTOM_TABS_ENABLED, false),
                settingsJSON.optInt(
                        APP_SETTING_APP_EVENTS_SESSION_TIMEOUT,
                        Constants.getDefaultAppEventsSessionTimeoutInSeconds()),
                SmartLoginOption.parseOptions(settingsJSON.optLong(APP_SETTING_SMART_LOGIN_OPTIONS)),
                parseDialogConfigurations(settingsJSON.optJSONObject(APP_SETTING_DIALOG_CONFIGS)),
                automaticLoggingEnabled,
                errorClassification,
                settingsJSON.optString(SMART_LOGIN_BOOKMARK_ICON_URL),
                settingsJSON.optString(SMART_LOGIN_MENU_ICON_URL),
                inAppPurchaseAutomaticLoggingEnabled,
                codelessEventsEnabled,
                eventBindings,
                settingsJSON.optString(SDK_UPDATE_MESSAGE),
                trackUninstallEnabled
        );

        fetchedAppSettings.put(applicationId, result);

        return result;
    }

    public static void setIsUnityInit(boolean flag) {
        isUnityInit = flag;
        if (unityEventBindings != null && isUnityInit) {
            UnityReflection.sendEventMapping(unityEventBindings.toString());
        }
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread. This call can block for long time if network is not available and network
    // timeout is long.
    private static JSONObject getAppSettingsQueryResponse(String applicationId) {
        Bundle appSettingsParams = new Bundle();
        ArrayList<String> appSettingFields = new ArrayList<>(Arrays.asList(APP_SETTING_FIELDS));

        if (BuildConfig.DEBUG) {
            appSettingFields.add(SDK_UPDATE_MESSAGE);
        }

        appSettingsParams.putString(APPLICATION_FIELDS, TextUtils.join(",", appSettingFields));

        GraphRequest request = GraphRequest.newGraphPathRequest(null, applicationId, null);
        request.setSkipClientToken(true);
        request.setParameters(appSettingsParams);

        return request.executeAndWait().getJSONObject();
    }

    private static Map<String, Map<String, FetchedAppSettings.DialogFeatureConfig>> parseDialogConfigurations(
            JSONObject dialogConfigResponse) {
        HashMap<String, Map<String, FetchedAppSettings.DialogFeatureConfig>> dialogConfigMap
                = new HashMap<>();

        if (dialogConfigResponse != null) {
            JSONArray dialogConfigData = dialogConfigResponse.optJSONArray("data");
            if (dialogConfigData != null) {
                for (int i = 0; i < dialogConfigData.length(); i++) {
                    FetchedAppSettings.DialogFeatureConfig dialogConfig =
                            FetchedAppSettings.DialogFeatureConfig.parseDialogConfig(
                                    dialogConfigData.optJSONObject(i));
                    if (dialogConfig == null) {
                        continue;
                    }

                    String dialogName = dialogConfig.getDialogName();
                    Map<String, FetchedAppSettings.DialogFeatureConfig> featureMap =
                            dialogConfigMap.get(dialogName);
                    if (featureMap == null) {
                        featureMap = new HashMap<>();
                        dialogConfigMap.put(dialogName, featureMap);
                    }
                    featureMap.put(dialogConfig.getFeatureName(), dialogConfig);
                }
            }
        }

        return dialogConfigMap;
    }

    public interface FetchedAppSettingsCallback {
        void onSuccess(FetchedAppSettings fetchedAppSettings);
        void onError();
    }
}