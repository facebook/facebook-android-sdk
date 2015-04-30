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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class AccessTokenManager {
    static final String TAG = "AccessTokenManager";

    static final String ACTION_CURRENT_ACCESS_TOKEN_CHANGED =
            "com.facebook.sdk.ACTION_CURRENT_ACCESS_TOKEN_CHANGED";
    static final String EXTRA_OLD_ACCESS_TOKEN =
            "com.facebook.sdk.EXTRA_OLD_ACCESS_TOKEN";
    static final String EXTRA_NEW_ACCESS_TOKEN =
            "com.facebook.sdk.EXTRA_NEW_ACCESS_TOKEN";
    static final String SHARED_PREFERENCES_NAME =
            "com.facebook.AccessTokenManager.SharedPreferences";

    // Token extension constants
    private static final int TOKEN_EXTEND_THRESHOLD_SECONDS = 24 * 60 * 60; // 1 day
    private static final int TOKEN_EXTEND_RETRY_SECONDS = 60 * 60; // 1 hour

    private static final String TOKEN_EXTEND_GRAPH_PATH = "oauth/access_token";
    private static final String ME_PERMISSIONS_GRAPH_PATH = "me/permissions";

    private static volatile AccessTokenManager instance;

    private final LocalBroadcastManager localBroadcastManager;
    private final AccessTokenCache accessTokenCache;
    private AccessToken currentAccessToken;
    private AtomicBoolean tokenRefreshInProgress = new AtomicBoolean(false);
    private Date lastAttemptedTokenExtendDate = new Date(0);

    AccessTokenManager(LocalBroadcastManager localBroadcastManager,
                       AccessTokenCache accessTokenCache) {

        Validate.notNull(localBroadcastManager, "localBroadcastManager");
        Validate.notNull(accessTokenCache, "accessTokenCache");

        this.localBroadcastManager = localBroadcastManager;
        this.accessTokenCache = accessTokenCache;
    }

    static AccessTokenManager getInstance() {
        if (instance == null) {
            synchronized (AccessTokenManager.class) {
                if (instance == null) {
                    Context applicationContext = FacebookSdk.getApplicationContext();
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(
                            applicationContext);
                    AccessTokenCache accessTokenCache = new AccessTokenCache();

                    instance = new AccessTokenManager(localBroadcastManager, accessTokenCache);
                }
            }
        }

        return instance;
    }

    AccessToken getCurrentAccessToken() {
        return currentAccessToken;
    }

    boolean loadCurrentAccessToken() {
        AccessToken accessToken = accessTokenCache.load();

        if (accessToken != null) {
            setCurrentAccessToken(accessToken, false);
            return true;
        }

        return false;
    }

    void setCurrentAccessToken(AccessToken currentAccessToken) {
        setCurrentAccessToken(currentAccessToken, true);
    }

    private void setCurrentAccessToken(AccessToken currentAccessToken, boolean saveToCache) {
        AccessToken oldAccessToken = this.currentAccessToken;
        this.currentAccessToken = currentAccessToken;
        tokenRefreshInProgress.set(false);
        this.lastAttemptedTokenExtendDate = new Date(0);

        if (saveToCache) {
            if (currentAccessToken != null) {
                accessTokenCache.save(currentAccessToken);
            } else {
                accessTokenCache.clear();
                Utility.clearFacebookCookies(FacebookSdk.getApplicationContext());
            }
        }

        if (!Utility.areObjectsEqual(oldAccessToken, currentAccessToken)) {
            sendCurrentAccessTokenChangedBroadcast(oldAccessToken, currentAccessToken);
        }
    }

    private void sendCurrentAccessTokenChangedBroadcast(AccessToken oldAccessToken,
        AccessToken currentAccessToken) {
        Intent intent = new Intent(ACTION_CURRENT_ACCESS_TOKEN_CHANGED);

        intent.putExtra(EXTRA_OLD_ACCESS_TOKEN, oldAccessToken);
        intent.putExtra(EXTRA_NEW_ACCESS_TOKEN, currentAccessToken);

        localBroadcastManager.sendBroadcast(intent);
    }

    void extendAccessTokenIfNeeded() {
        if (!shouldExtendAccessToken()) {
            return;
        }
        refreshCurrentAccessToken();
    }

    private boolean shouldExtendAccessToken() {
        if (currentAccessToken == null) {
            return false;
        }
        Long now = new Date().getTime();

        return currentAccessToken.getSource().canExtendToken()
                && now - lastAttemptedTokenExtendDate.getTime() > TOKEN_EXTEND_RETRY_SECONDS * 1000
                && now - currentAccessToken.getLastRefresh().getTime() >
                    TOKEN_EXTEND_THRESHOLD_SECONDS * 1000;
    }

    private static GraphRequest createGrantedPermissionsRequest(
            AccessToken accessToken,
            GraphRequest.Callback callback
    ) {
        Bundle parameters = new Bundle();
        return new GraphRequest(
                accessToken,
                ME_PERMISSIONS_GRAPH_PATH,
                parameters,
                HttpMethod.GET,
                callback);
    }

    private static GraphRequest createExtendAccessTokenRequest(
            AccessToken accessToken,
            GraphRequest.Callback callback
    ) {
        Bundle parameters = new Bundle();
        parameters.putString("grant_type", "fb_extend_sso_token");
        return new GraphRequest(
                accessToken,
                TOKEN_EXTEND_GRAPH_PATH,
                parameters,
                HttpMethod.GET,
                callback);
    }

    private static class RefreshResult {
        public String accessToken;
        public int expiresAt;
    }

    void refreshCurrentAccessToken() {
        if (Looper.getMainLooper().equals(Looper.myLooper())) {
            refreshCurrentAccessTokenImpl();
        } else {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshCurrentAccessTokenImpl();
                }
            });
        }
    }

    private void refreshCurrentAccessTokenImpl() {
        final AccessToken accessToken = currentAccessToken;
        if (accessToken == null) {
            return;
        }
        if (!tokenRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        Validate.runningOnUiThread();

        lastAttemptedTokenExtendDate = new Date();

        final Set<String> permissions = new HashSet<>();
        final Set<String> declinedPermissions = new HashSet<>();
        final AtomicBoolean permissionsCallSucceeded = new AtomicBoolean(false);
        final RefreshResult refreshResult = new RefreshResult();

        GraphRequestBatch batch = new GraphRequestBatch(
                createGrantedPermissionsRequest(accessToken, new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject result = response.getJSONObject();
                        if (result == null) {
                            return;
                        }
                        JSONArray permissionsArray = result.optJSONArray("data");
                        if (permissionsArray == null) {
                            return;
                        }
                        permissionsCallSucceeded.set(true);
                        for (int i = 0; i < permissionsArray.length(); i++) {
                            JSONObject permissionEntry = permissionsArray.optJSONObject(i);
                            if (permissionEntry == null) {
                                continue;
                            }
                            String permission = permissionEntry.optString("permission");
                            String status = permissionEntry.optString("status");
                            if (!Utility.isNullOrEmpty(permission) &&
                                    !Utility.isNullOrEmpty(status)) {
                                status = status.toLowerCase(Locale.US);
                                if (status.equals("granted")) {
                                    permissions.add(permission);
                                } else if (status.equals("declined")) {
                                    declinedPermissions.add(permission);
                                } else {
                                    Log.w(TAG, "Unexpected status: " + status);
                                }
                            }
                        }
                    }
                }),
                createExtendAccessTokenRequest(accessToken, new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject data = response.getJSONObject();
                        if (data == null) {
                            return;
                        }
                        refreshResult.accessToken = data.optString("access_token");
                        refreshResult.expiresAt = data.optInt("expires_at");
                    }
                })
        );

        batch.addCallback(new GraphRequestBatch.Callback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
                if (getInstance().getCurrentAccessToken() == null ||
                        getInstance().getCurrentAccessToken().getUserId()
                                != accessToken.getUserId()) {
                    return;
                }
                try {
                    if (permissionsCallSucceeded.get() == false &&
                            refreshResult.accessToken == null &&
                            refreshResult.expiresAt == 0) {
                        return;
                    }
                    AccessToken newAccessToken = new AccessToken(
                            refreshResult.accessToken != null ? refreshResult.accessToken :
                                    accessToken.getToken(),
                            accessToken.getApplicationId(),
                            accessToken.getUserId(),
                            permissionsCallSucceeded.get()
                                    ? permissions : accessToken.getPermissions(),
                            permissionsCallSucceeded.get()
                                    ? declinedPermissions : accessToken.getDeclinedPermissions(),
                            accessToken.getSource(),
                            refreshResult.expiresAt != 0
                                    ? new Date(refreshResult.expiresAt * 1000l)
                                    : accessToken.getExpires(),
                            new Date()
                    );
                    getInstance().setCurrentAccessToken(newAccessToken);
                } finally {
                    tokenRefreshInProgress.set(false);
                }
            }
        });
        batch.executeAsync();
    }
}
