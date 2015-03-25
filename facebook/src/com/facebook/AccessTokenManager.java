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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.internal.NativeProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import java.util.Date;

final class AccessTokenManager {
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

    private static volatile AccessTokenManager instance;

    private final LocalBroadcastManager localBroadcastManager;
    private final AccessTokenCache accessTokenCache;
    private AccessToken currentAccessToken;
    private TokenRefreshRequest currentTokenRefreshRequest;
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
        this.currentTokenRefreshRequest = null;
        this.lastAttemptedTokenExtendDate = new Date(0);

        if (saveToCache) {
            if (currentAccessToken != null) {
                accessTokenCache.save(currentAccessToken);
            } else {
                accessTokenCache.clear();
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
        currentTokenRefreshRequest = new TokenRefreshRequest(currentAccessToken);
        currentTokenRefreshRequest.bind();
    }

    private boolean shouldExtendAccessToken() {
        if (currentAccessToken == null || currentTokenRefreshRequest != null) {
            return false;
        }
        Long now = new Date().getTime();

        return currentAccessToken.getSource().canExtendToken()
                && now - lastAttemptedTokenExtendDate.getTime() > TOKEN_EXTEND_RETRY_SECONDS * 1000
                && now - currentAccessToken.getLastRefresh().getTime() >
                    TOKEN_EXTEND_THRESHOLD_SECONDS * 1000;
    }

    class TokenRefreshRequest implements ServiceConnection {
        final Messenger messageReceiver;
        Messenger messageSender = null;

        TokenRefreshRequest(AccessToken accessToken) {
            this.messageReceiver = new Messenger(
                    new TokenRefreshRequestHandler(accessToken, this));
        }

        public void bind() {
            Context context = FacebookSdk.getApplicationContext();
            Intent intent = NativeProtocol.createTokenRefreshIntent(context);
            if (intent != null && context.bindService(intent, this, Context.BIND_AUTO_CREATE)) {
                lastAttemptedTokenExtendDate = new Date();
            } else {
                cleanup();
            }
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            messageSender = new Messenger(service);
            refreshToken();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg) {
            cleanup();

            try {
                // We returned an error so there's no point in keeping the binding open.
                FacebookSdk.getApplicationContext().unbindService(TokenRefreshRequest.this);
            } catch (IllegalArgumentException ex) {
                // Do nothing, the connection was already unbound
            }
        }

        private void cleanup() {
            if (currentTokenRefreshRequest == this) {
                currentTokenRefreshRequest = null;
            }
        }

        private void refreshToken() {
            Bundle requestData = new Bundle();
            requestData.putString(AccessToken.ACCESS_TOKEN_KEY, getCurrentAccessToken().getToken());

            Message request = Message.obtain();
            request.setData(requestData);
            request.replyTo = messageReceiver;

            try {
                messageSender.send(request);
            } catch (RemoteException e) {
                cleanup();
            }
        }
    }

    static class TokenRefreshRequestHandler extends Handler {
        private AccessToken accessToken;
        private TokenRefreshRequest tokenRefreshRequest;

        TokenRefreshRequestHandler(
                AccessToken accessToken,
                TokenRefreshRequest tokenRefreshRequest) {
            super(Looper.getMainLooper());
            this.accessToken = accessToken;
            this.tokenRefreshRequest = tokenRefreshRequest;
        }

        @Override
        public void handleMessage(Message msg) {
            AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
            if (currentAccessToken != null && currentAccessToken.equals(accessToken) &&
                    msg.getData().getString(AccessToken.ACCESS_TOKEN_KEY) != null) {
                AccessToken newToken = AccessToken.createFromRefresh(accessToken, msg.getData());
                AccessToken.setCurrentAccessToken(newToken);
            }

            // The refreshToken function should be called rarely, so there is no point in keeping
            // the binding open.
            FacebookSdk.getApplicationContext().unbindService(tokenRefreshRequest);
            tokenRefreshRequest.cleanup();
        }
    }
}
