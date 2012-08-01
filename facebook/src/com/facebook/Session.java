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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.FbDialog;

// TODO: docs
public class Session {
    public static final String TAG = Session.class.getCanonicalName();
    public static final int DEFAULT_AUTHORIZE_ACTIVITY_CODE = 0xface;
    public static final String WEB_VIEW_ERROR_CODE_KEY = "com.facebook.sdk.WebViewErrorCode";
    public static final String WEB_VIEW_FAILING_URL_KEY = "com.facebook.sdk.FailingUrl";
    public static final String ACTION_ACTIVE_SESSION_SET = "com.facebook.sdk.ACTIVE_SESSION_SET";
    public static final String ACTION_ACTIVE_SESSION_UNSET = "com.facebook.sdk.ACTIVE_SESSION_UNSET";
    public static final String ACTION_ACTIVE_SESSION_OPENED = "com.facebook.sdk.ACTIVE_SESSION_OPENED";
    public static final String ACTION_ACTIVE_SESSION_CLOSED = "com.facebook.sdk.ACTIVE_SESSION_CLOSED";
    public static final String APPLICATION_ID_PROPERTY = "com.facebook.sdk.ApplicationId";
    
    private static Object staticLock = new Object();
    private static Session activeSession;
    private static List<ActiveSessionRegistration> activeSessionCallbacks = new ArrayList<ActiveSessionRegistration>();
    private static volatile Context applicationContext;

    private final String applicationId;
    private volatile Bundle authorizationBundle;
    private SessionStatusCallback callback;
    private final Handler handler;
    private volatile AuthRequest pendingRequest;
    private SessionState state;
    // This is the object that synchronizes access to state and tokenInfo
    private final Object lock = new Object();
    private final TokenCache tokenCache;
    private AccessToken tokenInfo;

    public Session(Context currentContext, String applicationId) {
        this(currentContext, applicationId, null, null, null);
    }

    public Session(Context currentContext, String applicationId, List<String> permissions, TokenCache tokenCache) {
        this(currentContext, applicationId, permissions, tokenCache, null);
    }

