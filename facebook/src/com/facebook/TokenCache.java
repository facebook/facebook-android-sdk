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

import android.os.Bundle;

public class TokenCache {
    public static final String TOKEN_KEY = "com.facebook.TokenCache.Token";
    public static final String EXPIRATION_DATE_KEY = "com.facebook.TokenCache.ExpirationDate";
    public static final String LAST_REFRESH_DATE_KEY = "com.facebook.TokenCache.LastRefreshDate";
    public static final String USER_FBID_KEY = "com.facebook.TokenCache.UserFBID";
    public static final String IS_SSO_KEY = "com.facebook.TokenCache.IsSSO";
    public static final String PERMISSIONS_KEY = "com.facebook.TokenCache.Permissions";

    public TokenCache() {
    }

    public TokenCache(String cacheKey) {
    }

    public String getCacheKey() {
        return null;
    }

    public Bundle load() {
        return null;
    }

    public void save(Bundle bundle) {
    }

    public void clear() {
    }

    @Override public String toString() {
        return null;
    }
}
