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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.internal.AutomaticAnalyticsLogger;
import com.facebook.appevents.internal.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public final class FetchedAppSettingsManager {
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
    private static final int AUTOMATIC_LOGGING_ENABLED_BITMASK_FIELD = 1 << 3;
    private static final String APP_SETTING_SMART_LOGIN_OPTIONS =
            "seamless_login";
    private static final String SMART_LOGIN_BOOKMARK_ICON_URL = "smart_login_bookmark_icon_url";
    private static final String SMART_LOGIN_MENU_ICON_URL = "smart_login_menu_icon_url";

    private static final String[] APP_SETTING_FIELDS = new String[]{
            APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING,
            APP_SETTING_NUX_CONTENT,
            APP_SETTING_NUX_ENABLED,
            APP_SETTING_CUSTOM_TABS_ENABLED,
            APP_SETTING_DIALOG_CONFIGS,
            APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES,
            APP_SETTING_APP_EVENTS_SESSION_TIMEOUT,
            APP_SETTING_APP_EVENTS_FEATURE_BITMASK,
            APP_SETTING_SMART_LOGIN_OPTIONS,
            SMART_LOGIN_BOOKMARK_ICON_URL,
            SMART_LOGIN_MENU_ICON_URL,
    };
    private static final String APPLICATION_FIELDS = "fields";

    private static Map<String, FetchedAppSettings> fetchedAppSettings =
            new ConcurrentHashMap<String, FetchedAppSettings>();
    private static AtomicBoolean loadingSettings = new AtomicBoolean(false);

    public static void loadAppSettingsAsync() {
        final Context context = FacebookSdk.getApplicationContext();
        final String applicationId = FacebookSdk.getApplicationId();
        boolean canStartLoading = loadingSettings.compareAndSet(false, true);
        if (Utility.isNullOrEmpty(applicationId) ||
                fetchedAppSettings.containsKey(applicationId) ||
                !canStartLoading) {
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
                if (!Utility.isNullOrEmpty(settingsJSONString)) {
                    JSONObject settingsJSON = null;
                    try {
                        settingsJSON = new JSONObject(settingsJSONString);
                    } catch (JSONException je) {
                        Utility.logd(Utility.LOG_TAG, je);
                    }
                    if (settingsJSON != null) {
                        parseAppSettingsFromJSON(applicationId, settingsJSON);
                    }
                }

                JSONObject resultJSON = getAppSettingsQueryResponse(applicationId);
                if (resultJSON != null) {
                    parseAppSettingsFromJSON(applicationId, resultJSON);

                    sharedPrefs.edit()
                            .putString(settingsKey, resultJSON.toString())
                            .apply();
                }

                // Start log activate & deactivate app events, in case autoLogAppEvents flag is set
                AutomaticAnalyticsLogger.logActivateAppEvent();

                loadingSettings.set(false);
            }
        });
    }

    // This call only gets the app settings if they're already fetched
    public static FetchedAppSettings getAppSettingsWithoutQuery(final String applicationId) {
        return applicationId != null ? fetchedAppSettings.get(applicationId) : null;
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread.
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

        return parseAppSettingsFromJSON(applicationId, response);
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
                settingsJSON.optString(SMART_LOGIN_MENU_ICON_URL)
        );

        fetchedAppSettings.put(applicationId, result);

        return result;
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread.
    private static JSONObject getAppSettingsQueryResponse(String applicationId) {
        Bundle appSettingsParams = new Bundle();
        appSettingsParams.putString(APPLICATION_FIELDS, TextUtils.join(",", APP_SETTING_FIELDS));

        GraphRequest request = GraphRequest.newGraphPathRequest(null, applicationId, null);
        request.setSkipClientToken(true);
        request.setParameters(appSettingsParams);

        return request.executeAndWait().getJSONObject();
    }

    private static Map<String, Map<String, FetchedAppSettings.DialogFeatureConfig>> parseDialogConfigurations(
            JSONObject dialogConfigResponse) {
        HashMap<String, Map<String, FetchedAppSettings.DialogFeatureConfig>> dialogConfigMap
                = new HashMap<String, Map<String, FetchedAppSettings.DialogFeatureConfig>>();

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
                        featureMap = new HashMap<String, FetchedAppSettings.DialogFeatureConfig>();
                        dialogConfigMap.put(dialogName, featureMap);
                    }
                    featureMap.put(dialogConfig.getFeatureName(), dialogConfig);
                }
            }
        }

        return dialogConfigMap;
    }
}
