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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.facebook.FacebookSdk.ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY;
import static com.facebook.FacebookSdk.AUTO_INIT_ENABLED_PROPERTY;
import static com.facebook.FacebookSdk.AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY;

final class UserSettingsManager {
    private static final String TAG = UserSettingsManager.class.getName();

    private static AtomicBoolean isInitialized = new AtomicBoolean(false);

    private static final String EVENTS_CODELESS_SETUP_ENABLED =
            "auto_event_setup_enabled";
    private static final long TIMEOUT_7D = 7 * 24 * 60 * 60 * 1000; // Millisecond
    private static final String ADVERTISER_ID_KEY = "advertiser_id";
    private static final String APPLICATION_FIELDS = "fields";

    private static UserSetting autoInitEnabled = new UserSetting(
            true,
            AUTO_INIT_ENABLED_PROPERTY,
            AUTO_INIT_ENABLED_PROPERTY);
    private static UserSetting autoLogAppEventsEnabled = new UserSetting(
            true,
            AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY,
            AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY);
    private static UserSetting advertiserIDCollectionEnabled = new UserSetting(
            true,
            ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY,
            ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY);
    private static UserSetting codelessSetupEnabled = new UserSetting(
            false,
            EVENTS_CODELESS_SETUP_ENABLED,
            null);

    // Cache
    private static final String USER_SETTINGS = "com.facebook.sdk.USER_SETTINGS";
    private static SharedPreferences userSettingPref;
    private static SharedPreferences.Editor userSettingPrefEditor;

    // Parameter names of settings in cache
    private static final String LAST_TIMESTAMP = "last_timestamp";
    private static final String VALUE = "value";

    // Warning message for App Event Flags
    private static final String AUTOLOG_APPEVENT_NOT_SET_WARNING =
            "Please set a value for AutoLogAppEventsEnabled. Set the flag to TRUE if you want " +
            "to collect app install, app launch and in-app purchase events automatically. To " +
            "request user consent before collecting data, set the flag value to FALSE, then " +
            "change to TRUE once user consent is received. " +
            "Learn more: https://developers.facebook.com/docs/app-events/getting-started-app-events-android#disable-auto-events.";
    private static final String ADVERTISERID_COLLECTION_NOT_SET_WARNING =
            "You haven't set a value for AdvertiserIDCollectionEnabled. Set the flag to TRUE " +
            "if you want to collect Advertiser ID for better advertising and analytics " +
            "results. To request user consent before collecting data, set the flag value to " +
            "FALSE, then change to TRUE once user consent is received. " +
            "Learn more: https://developers.facebook.com/docs/app-events/getting-started-app-events-android#disable-auto-events.";
    private static final String ADVERTISERID_COLLECTION_FALSE_WARNING =
            "The value for AdvertiserIDCollectionEnabled is currently set to FALSE so you're " +
            "sending app events without collecting Advertiser ID. This can affect the quality " +
            "of your advertising and analytics results.";

    public static void initializeIfNotInitialized() {
        if (!FacebookSdk.isInitialized()) {
            return;
        }

        if (!isInitialized.compareAndSet(false, true)) {
            return;
        }

        userSettingPref = FacebookSdk.getApplicationContext()
                .getSharedPreferences(USER_SETTINGS, Context.MODE_PRIVATE);
        userSettingPrefEditor = userSettingPref.edit();

        initializeUserSetting(autoLogAppEventsEnabled);
        initializeUserSetting(advertiserIDCollectionEnabled);
        initializeUserSetting(autoInitEnabled);
        initializeCodelessSepupEnabledAsync();
        logWarnings();
    }

    private static void initializeUserSetting(UserSetting userSetting) {
        if (userSetting == codelessSetupEnabled) {
            initializeCodelessSepupEnabledAsync();
        } else {
            if (userSetting.value == null) {
                readSettingFromCache(userSetting);
                if (userSetting.value == null && userSetting.keyInManifest != null) {
                    loadSettingFromManifest(userSetting);
                }
            } else {
                // if flag has been set before initialization, load setting to cache
                writeSettingToCache(userSetting);
            }
        }
    }

