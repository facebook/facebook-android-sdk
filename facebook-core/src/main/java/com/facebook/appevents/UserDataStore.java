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
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UserDataStore {
    private static final String TAG = UserDataStore.class.getSimpleName();
    private static final String USER_DATA_KEY =
            "com.facebook.appevents.UserDataStore.userData";
    private static final String INTERNAL_USER_DATA_KEY =
            "com.facebook.appevents.UserDataStore.internalUserData";

    private static SharedPreferences sharedPreferences;
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static final int MAX_NUM = 5;
    private static final String DATA_SEPARATOR = ",";
    private static final ConcurrentHashMap<String, String> externalHashedUserData
            = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> internalHashedUserData
            = new ConcurrentHashMap<>();

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
        if (initialized.get()) {
            return;
        }
        initAndWait();
    }

    private static void writeDataIntoCache(final String key, final String value) {
        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!initialized.get()) {
                    initAndWait();
                }
                sharedPreferences.edit()
                        .putString(key, value)
                        .apply();
            }
        });
    }

    static void setUserDataAndHash(final Bundle ud) {
        InternalAppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!initialized.get()) {
                    Log.w(TAG, "initStore should have been called before calling setUserData");
                    initAndWait();
                }

                updateHashUserData(ud);

                writeDataIntoCache(USER_DATA_KEY, Utility.mapToJsonStr(externalHashedUserData));
                writeDataIntoCache(INTERNAL_USER_DATA_KEY,
                        Utility.mapToJsonStr(internalHashedUserData));
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
        InternalAppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!initialized.get()) {
                    Log.w(TAG, "initStore should have been called before calling setUserData");
                    initAndWait();
                }
                externalHashedUserData.clear();
                sharedPreferences.edit().putString(USER_DATA_KEY, null).apply();
            }
        });
    }

    public static void removeRules(List<String> rules) {
        if (!initialized.get()) {
            initAndWait();
        }
        for (String rule : rules) {
            if (internalHashedUserData.containsKey(rule)) {
                internalHashedUserData.remove(rule);
            }
        }
        writeDataIntoCache(INTERNAL_USER_DATA_KEY, Utility.mapToJsonStr(internalHashedUserData));
    }

    static String getHashedUserData() {
        if (!initialized.get()) {
            Log.w(TAG, "initStore should have been called before calling setUserID");
            initAndWait();
        }
        return Utility.mapToJsonStr(externalHashedUserData);
    }

    public static String getAllHashedUserData() {
        if (!initialized.get()) {
            initAndWait();
        }
        Map<String, String> allHashedUserData = new HashMap<>();
        allHashedUserData.putAll(externalHashedUserData);
        allHashedUserData.putAll(internalHashedUserData);
        return Utility.mapToJsonStr(allHashedUserData);
    }

    private synchronized static void initAndWait() {
        if (initialized.get()) {
            return;
        }
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(
                        FacebookSdk.getApplicationContext());
        String externalUdRaw = sharedPreferences.getString(USER_DATA_KEY, "");
        String internalUdRaw = sharedPreferences.getString(INTERNAL_USER_DATA_KEY, "");
        externalHashedUserData.putAll(Utility.JsonStrToMap(externalUdRaw));
        internalHashedUserData.putAll(Utility.JsonStrToMap(internalUdRaw));
        initialized.set(true);
    }

    public static Map<String, String> getInternalHashedUserData() {
        if (!initialized.get()) {
            initAndWait();
        }
        return new HashMap<>(internalHashedUserData);
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
                externalHashedUserData.put(key, value.toLowerCase());
            } else {
                final String encryptedValue = Utility.sha256hash(normalizeData(key, value));
                if (encryptedValue != null) {
                    externalHashedUserData.put(key, encryptedValue);
                }
            }
        }
    }

    static void setInternalUd(final Map<String, String> ud) {
        if (!initialized.get()) {
            initAndWait();
        }
        for (Map.Entry<String, String> entry : ud.entrySet()) {
            final String key = entry.getKey();
            final String rawVal = ud.get(key);
            final String value = Utility.sha256hash(normalizeData(key, rawVal.trim()));
            if (internalHashedUserData.containsKey(key)) {
                String originalVal = internalHashedUserData.get(key);
                String[] previousData;
                if (originalVal != null) {
                    previousData = originalVal.split(DATA_SEPARATOR);
                } else {
                    previousData = new String[]{
                    };
                }
                Set<String> set = new HashSet<>(Arrays.asList(previousData));
                if (set.contains(value)) {
                    return;
                }
                StringBuilder sb = new StringBuilder();

                if (previousData.length == 0) {
                    sb.append(value);
                } else if (previousData.length < MAX_NUM) {
                    sb.append(originalVal).append(DATA_SEPARATOR).append(value);
                } else {
                    for (int i = 1; i < MAX_NUM; i++) {
                        sb.append(previousData[i]).append(DATA_SEPARATOR);
                    }
                    sb.append(value);
                    set.remove(previousData[0]);
                }
                // Update new added value into hashed User Data and save to cache
                internalHashedUserData.put(key, sb.toString());
            } else {
                internalHashedUserData.put(key, value);
            }
        }

        writeDataIntoCache(INTERNAL_USER_DATA_KEY, Utility.mapToJsonStr(internalHashedUserData));
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
}
