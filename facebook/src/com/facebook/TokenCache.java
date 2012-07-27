/**
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

// TODO: docs, particularly expectations around Bundle ownership/mutability
public abstract class TokenCache {
    public static final String TOKEN_KEY = "com.facebook.TokenCache.Token";
    public static final String EXPIRATION_DATE_KEY = "com.facebook.TokenCache.ExpirationDate";
    public static final String LAST_REFRESH_DATE_KEY = "com.facebook.TokenCache.LastRefreshDate";
    public static final String USER_FBID_KEY = "com.facebook.TokenCache.UserFBID";
    public static final String IS_SSO_KEY = "com.facebook.TokenCache.IsSSO";
    public static final String PERMISSIONS_KEY = "com.facebook.TokenCache.Permissions";
    private static final long INVALID_BUNDLE_MILLISECONDS = Long.MIN_VALUE;

    public abstract Bundle load();

    public abstract void save(Bundle bundle);

    public abstract void clear();

    public static boolean hasTokenInformation(Bundle bundle) {
        if (bundle == null) {
            return false;
        }

        String token = bundle.getString(TOKEN_KEY);
        if ((token == null) || (token.length() == 0)) {
            return false;
        }

        long expiresMilliseconds = bundle.getLong(EXPIRATION_DATE_KEY, 0L);
        if (expiresMilliseconds == 0L) {
            return false;
        }

        return true;
    }

    public static String getToken(Bundle bundle) {
        return bundle.getString(TOKEN_KEY);
    }

    public static void putToken(Bundle bundle, String value) {
        bundle.putString(TOKEN_KEY, value);
    }

    public static Date getExpirationDate(Bundle bundle) {
        return getDate(bundle, EXPIRATION_DATE_KEY);
    }

    public static void putExpirationDate(Bundle bundle, Date value) {
        putDate(bundle, EXPIRATION_DATE_KEY, value);
    }

    public static long getExpirationMilliseconds(Bundle bundle) {
        return bundle.getLong(EXPIRATION_DATE_KEY);
    }

    public static void putExpirationMilliseconds(Bundle bundle, long value) {
        bundle.putLong(EXPIRATION_DATE_KEY, value);
    }

    public static List<String> getPermissions(Bundle bundle) {
        return bundle.getStringArrayList(PERMISSIONS_KEY);
    }

    public static void putPermissions(Bundle bundle, List<String> value) {
        ArrayList<String> arrayList;
        if (!(value instanceof ArrayList<?>)) {
            arrayList = (ArrayList<String>) value;
        } else {
            arrayList = new ArrayList<String>(value);
        }
        bundle.putStringArrayList(PERMISSIONS_KEY, arrayList);
    }

    public static boolean getIsSSO(Bundle bundle) {
        return bundle.getBoolean(IS_SSO_KEY);
    }

    public static void putIsSSO(Bundle bundle, boolean value) {
        bundle.putBoolean(IS_SSO_KEY, value);
    }

    public static Date getLastRefreshDate(Bundle bundle) {
        return getDate(bundle, LAST_REFRESH_DATE_KEY);
    }

    public static void putLastRefreshDate(Bundle bundle, Date value) {
        putDate(bundle, LAST_REFRESH_DATE_KEY, value);
    }

    public static long getLastRefreshMilliseconds(Bundle bundle) {
        return bundle.getLong(LAST_REFRESH_DATE_KEY);
    }

    public static void putLastRefreshMilliseconds(Bundle bundle, long value) {
        bundle.putLong(LAST_REFRESH_DATE_KEY, value);
    }

    static Date getDate(Bundle bundle, String key) {
        if (bundle == null) {
            return null;
        }

        long n = bundle.getLong(key, INVALID_BUNDLE_MILLISECONDS);
        if (n == INVALID_BUNDLE_MILLISECONDS) {
            return null;
        }

        return new Date(n);
    }

    static void putDate(Bundle bundle, String key, Date date) {
        bundle.putLong(key, date.getTime());
    }
}