    private static void initializeCodelessSepupEnabledAsync() {
        readSettingFromCache(codelessSetupEnabled);
        final long currTime = System.currentTimeMillis();
        if (codelessSetupEnabled.value != null && currTime - codelessSetupEnabled.lastTS < TIMEOUT_7D) {
            return;
        } else {
            codelessSetupEnabled.value = null;
            codelessSetupEnabled.lastTS = 0;
        }

        // fetch data through Graph request if cache is unavailable
        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (advertiserIDCollectionEnabled.getValue()) {
                    FetchedAppSettings appSettings = FetchedAppSettingsManager
                            .queryAppSettings(FacebookSdk.getApplicationId(), false);
                    if (appSettings != null && appSettings.getCodelessEventsEnabled()) {
                        String advertiser_id = null;
                        final Context context = FacebookSdk.getApplicationContext();
                        AttributionIdentifiers identifiers =
                                AttributionIdentifiers.getAttributionIdentifiers(context);
                        if (identifiers != null
                                && identifiers.getAndroidAdvertiserId() != null) {
                            advertiser_id = identifiers.getAndroidAdvertiserId();
                        }
                        if (advertiser_id != null) {
                            Bundle codelessSettingsParams = new Bundle();
                            codelessSettingsParams.putString(
                                    ADVERTISER_ID_KEY, identifiers.getAndroidAdvertiserId());
                            codelessSettingsParams.putString(
                                    APPLICATION_FIELDS, EVENTS_CODELESS_SETUP_ENABLED);
                            GraphRequest codelessRequest = GraphRequest.newGraphPathRequest(
                                    null, FacebookSdk.getApplicationId(), null);
                            codelessRequest.setSkipClientToken(true);
                            codelessRequest.setParameters(codelessSettingsParams);
                            JSONObject response = codelessRequest.executeAndWait().getJSONObject();
                            if (response != null) {
                                codelessSetupEnabled.value =
                                        response.optBoolean(EVENTS_CODELESS_SETUP_ENABLED, false);
                                codelessSetupEnabled.lastTS = currTime;
                                writeSettingToCache(codelessSetupEnabled);
                            }
                        }
                    }
                }
            }
        });
    }

    private static void writeSettingToCache(UserSetting userSetting) {
        validateInitialized();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(VALUE, userSetting.value);
            jsonObject.put(LAST_TIMESTAMP, userSetting.lastTS);
            userSettingPrefEditor
                    .putString(userSetting.keyInCache, jsonObject.toString())
                    .commit();
        } catch (JSONException je) {
            Utility.logd(TAG, je);
        }
    }

    private static void readSettingFromCache(UserSetting userSetting) {
        validateInitialized();
        try {
            String settingStr = userSettingPref.getString(userSetting.keyInCache, "");
            if (!settingStr.isEmpty()) {
                JSONObject setting = new JSONObject(settingStr);
                userSetting.value = setting.getBoolean(VALUE);
                userSetting.lastTS = setting.getLong(LAST_TIMESTAMP);
            }
        } catch (JSONException je) {
            Utility.logd(TAG, je);
        }
    }

    private static void loadSettingFromManifest(UserSetting userSetting) {
        validateInitialized();
        try {
            ApplicationInfo ai = FacebookSdk.getApplicationContext()
                    .getPackageManager()
                    .getApplicationInfo(
                            FacebookSdk.getApplicationContext().getPackageName(),
                            PackageManager.GET_META_DATA);
            if (ai != null && ai.metaData != null && ai.metaData.containsKey(userSetting.keyInManifest)) {
                // default value should not be used
                userSetting.value = ai.metaData.getBoolean(userSetting.keyInManifest, userSetting.defaultVal);
            }

        } catch (PackageManager.NameNotFoundException e) {
            Utility.logd(TAG, e);
        }
    }

    private static void logWarnings() {
        try {
            ApplicationInfo ai = FacebookSdk.getApplicationContext()
                    .getPackageManager()
                    .getApplicationInfo(
                            FacebookSdk.getApplicationContext().getPackageName(),
                            PackageManager.GET_META_DATA);
            if (ai != null && ai.metaData != null) {
                // Log warnings for App Event Flags
                if (!ai.metaData.containsKey(AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY)) {
                    Log.w(TAG, AUTOLOG_APPEVENT_NOT_SET_WARNING);
                }
                if (!ai.metaData.containsKey(ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY)) {
                    Log.w(TAG, ADVERTISERID_COLLECTION_NOT_SET_WARNING);
                }
                if (!UserSettingsManager.getAdvertiserIDCollectionEnabled()) {
                    Log.w(TAG, ADVERTISERID_COLLECTION_FALSE_WARNING);
                }
            }
        } catch (PackageManager.NameNotFoundException e) { /* no op */}
    }

    /**
     * Sanity check that if UserSettingsManager initialized successfully
     */
    private static void validateInitialized() {
        if (!isInitialized.get()) {
            throw new FacebookSdkNotInitializedException(
                    "The UserSettingManager has not been initialized successfully");
        }
    }

    public static void setAutoInitEnabled(boolean flag) {
        autoInitEnabled.value = flag;
        autoInitEnabled.lastTS = System.currentTimeMillis();
        if (isInitialized.get()) {
            writeSettingToCache(autoInitEnabled);
        } else {
            initializeIfNotInitialized();
        }
    }

    public static boolean getAutoInitEnabled() {
        initializeIfNotInitialized();
        return autoInitEnabled.getValue();
    }

    public static void setAutoLogAppEventsEnabled(boolean flag) {
        autoLogAppEventsEnabled.value = flag;
        autoLogAppEventsEnabled.lastTS = System.currentTimeMillis();
        if (isInitialized.get()) {
            writeSettingToCache(autoLogAppEventsEnabled);
        } else {
            initializeIfNotInitialized();
        }
    }

    public static boolean getAutoLogAppEventsEnabled() {
        initializeIfNotInitialized();
        return autoLogAppEventsEnabled.getValue();
    }

    public static void setAdvertiserIDCollectionEnabled(boolean flag) {
        advertiserIDCollectionEnabled.value = flag;
        advertiserIDCollectionEnabled.lastTS = System.currentTimeMillis();
        if (isInitialized.get()) {
            writeSettingToCache(advertiserIDCollectionEnabled);
        } else {
            initializeIfNotInitialized();
        }
    }

    public static boolean getAdvertiserIDCollectionEnabled() {
        initializeIfNotInitialized();
        return advertiserIDCollectionEnabled.getValue();
    }

    public static boolean getCodelessSetupEnabled() {
        initializeIfNotInitialized();
        return codelessSetupEnabled.getValue();
    }

    private static class UserSetting {
        String keyInCache;
        String keyInManifest;
        Boolean value;
        boolean defaultVal;
        long lastTS;

        UserSetting(boolean defaultVal, String keyInCache, String keyInManifest) {
            this.defaultVal = defaultVal;
            this.keyInCache = keyInCache;
            this.keyInManifest = keyInManifest;
        }

        boolean getValue() {
            return value == null ? defaultVal : value;
        }
    }
}
