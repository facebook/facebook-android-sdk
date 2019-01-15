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
import android.media.FaceDetector;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.facebook.FacebookSdk.ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY;
import static com.facebook.FacebookSdk.AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY;

final class UserSettingsManager {
    private static final String TAG = UserSettingsManager.class.getName();

    private static AtomicBoolean isInitialized = new AtomicBoolean(false);

    private static UserSetting autoLogAppEventsEnabled = new UserSetting(
            true,
            AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY,
            AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY);
    private static UserSetting advertiserIDCollectionEnabled = new UserSetting(
            true,
            ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY,
            ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY);

    // Cache
    private static final String USER_SETTINGS = "com.facebook.sdk.USER_SETTINGS";
    private static SharedPreferences userSettingPref;
    private static SharedPreferences.Editor userSettingPrefEditor;

    // Parameter names of settings in cache
    private static final String LAST_TIMESTAMP = "last_timestamp";
    private static final String VALUE = "value";

    private static void initializeIfNotInitialized() {
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
    }

    private static void initializeUserSetting(UserSetting userSetting) {
        if (userSetting.value == null) {
            loadSettingFromCache(userSetting);
            if (userSetting.value == null && userSetting.keyInManifest != null) {
                loadSettingFromManifest(userSetting);
            }
        } else {
            // if flag has been set before initialization, load setting to cache
            putSettingToCache(userSetting);
        }
    }

    private static void putSettingToCache(UserSetting userSetting) {
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

    private static void loadSettingFromCache(UserSetting userSetting) {
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

    /**
     * Sanity check that if UserSettingsManager initialized successfully
     * */
    private static void validateInitialized() {
        if (!isInitialized.get()) {
            throw new FacebookSdkNotInitializedException(
                    "The UserSettingManager has not been initialized successfully");
        }
    }

    public static void setAutoLogAppEventsEnabled(boolean flag) {
        autoLogAppEventsEnabled.value = flag;
        autoLogAppEventsEnabled.lastTS = System.currentTimeMillis();
        if (isInitialized.get()) {
            putSettingToCache(autoLogAppEventsEnabled);
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
            putSettingToCache(advertiserIDCollectionEnabled);
        } else {
            initializeIfNotInitialized();
        }
    }

    public static boolean getAdvertiserIDCollectionEnabled() {
        initializeIfNotInitialized();
        return advertiserIDCollectionEnabled.getValue();
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
