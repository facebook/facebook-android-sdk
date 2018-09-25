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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserDataStore {
    private static final String TAG = UserDataStore.class.getSimpleName();
    private static final String USER_DATA_KEY =
            "com.facebook.appevents.UserDataStore.userData";

    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static String hashedUserData;
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

    public static void initStore() {
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

    public static void setUserDataAndHash(final Bundle ud) {
        if (!initialized) {
            Log.w(TAG, "initStore should have been called before calling setUserData");
            initAndWait();
        }

        AppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                lock.writeLock().lock();
                try {
                    hashedUserData = hashUserData(ud);
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(
                                    FacebookSdk.getApplicationContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(USER_DATA_KEY, hashedUserData);
                    editor.apply();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }

    public static void setUserDataAndHash(
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


    public static String getHashedUserData() {
        if (!initialized) {
            Log.w(TAG, "initStore should have been called before calling setUserID");
            initAndWait();
        }

        lock.readLock().lock();
        try {
            return hashedUserData;
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

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                    FacebookSdk.getApplicationContext());
            hashedUserData = sharedPreferences.getString(USER_DATA_KEY, null);
            initialized = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static String hashUserData(final Bundle ud) {
        if (ud == null) {
            return null;
        }

        JSONObject hashedUserData = new JSONObject();
        for (String key : ud.keySet()) {
            try {
                final String value = ud.get(key).toString();
                if (maybeSHA256Hashed(value)) {
                    hashedUserData.put(key, value.toLowerCase());
                } else {
                    final String normalizedValue = normalizeData(key, ud.get(key).toString());
                    final String encryptedValue = encryptData(normalizedValue);
                    if (encryptedValue != null) {
                        hashedUserData.put(key, encryptedValue);
                    }
                }
            } catch (JSONException _e) {

            }
        }

        return hashedUserData.toString();
    }

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
        String normalizedData = "";
        switch (type) {
            case EMAIL:
            case FIRST_NAME:
            case LAST_NAME:
            case CITY:
            case STATE:
            case COUNTRY:
                normalizedData = data.trim().toLowerCase();
                break;
            case PHONE:
                normalizedData = data.trim().replaceAll("[^0-9]", "");
                break;
            case GENDER:
                String temp = data.trim().toLowerCase();
                normalizedData = temp.length() > 0 ? temp.substring(0, 1) : "";
                break;
            default:
                break;
        }

        return normalizedData;
    }

    private static boolean maybeSHA256Hashed(String data) {
        return data.matches("[A-Fa-f0-9]{64}");
    }
}
