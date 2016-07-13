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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

class AccessTokenCache {
    static final String CACHED_ACCESS_TOKEN_KEY
            = "com.facebook.AccessTokenManager.CachedAccessToken";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferencesTokenCachingStrategyFactory tokenCachingStrategyFactory;
    private LegacyTokenHelper tokenCachingStrategy;

    AccessTokenCache(SharedPreferences sharedPreferences,
                     SharedPreferencesTokenCachingStrategyFactory tokenCachingStrategyFactory) {
        this.sharedPreferences = sharedPreferences;
        this.tokenCachingStrategyFactory = tokenCachingStrategyFactory;
    }

    public AccessTokenCache() {
        this(
                FacebookSdk.getApplicationContext().getSharedPreferences(
                        AccessTokenManager.SHARED_PREFERENCES_NAME,
                        Context.MODE_PRIVATE),
                new SharedPreferencesTokenCachingStrategyFactory()
        );
    }

    public AccessToken load() {
        AccessToken accessToken = null;
        if (hasCachedAccessToken()) {
            // If we have something cached, we try to use it; even if it is invalid, do not fall
            // back to a legacy caching strategy.
            accessToken = getCachedAccessToken();
        } else if (shouldCheckLegacyToken()) {
            accessToken = getLegacyAccessToken();

            if (accessToken != null) {
                save(accessToken);
                getTokenCachingStrategy().clear();
            }
        }

        return accessToken;
    }

    public void save(AccessToken accessToken) {
        Validate.notNull(accessToken, "accessToken");

        JSONObject jsonObject = null;
        try {
            jsonObject = accessToken.toJSONObject();
            sharedPreferences.edit().putString(CACHED_ACCESS_TOKEN_KEY, jsonObject.toString())
                    .apply();
        } catch (JSONException e) {
            // Can't recover
        }
    }

    public void clear() {
        sharedPreferences.edit().remove(CACHED_ACCESS_TOKEN_KEY).apply();
        if (shouldCheckLegacyToken()) {
            getTokenCachingStrategy().clear();
        }
    }

    private boolean hasCachedAccessToken() {
        return sharedPreferences.contains(CACHED_ACCESS_TOKEN_KEY);
    }

    private AccessToken getCachedAccessToken() {
        String jsonString = sharedPreferences.getString(CACHED_ACCESS_TOKEN_KEY, null);
        if (jsonString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                return AccessToken.createFromJSONObject(jsonObject);
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }

    private boolean shouldCheckLegacyToken() {
        return FacebookSdk.isLegacyTokenUpgradeSupported();
    }

    private AccessToken getLegacyAccessToken() {
        AccessToken accessToken = null;
        Bundle bundle = getTokenCachingStrategy().load();

        if (bundle != null && LegacyTokenHelper.hasTokenInformation(bundle)) {
            accessToken = AccessToken.createFromLegacyCache(bundle);
        }
        return accessToken;
    }

    private LegacyTokenHelper getTokenCachingStrategy() {
        if (tokenCachingStrategy == null) {
            synchronized (this) {
                if (tokenCachingStrategy == null) {
                    tokenCachingStrategy = tokenCachingStrategyFactory.create();
                }
            }
        }
        return tokenCachingStrategy;
    }

    static class SharedPreferencesTokenCachingStrategyFactory {
        public LegacyTokenHelper create() {
            return new LegacyTokenHelper(FacebookSdk.getApplicationContext());
        }
    }
}
