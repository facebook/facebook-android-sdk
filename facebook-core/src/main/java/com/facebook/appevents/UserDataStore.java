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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.internal.AppEventUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserDataStore {
    private static final String TAG = UserDataStore.class.getSimpleName();
    private static final String USER_DATA_KEY =
            "com.facebook.appevents.UserDataStore.userData";

    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static Map<String, String> hashedUserData;
    private static SharedPreferences sharedPreferences;
    private static volatile boolean initialized = false;

    /**
     * User data types
     */
    public static final String EMAIL = "em";
    public static final String FIRST_NAME = "fn";
    public static final String LAST_NAME = "ln";
    public static final String PHONE = "ph";
    public static final String DATE_OF_BIRTH = "db";
    public static final String GENDER = "ge";
    public static final String CITY = "ct";
    public static final String STATE = "st";
    public static final String ZIP = "zp";
    public static final String COUNTRY = "country";

    static void initStore() {
        if (initialized) {
            return;
        }

        AppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                initAndWait();
            }
        });
    }

    static void setUserDataAndHash(final Bundle ud) {
        if (!initialized) {
            Log.w(TAG, "initStore should have been called before calling setUserData");
            initAndWait();
        }

        AppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                lock.writeLock().lock();
                try {
                    updateHashUserData(ud);
                    sharedPreferences.edit()
                            .putString(USER_DATA_KEY, mapToJsonStr(hashedUserData))
                            .apply();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }

    static void setUserDataAndHash(
            @Nullable final String email,
            @Nullable final String firstName,
            @Nullable final String lastName,
            @Nullable final String phone,
            @Nullable final String dateOfBirth,
            @Nullable final String gender,
            @Nullable final String city,
            @Nullable final String state,
            @Nullable final String zip,
            @Nullable final String country) {
        Bundle ud = new Bundle();
        if (email != null) {
            ud.putString(EMAIL, email);
        }
        if (firstName != null) {
            ud.putString(FIRST_NAME, firstName);
        }
        if (lastName != null) {
            ud.putString(LAST_NAME, lastName);
        }
        if (phone != null) {
            ud.putString(PHONE, phone);
        }
        if (dateOfBirth != null) {
            ud.putString(DATE_OF_BIRTH, dateOfBirth);
        }
        if (gender != null) {
            ud.putString(GENDER, gender);
        }
        if (city != null) {
            ud.putString(CITY, city);
        }
        if (state != null) {
            ud.putString(STATE, state);
        }
        if (zip != null) {
            ud.putString(ZIP, zip);
        }
        if (country != null) {
            ud.putString(COUNTRY, country);
        }
        setUserDataAndHash(ud);
    }

    static void clear() {
        AppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                lock.writeLock().lock();
                try {
                    hashedUserData.clear();
                    sharedPreferences.edit().remove(USER_DATA_KEY).apply();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }

    static String getHashedUserData() {
        if (!initialized) {
            Log.w(TAG, "initStore should have been called before calling setUserID");
            initAndWait();
        }

        lock.readLock().lock();
        try {
            return mapToJsonStr(hashedUserData);
        } finally {
            lock.readLock().unlock();
        }
    }

    private static void initAndWait() {
        if (initialized) {
            return;
        }

        lock.writeLock().lock();
        try {
            if (initialized) {
                return;
            }

            sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(
                            FacebookSdk.getApplicationContext());
            String udRaw = sharedPreferences.getString(USER_DATA_KEY, "");
            hashedUserData = JsonStrToMap(udRaw);
            initialized = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void updateHashUserData(final Bundle ud) {
        if (ud == null) {
            return;
        }

        for (String key : ud.keySet()) {
            final Object rawVal = ud.get(key);
            if (rawVal == null) {
                continue;
            }
            final String value = rawVal.toString();
            if (maybeSHA256Hashed(value)) {
                hashedUserData.put(key, value.toLowerCase());
            } else {
                final String encryptedValue = encryptData(normalizeData(key, value));
                if (encryptedValue != null) {
                    hashedUserData.put(key, encryptedValue);
                }
            }
        }
    }

    @Nullable
    private static String encryptData(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        md.update(data.getBytes());
        return AppEventUtility.bytesToHex(md.digest());
    }

    private static String normalizeData(String type, String data) {
        data = data.trim().toLowerCase();

        if (EMAIL.equals(type)) {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(data).matches()) {
                return data;
            } else {
                Log.e(TAG, "Setting email failure: this is not a valid email address");
                return "";
            }
        }

        if (PHONE.equals(type)) {
            return data.replaceAll("[^0-9]", "");
        }

        if (GENDER.equals(type)) {
            data = data.length() > 0 ? data.substring(0, 1) : "";
            if ("f".equals(data) || "m".equals(data)) {
                return data;
            } else {
                Log.e(TAG, "Setting gender failure: the supported value for gender is f or m");
                return "";
            }
        }

        return data;
    }

    private static boolean maybeSHA256Hashed(String data) {
        return data.matches("[A-Fa-f0-9]{64}");
    }

    private static String mapToJsonStr(Map<String, String> map) {
        if (map.isEmpty()) {
            return "";
        }

        try {
            JSONObject jsonObject = new JSONObject();
            for (String key : map.keySet()) {
                jsonObject.put(key, map.get(key));
            }
            return jsonObject.toString();
        } catch (JSONException _e) {
            return "";
        }
    }

    private static Map<String, String> JsonStrToMap(String str) {
        if (str.isEmpty()) {
            return new HashMap<>();
        }

        try {
            Map<String, String> map = new HashMap<>();
            JSONObject jsonObject = new JSONObject(str);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, jsonObject.getString(key));
            }
            return map;
        } catch (JSONException _e) {
            return new HashMap<>();
        }
    }
}
