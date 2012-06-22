/**
 * Copyright 2012 Facebook
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.FbDialog;

// TODO: docs
public class Session {
    public static final String TAG = Session.class.getCanonicalName();
    public static final int DEFAULT_AUTHORIZE_ACTIVITY_CODE = 0xface;

    private final String applicationId;
    private SessionStatusCallback callback;
    private final Activity activity;
    private final Handler handler;
    private volatile AuthRequest pendingRequest;
    private SessionState state;
    private final TokenCache tokenCache;
    private AccessToken tokenInfo;

    public Session(
            Activity activity,
            String applicationId,
            List<String> permissions,
            TokenCache tokenCache) {
        // Defaults
        if (permissions == null) {
            permissions = Collections.emptyList();
        }
        if (tokenCache == null) {
            tokenCache = new SharedPreferencesTokenCache(activity);
        }

        Validate.notNull(activity, "activity");
        Validate.notNull(applicationId, "applicationId");
        Validate.containsNoNulls(permissions, "permissions");

        this.applicationId = applicationId;
        this.activity = activity;
        this.handler = new Handler();
        this.tokenCache = tokenCache;
        this.state = SessionState.CREATED;

        Bundle tokenState = tokenCache.load();
        if (TokenCache.hasTokenInformation(tokenState)) {
            Date cachedExpirationDate = Utility.getBundleDate(
                    tokenState,
                    TokenCache.EXPIRATION_DATE_KEY);
            ArrayList<String> cachedPermissions = tokenState.getStringArrayList(TokenCache.PERMISSIONS_KEY);
            Date now = new Date();

            if ((cachedExpirationDate == null) ||
                    cachedExpirationDate.before(now) ||
                    !Utility.isSubset(permissions, cachedPermissions)) {
                // If expired or we require new permissions, clear out the current token cache.
                tokenCache.clear();
                this.tokenInfo = AccessToken.createEmptyToken(permissions);
            } else {
                // Otherwise we have a valid token, so use it.
                this.tokenInfo = AccessToken.createFromCache(tokenState);
                this.state = SessionState.CREATED_TOKEN_LOADED;
            }
        } else {
            this.tokenInfo = AccessToken.createEmptyToken(permissions);
        }
    }

    public final boolean getIsOpened() {
        return this.state.getIsOpened();
    }

    public final SessionState getState() {
        return this.state;
    }

    public final String getApplicationId() {
        return this.applicationId;
    }

    public final String getAccessToken() {
        return this.tokenInfo.getToken();
    }

    public final Date getExpirationDate() {
        return this.tokenInfo.getExpires();
    }

    public final List<String> getPermissions() {
        return this.tokenInfo.getPermissions();
    }

    public final void open(SessionStatusCallback callback) {
        open(callback, SessionLoginBehavior.SSO_WITH_FALLBACK, DEFAULT_AUTHORIZE_ACTIVITY_CODE);
    }

    public final void open(SessionStatusCallback callback, SessionLoginBehavior behavior, int activityCode) {
        SessionState newState;

        synchronized (this) {
            switch (this.state) {
            case CREATED:
                this.state = newState = SessionState.OPENING;
                break;
            case CREATED_TOKEN_LOADED:
                this.state = newState = SessionState.OPENED;
                break;
            default:
                throw new UnsupportedOperationException(
                        "Session: an attempt was made to open an already opened session."); // TODO localize
            }
            this.callback = callback;
            this.postStateChange(newState, null);
        }

        if (newState == SessionState.OPENING) {
            AuthRequest request = new AuthRequest(
                    behavior,
                    activityCode,
                    this.tokenInfo.getPermissions());

            this.authorize(request);
        }
    }

    public final boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        AuthRequest request = this.pendingRequest;
        if ((request == null) || (requestCode != request.getActivityCode())) {
            return false;
        }

        Exception exception = null;
        AccessToken newToken = null;

        if (resultCode == Activity.RESULT_CANCELED) {
            if (data == null) {
                // User pressed the 'back' button
                exception = new FacebookOperationCanceledException("TODO");
            } else {
                exception = new FacebookException(
                        data.getStringExtra("error"),
                        data.getIntExtra("error_code", -1),
                        data.getStringExtra("failing_url")); // TODO, last arg is not JSON
            }
        } else if (resultCode == Activity.RESULT_OK) {
            String error = data.getStringExtra("error");
            if (error == null) {
                error = data.getStringExtra("error_type");
            }
            if (error != null) {
                if (ServerProtocol.errorsProxyAuthDisabled.contains(error)) {
                    this.authorize(request.retry(AuthRequest.ALLOW_WEBVIEW_FLAG));
                } else if (ServerProtocol.errorsUserCanceled.contains(error)) {
                    exception = new FacebookOperationCanceledException("TODO");
                } else {
                    String description = data.getStringExtra("error_description");
                    if (description != null) {
                        error = error + ": " + description;
                    }
                    exception = new FacebookException(error);
                }
            } else {
                newToken = AccessToken.createFromSSO(this.pendingRequest.getPermissions(), data);
            }
        }

        if ((newToken != null) || (exception != null)) {
            this.finishAuth(newToken, exception);
        }

        return true;
    }

    public final void close() {
        synchronized (this) {
            switch (this.state) {
            case OPENING:
                this.state = SessionState.CLOSED_LOGIN_FAILED;
                this.postStateChange(
                        this.state,
                        new FacebookException("TODO"));
                break;

            case CREATED_TOKEN_LOADED:
            case OPENED:
            case OPENED_TOKEN_EXTENDED:
                this.state = SessionState.CLOSED;
                this.postStateChange(this.state, null);
                break;
            }
        }
    }

    public final void closeAndClearTokenInformation() {
        if (this.tokenCache != null) {
            this.tokenCache.clear();
        }
        close();
    }

    @Override public final String toString() {
        return new StringBuilder()
        .append("{Session")
        .append(" state:").append(this.state)
        .append(", token:").append((this.tokenInfo == null) ? "null" : this.tokenInfo)
        .append(", appId:").append((this.applicationId == null) ? "null" : this.applicationId)
        .append("}").toString();
    }

    private void authorize(AuthRequest request) {
        synchronized (this) {
            if (this.pendingRequest != null) {
                throw new FacebookException("TODO");
            }
            this.pendingRequest = request;
        }

        boolean started = false;

        if (!started && request.allowKatana()) {
            started = tryKatanaProxyAuth(request);
        }
        // TODO: support wakizashi in debug?
        // TODO: support browser?
        if (!started && request.allowWebView()) {
            started = tryDialogAuth(request);
        }

        if (!started) {
            synchronized (this) {
                switch (this.state) {
                case CLOSED:
                case CLOSED_LOGIN_FAILED:
                    return;

                    // TODO port: token refresh should not close if it comes through here.

                default:
                    this.state = SessionState.CLOSED_LOGIN_FAILED;
                    this.postStateChange(
                            this.state,
                            new FacebookException("TODO"));
                }
            }
        }
    }

    private boolean tryDialogAuth(final AuthRequest request) {
        int permissionCheck = this.activity.checkCallingOrSelfPermission(Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Builder builder = new Builder(this.activity);
            builder.setTitle("TODO");
            builder.setMessage("TODO");
            builder.create().show();
            return false;
        }

        Bundle parameters = new Bundle();
        if (!Utility.isNullOrEmpty(request.getPermissions())) {
            String scope = TextUtils.join(",", request.getPermissions());
            parameters.putString(ServerProtocol.DIALOG_PARAM_SCOPE, scope);
        }
        
        // TODO port: Facebook.java does this:
        // CookieSyncManager.createInstance(this.activity);
        
        final Session session = this;
        DialogListener listener = new DialogListener() {
            public void onComplete(Bundle bundle) {
                // TODO port: Facebook.java does this:
                // CookieSyncManager.getInstance().sync();
                AccessToken newToken = AccessToken.createFromDialog(
                        request.getPermissions(),
                        bundle);
                session.finishAuth(newToken, null);
            }

            public void onError(DialogError error) {
                Exception exception = new FacebookException("TODO");
                session.finishAuth(null, exception);
            }

            public void onFacebookError(FacebookError error) {
                Exception exception = new FacebookException("TODO");
                session.finishAuth(null, exception);
            }

            public void onCancel() {
                Exception exception = new FacebookOperationCanceledException("TODO");
                session.finishAuth(null, exception);
            }
        };

        parameters.putString(ServerProtocol.DIALOG_PARAM_DISPLAY, "touch");
        parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, "fbconnect://success");
        parameters.putString(ServerProtocol.DIALOG_PARAM_TYPE, "user_agent");
        parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, this.applicationId);

        Uri uri = Utility.buildUri(ServerProtocol.DIALOG_AUTHORITY, ServerProtocol.DIALOG_OAUTH_PATH, parameters);
        new FbDialog(this.activity, uri.toString(), listener).show();

        return true;
    }

    private boolean tryKatanaProxyAuth(AuthRequest request) {
        Intent intent = new Intent();

        intent.setClassName(NativeProtocol.KATANA_PACKAGE, NativeProtocol.KATANA_PROXY_AUTH_ACTIVITY);
        intent.putExtra("client_id", this.applicationId);

        if (!Utility.isNullOrEmpty(request.permissions)) {
            intent.putExtra("scope", TextUtils.join(",", request.permissions));
        }

        ResolveInfo resolveInfo =
                this.activity.getPackageManager().resolveActivity(intent, 0);
        if ((resolveInfo == null) ||
                !validateFacebookAppSignature(resolveInfo.activityInfo.packageName)) {

            return false;
        }

        try {
            this.activity.startActivityForResult(intent, request.activityCode);
        } catch (ActivityNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean validateFacebookAppSignature(String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = this.activity.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException e) {
            return false;
        }

        for (Signature signature : packageInfo.signatures) {
            if (signature.toCharsString().equals(NativeProtocol.KATANA_SIGNATURE)) {
                return true;
            }
        }

        return false;
    }

    private void finishAuth(AccessToken newToken, Exception exception) {
        // If the token we came up with is expired/invalid, then auth failed.
        if ((newToken != null) && newToken.isInvalid()) {
            newToken = null;
            exception = new FacebookException("TODO");
        }

        // Update the cache if we have a new token.
        if ((newToken != null) && (this.tokenCache != null)) {
            this.tokenCache.save(newToken.toCacheBundle());
        }

        synchronized (this) {
            switch (this.state) {
            case OPENING:
                if (newToken != null) {
                    this.tokenInfo = newToken;
                    this.state = SessionState.OPENED;
                } else if (exception != null) {
                    this.state = SessionState.CLOSED_LOGIN_FAILED;
                }
                this.postStateChange(this.state, exception);
                break;
            }
        }

    }

    private void postStateChange(
            final SessionState newState,
            final Exception error) {
        final SessionStatusCallback callback = this.callback;
        final Session session = this;

        if (callback != null) {
            Runnable closure = new Runnable() {
                public void run() {
                    callback.call(session, newState, error);
                }
            };

            this.handler.post(closure);
        }
    }

    private static final class AuthRequest {
        public static final int ALLOW_KATANA_FLAG = 0x1;
        public static final int ALLOW_WEBVIEW_FLAG = 0x8;

        private final int behaviorFlags;
        private final int activityCode;
        private final List<String> permissions;

        private AuthRequest(int behaviorFlags, int activityCode, List<String> permissions) {
            this.behaviorFlags = behaviorFlags;
            this.activityCode = activityCode;
            this.permissions = permissions;
        }

        public AuthRequest(SessionLoginBehavior behavior, int activityCode, List<String> permissions) {
            this(getFlags(behavior), activityCode, permissions);
        }

        public AuthRequest retry(int newBehaviorFlags) {
            return new AuthRequest(newBehaviorFlags,  this.activityCode, this.permissions);
        }

        public boolean allowKatana() {
            return (this.behaviorFlags & ALLOW_KATANA_FLAG) != 0;
        }

        public boolean allowWebView() {
            return (this.behaviorFlags & ALLOW_WEBVIEW_FLAG) != 0;
        }

        public int getActivityCode() {
            return this.activityCode;
        }

        public List<String> getPermissions() {
            return this.permissions;
        }

        private static final int getFlags(SessionLoginBehavior behavior) {
            switch (behavior) {
            case SSO_ONLY:
                return ALLOW_KATANA_FLAG;
            case SUPPRESS_SSO:
                return ALLOW_WEBVIEW_FLAG;
            default:
                return ALLOW_KATANA_FLAG|ALLOW_WEBVIEW_FLAG;
            }
        }
    }
}