    Session(Context currentContext, String applicationId, List<String> permissions, TokenCache tokenCache,
            Handler handler) {
        if (permissions == null) {
            permissions = Collections.emptyList();
        }

        // if the application ID passed in is null, try to get it from the meta-data in the manifest.
        if (applicationId == null) {
            try {
                ApplicationInfo ai = currentContext.getPackageManager().
                        getApplicationInfo(currentContext.getPackageName(), PackageManager.GET_META_DATA);
                applicationId = ai.metaData.getString(APPLICATION_ID_PROPERTY);
            } catch (NameNotFoundException e) {
                // if we can't find it in the manifest, just leave it as null, and the validator will
                // catch it
            }
        }
        
        Validate.notNull(currentContext, "currentContext");
        Validate.notNull(applicationId, "applicationId");
        Validate.containsNoNulls(permissions, "permissions");

        applicationContext = currentContext.getApplicationContext();

        if (tokenCache == null) {
            tokenCache = new SharedPreferencesTokenCache(applicationContext);
        }

        this.applicationId = applicationId;
        this.tokenCache = tokenCache;
        this.state = SessionState.CREATED;

        // - If we are given a handler, use it.
        // - Otherwise, if we are associated with a Looper, create a Handler so
        // that callbacks return to this thread.
        // - If handler is null and we are not on a Looper thread, set
        // this.handler
        // to null so that we post callbacks to a threadpool thread.
        if ((handler == null) && (Looper.myLooper() != null)) {
            handler = new Handler();
        }
        this.handler = handler;

        Bundle tokenState = tokenCache.load();
        if (TokenCache.hasTokenInformation(tokenState)) {
            Date cachedExpirationDate = TokenCache.getDate(tokenState, TokenCache.EXPIRATION_DATE_KEY);
            ArrayList<String> cachedPermissions = tokenState.getStringArrayList(TokenCache.PERMISSIONS_KEY);
            Date now = new Date();

            if ((cachedExpirationDate == null) || cachedExpirationDate.before(now)
                    || !Utility.isSubset(permissions, cachedPermissions)) {
                // If expired or we require new permissions, clear out the
                // current token cache.
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

    public final Bundle getAuthorizationBundle() {
        synchronized (this.lock) {
            return this.authorizationBundle;
        }
    }

    public final boolean getIsOpened() {
        synchronized (this.lock) {
            return this.state.getIsOpened();
        }
    }

    public final SessionState getState() {
        synchronized (this.lock) {
            return this.state;
        }
    }

    public final String getApplicationId() {
        return this.applicationId;
    }

    public final String getAccessToken() {
        synchronized (this.lock) {
            return this.tokenInfo.getToken();
        }
    }

    public final Date getExpirationDate() {
        synchronized (this.lock) {
            return this.tokenInfo.getExpires();
        }
    }

    public final List<String> getPermissions() {
        synchronized (this.lock) {
            return this.tokenInfo.getPermissions();
        }
    }

    public final void open(Activity currentActivity, SessionStatusCallback callback) {
        open(currentActivity, callback, SessionLoginBehavior.SSO_WITH_FALLBACK, DEFAULT_AUTHORIZE_ACTIVITY_CODE);
    }

    public final void open(Activity currentActivity, SessionStatusCallback callback, SessionLoginBehavior behavior,
            int activityCode) {
        SessionState newState;

        synchronized (this.lock) {
            final SessionState oldState = this.state;

            switch (this.state) {
            case CREATED:
                Validate.notNull(currentActivity,  "currentActivity");
                this.state = newState = SessionState.OPENING;
                break;
            case CREATED_TOKEN_LOADED:
                this.state = newState = SessionState.OPENED;
                break;
            default:
                throw new UnsupportedOperationException(
                        "Session: an attempt was made to open an already opened session."); // TODO
                                                                                            // localize
            }
            this.callback = callback;
            this.postStateChange(oldState, newState, null);
        }

        if (newState == SessionState.OPENING) {
            AuthRequest request = new AuthRequest(behavior, activityCode, this.tokenInfo.getPermissions());

            authorize(currentActivity, request);
        }
    }

    public final boolean onActivityResult(Activity currentActivity, int requestCode, int resultCode, Intent data) {
        Validate.notNull(currentActivity, "currentActivity");
        Validate.notNull(data, "data");

        AuthRequest request = this.pendingRequest;
        if ((request == null) || (requestCode != request.getActivityCode())) {
            return false;
        }

        AccessToken newToken = null;
        Exception exception = null;
        this.authorizationBundle = null;

        if (resultCode == Activity.RESULT_CANCELED) {
            if (data == null) {
                // User pressed the 'back' button
                exception = new FacebookOperationCanceledException("TODO");
            } else {
                this.authorizationBundle = data.getExtras();
                exception = new FacebookAuthorizationException(this.authorizationBundle.getString("error"));
            }
        } else if (resultCode == Activity.RESULT_OK) {
            this.authorizationBundle = data.getExtras();
            String error = this.authorizationBundle.getString("error");
            if (error == null) {
                error = this.authorizationBundle.getString("error_type");
            }
            if (error != null) {
                if (ServerProtocol.errorsProxyAuthDisabled.contains(error)) {
                    authorize(currentActivity, request.retry(AuthRequest.ALLOW_WEBVIEW_FLAG));
                } else if (ServerProtocol.errorsUserCanceled.contains(error)) {
                    exception = new FacebookOperationCanceledException("TODO");
                } else {
                    String description = this.authorizationBundle.getString("error_description");
                    if (description != null) {
                        error = error + ": " + description;
                    }
                    exception = new FacebookAuthorizationException(error);
                }
            } else {
                newToken = AccessToken.createFromSSO(this.pendingRequest.getPermissions(), data);
            }
        }

        if ((newToken != null) || (exception != null)) {
            finishAuth(currentActivity, newToken, exception);
        }

        return true;
    }

    public final void close() {
        synchronized (this.lock) {
            final SessionState oldState = this.state;

            switch (this.state) {
            case OPENING:
                this.state = SessionState.CLOSED_LOGIN_FAILED;
                postStateChange(oldState, this.state, new FacebookException(
                        "TODO exception for transitioning to CLOSED_LOGIN_FAILED state"));
                break;

            case CREATED_TOKEN_LOADED:
            case OPENED:
            case OPENED_TOKEN_EXTENDED:
                this.state = SessionState.CLOSED;
                postStateChange(oldState, this.state, null);
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

    @Override
    public final String toString() {
        return new StringBuilder().append("{Session").append(" state:").append(this.state).append(", token:")
                .append((this.tokenInfo == null) ? "null" : this.tokenInfo).append(", appId:")
                .append((this.applicationId == null) ? "null" : this.applicationId).append("}").toString();
    }

    public void internalRefreshToken(Bundle bundle) {
        synchronized (this.lock) {
            final SessionState oldState = this.state;

            switch (this.state) {
            case OPENED:
                this.state = SessionState.OPENED_TOKEN_EXTENDED;
                postStateChange(oldState, this.state, null);
                break;
            case OPENED_TOKEN_EXTENDED:
                break;
            default:
                // Silently ignore attempts to refresh token if we are not open
                Log.d(TAG, "refreshToken ignored in state " + this.state);
                return;
            }
            this.tokenInfo = AccessToken.createForRefresh(this.tokenInfo, bundle);
        }
    }

    void authorize(Activity currentActivity, AuthRequest request) {
        synchronized (this.lock) {
            if (this.pendingRequest != null) {
                throw new FacebookException("TODO");
            }
            this.pendingRequest = request;
        }

        boolean started = false;

        if (!started && request.allowKatana()) {
            started = tryKatanaProxyAuth(currentActivity, request);
        }
        // TODO: support wakizashi in debug?
        // TODO: support browser?
        if (!started && request.allowWebView()) {
            started = tryDialogAuth(currentActivity, request);
        }

        if (!started) {
            synchronized (this.lock) {
                final SessionState oldState = this.state;

                switch (this.state) {
                case CLOSED:
                case CLOSED_LOGIN_FAILED:
                    return;

                    // TODO port: token refresh should not close if it comes
                    // through here.

                default:
                    this.state = SessionState.CLOSED_LOGIN_FAILED;
                    postStateChange(oldState, this.state, new FacebookException("TODO"));
                }
            }
        }
    }

    private boolean tryDialogAuth(final Activity currentActivity, final AuthRequest request) {
        int permissionCheck = currentActivity.checkCallingOrSelfPermission(Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Builder builder = new Builder(currentActivity);
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
        // CookieSyncManager.createInstance(currentActivity);

        DialogListener listener = new DialogListener() {
            public void onComplete(Bundle bundle) {
                // TODO port: Facebook.java does this:
                // CookieSyncManager.getInstance().sync();
                AccessToken newToken = AccessToken.createFromDialog(request.getPermissions(), bundle);
                Session.this.authorizationBundle = bundle;

                // TODO: should not use currentActivity, since this might be
                // unloaded now.
                Session.this.finishAuth(currentActivity, newToken, null);
            }

            public void onError(DialogError error) {
                Bundle bundle = new Bundle();
                bundle.putInt(WEB_VIEW_ERROR_CODE_KEY, error.getErrorCode());
                bundle.putString(WEB_VIEW_FAILING_URL_KEY, error.getFailingUrl());
                Session.this.authorizationBundle = bundle;

                Exception exception = new FacebookAuthorizationException(error.getMessage());
                Session.this.finishAuth(currentActivity, null, exception);
            }

            public void onFacebookError(FacebookError error) {
                Exception exception = new FacebookAuthorizationException(error.getMessage());

                // TODO: should not use currentActivity, since this might be
                // unloaded now.
                Session.this.finishAuth(currentActivity, null, exception);
            }

            public void onCancel() {
                Exception exception = new FacebookOperationCanceledException("TODO");

                // TODO: should not use currentActivity, since this might be
                // unloaded now.
                Session.this.finishAuth(currentActivity, null, exception);
            }
        };

        parameters.putString(ServerProtocol.DIALOG_PARAM_DISPLAY, "touch");
        parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, "fbconnect://success");
        parameters.putString(ServerProtocol.DIALOG_PARAM_TYPE, "user_agent");
        parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, this.applicationId);

        Uri uri = Utility.buildUri(ServerProtocol.DIALOG_AUTHORITY, ServerProtocol.DIALOG_OAUTH_PATH, parameters);
        new FbDialog(currentActivity, uri.toString(), listener).show();

        return true;
    }

    private boolean tryKatanaProxyAuth(Activity currentActivity, AuthRequest request) {
        Intent intent = new Intent();

        intent.setClassName(NativeProtocol.KATANA_PACKAGE, NativeProtocol.KATANA_PROXY_AUTH_ACTIVITY);
        intent.putExtra("client_id", this.applicationId);

        if (!Utility.isNullOrEmpty(request.permissions)) {
            intent.putExtra("scope", TextUtils.join(",", request.permissions));
        }

        ResolveInfo resolveInfo = currentActivity.getPackageManager().resolveActivity(intent, 0);
        if ((resolveInfo == null) || !validateFacebookAppSignature(resolveInfo.activityInfo.packageName)) {
            return false;
        }

        try {
            currentActivity.startActivityForResult(intent, request.activityCode);
        } catch (ActivityNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean validateFacebookAppSignature(String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = applicationContext.getPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
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

    void finishAuth(Activity currentActivity, AccessToken newToken, Exception exception) {
        // If the token we came up with is expired/invalid, then auth failed.
        if ((newToken != null) && newToken.isInvalid()) {
            newToken = null;
            exception = new FacebookException("TODO");
        }

        // Update the cache if we have a new token.
        if ((newToken != null) && (this.tokenCache != null)) {
            this.tokenCache.save(newToken.toCacheBundle());
        }

        synchronized (this.lock) {
            final SessionState oldState = this.state;

            switch (this.state) {
            case OPENING:
                if (newToken != null) {
                    this.tokenInfo = newToken;
                    this.state = SessionState.OPENED;
                } else if (exception != null) {
                    this.state = SessionState.CLOSED_LOGIN_FAILED;
                }
                postStateChange(oldState, this.state, exception);
                break;
            }
        }
    }

    void postStateChange(final SessionState oldState, final SessionState newState, final Exception error) {
        final SessionStatusCallback callback = this.callback;
        final Session session = this;

        if (callback != null) {
            Runnable closure = new Runnable() {
                public void run() {
                    // TODO: Do we want to fail if this runs synchronously?
                    // This can be called inside a synchronized block.
                    callback.call(session, newState, error);
                }
            };

            runWithHandlerOrExecutor(handler, closure);
        }

        if (this == Session.activeSession) {
            if (oldState.getIsOpened() != newState.getIsOpened()) {
                if (newState.getIsOpened()) {
                    postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_OPENED);
                } else {
                    postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_CLOSED);
                }
            }
        }
    }

    static void postActiveSessionAction(String action) {
        final Intent intent = new Intent(action);

        synchronized (staticLock) {
            for (ActiveSessionRegistration registration : activeSessionCallbacks) {
                if (registration.getFilter().matchAction(action)) {
                    final BroadcastReceiver receiver = registration.getReceiver();

                    final Runnable closure = new Runnable() {
                        public void run() {
                            receiver.onReceive(applicationContext, intent);
                        }
                    };

                    runWithHandlerOrExecutor(registration.getHandler(), closure);
                }
            }
        }
    }

    private static void runWithHandlerOrExecutor(Handler handler, Runnable runnable) {
        if (handler != null) {
            handler.post(runnable);
        } else {
            SdkRuntime.getExecutor().execute(runnable);
        }
    }

    public static final Session getActiveSession() {
        synchronized (Session.staticLock) {
            return Session.activeSession;
        }
    }

    public static final void setActiveSession(Session session) {
        synchronized (Session.staticLock) {
            if (session != Session.activeSession) {
                Session oldSession = Session.activeSession;

                if (oldSession != null) {
                    oldSession.close();
                }

                Session.activeSession = session;

                if (oldSession != null) {
                    postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_UNSET);
                }

                if (session != null) {
                    postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_SET);

                    if (session.getIsOpened()) {
                        postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_OPENED);
                    }
                }
            }
        }
    }

    public static Session sessionOpen(Activity currentActivity, String applicationId) {
        return sessionOpen(currentActivity, applicationId, null, null);
    }

    public static Session sessionOpen(Activity currentActivity, String applicationId, List<String> permissions,
            SessionStatusCallback callback) {
        Session newSession = new Session(currentActivity, applicationId, permissions, null);

        setActiveSession(newSession);
        newSession.open(currentActivity, callback);
        return newSession;
    }
    
    public static Session sessionOpen(Activity currentActivity, String applicationId, List<String> permissions,
            SessionStatusCallback callback, SessionLoginBehavior behavior, int activityCode) {
        Session newSession = new Session(currentActivity, applicationId, permissions, null);

        setActiveSession(newSession);
        newSession.open(currentActivity, callback, behavior, activityCode);
        return newSession;
    }

    public static void registerActiveSessionReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        ActiveSessionRegistration registration = new ActiveSessionRegistration(receiver, filter);
        activeSessionCallbacks.add(registration);
    }

    public static void unregisterActiveSessionReceiver(BroadcastReceiver receiver) {
        synchronized (staticLock) {
            for (int i = activeSessionCallbacks.size() - 1; i >= 0; i--) {
                if (activeSessionCallbacks.get(i).getReceiver() == receiver) {
                    activeSessionCallbacks.remove(i);
                }
            }
        }
    }

    private static final class ActiveSessionRegistration {
        private final BroadcastReceiver receiver;
        private final IntentFilter filter;
        private final Handler handler;

        public ActiveSessionRegistration(BroadcastReceiver receiver, IntentFilter filter) {
            this.receiver = receiver;
            this.filter = filter;
            this.handler = (Looper.myLooper() != null) ? new Handler() : null;
        }

        public BroadcastReceiver getReceiver() {
            return receiver;
        }

        public IntentFilter getFilter() {
            return filter;
        }

        public Handler getHandler() {
            return handler;
        }
    }

    static final class AuthRequest {
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
            return new AuthRequest(newBehaviorFlags, this.activityCode, this.permissions);
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
                return ALLOW_KATANA_FLAG | ALLOW_WEBVIEW_FLAG;
            }
        }
    }
}
