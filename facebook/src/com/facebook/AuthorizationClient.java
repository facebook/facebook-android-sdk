/**
 * Copyright 2010-present Facebook.
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

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieSyncManager;
import com.facebook.android.R;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.model.GraphMultiResult;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class AuthorizationClient implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "Facebook-AuthorizationClient";
    private static final String WEB_VIEW_AUTH_HANDLER_STORE =
            "com.facebook.AuthorizationClient.WebViewAuthHandler.TOKEN_STORE_KEY";
    private static final String WEB_VIEW_AUTH_HANDLER_TOKEN_KEY = "TOKEN";

    List<AuthHandler> handlersToTry;
    AuthHandler currentHandler;
    transient Context context;
    transient StartActivityDelegate startActivityDelegate;
    transient OnCompletedListener onCompletedListener;
    transient BackgroundProcessingListener backgroundProcessingListener;
    transient boolean checkedInternetPermission;
    AuthorizationRequest pendingRequest;

    interface OnCompletedListener {
        void onCompleted(Result result);
    }

    interface BackgroundProcessingListener {
        void onBackgroundProcessingStarted();

        void onBackgroundProcessingStopped();
    }

    interface StartActivityDelegate {
        public void startActivityForResult(Intent intent, int requestCode);

        public Activity getActivityContext();
    }

    void setContext(final Context context) {
        this.context = context;
        // We rely on individual requests to tell us how to start an activity.
        startActivityDelegate = null;
    }

    void setContext(final Activity activity) {
        this.context = activity;

        // If we are used in the context of an activity, we will always use that activity to
        // call startActivityForResult.
        startActivityDelegate = new StartActivityDelegate() {
            @Override
            public void startActivityForResult(Intent intent, int requestCode) {
                activity.startActivityForResult(intent, requestCode);
            }

            @Override
            public Activity getActivityContext() {
                return activity;
            }
        };
    }

    void startOrContinueAuth(AuthorizationRequest request) {
        if (getInProgress()) {
            continueAuth();
        } else {
            authorize(request);
        }
    }

    void authorize(AuthorizationRequest request) {
        if (request == null) {
            return;
        }

        if (pendingRequest != null) {
            throw new FacebookException("Attempted to authorize while a request is pending.");
        }

        if (request.needsNewTokenValidation() && !checkInternetPermission()) {
            // We're going to need INTERNET permission later and don't have it, so fail early.
            return;
        }
        pendingRequest = request;
        handlersToTry = getHandlerTypes(request);
        tryNextHandler();
    }

    void continueAuth() {
        if (pendingRequest == null || currentHandler == null) {
            throw new FacebookException("Attempted to continue authorization without a pending request.");
        }

        if (currentHandler.needsRestart()) {
            currentHandler.cancel();
            tryCurrentHandler();
        }
    }

    boolean getInProgress() {
        return pendingRequest != null && currentHandler != null;
    }

    void cancelCurrentHandler() {
        if (currentHandler != null) {
            currentHandler.cancel();
        }
    }

    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == pendingRequest.getRequestCode()) {
            return currentHandler.onActivityResult(requestCode, resultCode, data);
        }
        return false;
    }

    private List<AuthHandler> getHandlerTypes(AuthorizationRequest request) {
        ArrayList<AuthHandler> handlers = new ArrayList<AuthHandler>();

        final SessionLoginBehavior behavior = request.getLoginBehavior();
        if (behavior.allowsKatanaAuth()) {
            if (!request.isLegacy()) {
                handlers.add(new GetTokenAuthHandler());
                handlers.add(new KatanaLoginDialogAuthHandler());
            }
            handlers.add(new KatanaProxyAuthHandler());
        }

        if (behavior.allowsWebViewAuth()) {
            handlers.add(new WebViewAuthHandler());
        }

        return handlers;
    }

    boolean checkInternetPermission() {
        if (checkedInternetPermission) {
            return true;
        }

        int permissionCheck = checkPermission(Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            String errorType = context.getString(R.string.com_facebook_internet_permission_error_title);
            String errorDescription = context.getString(R.string.com_facebook_internet_permission_error_message);
            complete(Result.createErrorResult(errorType, errorDescription));

            return false;
        }

        checkedInternetPermission = true;
        return true;
    }

    void tryNextHandler() {
        while (handlersToTry != null && !handlersToTry.isEmpty()) {
            currentHandler = handlersToTry.remove(0);

            boolean started = tryCurrentHandler();

            if (started) {
                return;
            }
        }

        if (pendingRequest != null) {
            // We went through all handlers without successfully attempting an auth.
            completeWithFailure();
        }
    }

    private void completeWithFailure() {
        complete(Result.createErrorResult("Login attempt failed.", null));
    }

    boolean tryCurrentHandler() {
        if (currentHandler.needsInternetPermission() && !checkInternetPermission()) {
            return false;
        }
        return currentHandler.tryAuthorize(pendingRequest);
    }

    void completeAndValidate(Result outcome) {
        // Do we need to validate a successful result (as in the case of a reauth)?
        if (outcome.token != null && pendingRequest.needsNewTokenValidation()) {
            validateSameFbidAndFinish(outcome);
        } else {
            // We're done, just notify the listener.
            complete(outcome);
        }
    }

    void complete(Result outcome) {
        handlersToTry = null;
        currentHandler = null;
        pendingRequest = null;

        notifyOnCompleteListener(outcome);
    }

    OnCompletedListener getOnCompletedListener() {
        return onCompletedListener;
    }

    void setOnCompletedListener(OnCompletedListener onCompletedListener) {
        this.onCompletedListener = onCompletedListener;
    }

    BackgroundProcessingListener getBackgroundProcessingListener() {
        return backgroundProcessingListener;
    }

    void setBackgroundProcessingListener(BackgroundProcessingListener backgroundProcessingListener) {
        this.backgroundProcessingListener = backgroundProcessingListener;
    }

    StartActivityDelegate getStartActivityDelegate() {
        if (startActivityDelegate != null) {
            return startActivityDelegate;
        } else if (pendingRequest != null) {
            // Wrap the request's delegate in our own.
            return new StartActivityDelegate() {
                @Override
                public void startActivityForResult(Intent intent, int requestCode) {
                    pendingRequest.getStartActivityDelegate().startActivityForResult(intent, requestCode);
                }

                @Override
                public Activity getActivityContext() {
                    return pendingRequest.getStartActivityDelegate().getActivityContext();
                }
            };
        }
        return null;
    }

    int checkPermission(String permission) {
        return context.checkCallingOrSelfPermission(permission);
    }

    void validateSameFbidAndFinish(Result pendingResult) {
        if (pendingResult.token == null) {
            throw new FacebookException("Can't validate without a token");
        }

        RequestBatch batch = createReauthValidationBatch(pendingResult);

        notifyBackgroundProcessingStart();

        batch.executeAsync();
    }

    RequestBatch createReauthValidationBatch(final Result pendingResult) {
        // We need to ensure that the token we got represents the same fbid as the old one. We issue
        // a "me" request using the current token, a "me" request using the new token, and a "me/permissions"
        // request using the current token to get the permissions of the user.

        final ArrayList<String> fbids = new ArrayList<String>();
        final ArrayList<String> tokenPermissions = new ArrayList<String>();
        final String newToken = pendingResult.token.getToken();

        Request.Callback meCallback = new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                try {
                    GraphUser user = response.getGraphObjectAs(GraphUser.class);
                    if (user != null) {
                        fbids.add(user.getId());
                    }
                } catch (Exception ex) {
                }
            }
        };

        String validateSameFbidAsToken = pendingRequest.getPreviousAccessToken();
        Request requestCurrentTokenMe = createGetProfileIdRequest(validateSameFbidAsToken);
        requestCurrentTokenMe.setCallback(meCallback);

        Request requestNewTokenMe = createGetProfileIdRequest(newToken);
        requestNewTokenMe.setCallback(meCallback);

        Request requestCurrentTokenPermissions = createGetPermissionsRequest(validateSameFbidAsToken);
        requestCurrentTokenPermissions.setCallback(new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                try {
                    GraphMultiResult result = response.getGraphObjectAs(GraphMultiResult.class);
                    if (result != null) {
                        GraphObjectList<GraphObject> data = result.getData();
                        if (data != null && data.size() == 1) {
                            GraphObject permissions = data.get(0);

                            // The keys are the permission names.
                            tokenPermissions.addAll(permissions.asMap().keySet());
                        }
                    }
                } catch (Exception ex) {
                }
            }
        });

        RequestBatch batch = new RequestBatch(requestCurrentTokenMe, requestNewTokenMe,
                requestCurrentTokenPermissions);
        batch.setBatchApplicationId(pendingRequest.getApplicationId());
        batch.addCallback(new RequestBatch.Callback() {
            @Override
            public void onBatchCompleted(RequestBatch batch) {
                try {
                    Result result = null;
                    if (fbids.size() == 2 && fbids.get(0) != null && fbids.get(1) != null &&
                            fbids.get(0).equals(fbids.get(1))) {
                        // Modify the token to have the right permission set.
                        AccessToken tokenWithPermissions = AccessToken
                                .createFromTokenWithRefreshedPermissions(pendingResult.token,
                                        tokenPermissions);
                        result = Result.createTokenResult(tokenWithPermissions);
                    } else {
                        result = Result
                                .createErrorResult("User logged in as different Facebook user.", null);
                    }
                    complete(result);
                } catch (Exception ex) {
                    complete(Result.createErrorResult("Caught exception", ex.getMessage()));
                } finally {
                    notifyBackgroundProcessingStop();
                }
            }
        });

        return batch;
    }

    Request createGetPermissionsRequest(String accessToken) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id");
        parameters.putString("access_token", accessToken);
        return new Request(null, "me/permissions", parameters, HttpMethod.GET, null);
    }

    Request createGetProfileIdRequest(String accessToken) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id");
        parameters.putString("access_token", accessToken);
        return new Request(null, "me", parameters, HttpMethod.GET, null);
    }

    private void notifyOnCompleteListener(Result outcome) {
        if (onCompletedListener != null) {
            onCompletedListener.onCompleted(outcome);
        }
    }

    private void notifyBackgroundProcessingStart() {
        if (backgroundProcessingListener != null) {
            backgroundProcessingListener.onBackgroundProcessingStarted();
        }
    }

    private void notifyBackgroundProcessingStop() {
        if (backgroundProcessingListener != null) {
            backgroundProcessingListener.onBackgroundProcessingStopped();
        }
    }

    abstract class AuthHandler implements Serializable {
        private static final long serialVersionUID = 1L;

        abstract boolean tryAuthorize(AuthorizationRequest request);

        boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            return false;
        }

        boolean needsRestart() {
            return false;
        }

        boolean needsInternetPermission() {
            return false;
        }

        void cancel() {
        }
    }

    class WebViewAuthHandler extends AuthHandler {
        private static final long serialVersionUID = 1L;
        private transient WebDialog loginDialog;

        @Override
        boolean needsRestart() {
            // Because we are presenting WebView UI within the current context, we need to explicitly
            // restart the process if the context goes away and is recreated.
            return true;
        }

        @Override
        boolean needsInternetPermission() {
            return true;
        }

        @Override
        void cancel() {
            if (loginDialog != null) {
                loginDialog.dismiss();
                loginDialog = null;
            }
        }

        @Override
        boolean tryAuthorize(final AuthorizationRequest request) {
            String applicationId = request.getApplicationId();
            Bundle parameters = new Bundle();
            if (!Utility.isNullOrEmpty(request.getPermissions())) {
                parameters.putString(ServerProtocol.DIALOG_PARAM_SCOPE, TextUtils.join(",", request.getPermissions()));
            }

            String previousToken = request.getPreviousAccessToken();
            if (!Utility.isNullOrEmpty(previousToken) && (previousToken.equals(loadCookieToken()))) {
                parameters.putString(ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, previousToken);
            } else {
                // The call to clear cookies will create the first instance of CookieSyncManager if necessary
                Utility.clearFacebookCookies(context);
            }

            WebDialog.OnCompleteListener listener = new WebDialog.OnCompleteListener() {
                @Override
                public void onComplete(Bundle values, FacebookException error) {
                    onWebDialogComplete(request, values, error);
                }
            };

            WebDialog.Builder builder =
                    new AuthDialogBuilder(getStartActivityDelegate().getActivityContext(), applicationId, parameters)
                            .setOnCompleteListener(listener);
            loginDialog = builder.build();
            loginDialog.show();

            return true;
        }

        void onWebDialogComplete(AuthorizationRequest request, Bundle values,
                FacebookException error) {
            Result outcome;
            if (values != null) {
                AccessToken token = AccessToken
                        .createFromWebBundle(request.getPermissions(), values, AccessTokenSource.WEB_VIEW);
                outcome = Result.createTokenResult(token);

                // Ensure any cookies set by the dialog are saved
                // This is to work around a bug where CookieManager may fail to instantiate if CookieSyncManager
                // has never been created.
                CookieSyncManager syncManager = CookieSyncManager.createInstance(context);
                syncManager.sync();
                saveCookieToken(token.getToken());
            } else {
                if (error instanceof FacebookOperationCanceledException) {
                    outcome = Result.createCancelResult("User canceled log in.");
                } else {
                    outcome = Result.createErrorResult(error.getMessage(), null);
                }
            }
            completeAndValidate(outcome);
        }

        private void saveCookieToken(String token) {
            Context context = getStartActivityDelegate().getActivityContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    WEB_VIEW_AUTH_HANDLER_STORE,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(WEB_VIEW_AUTH_HANDLER_TOKEN_KEY, token);
            if (!editor.commit()) {
                Utility.logd(TAG, "Could not update saved web view auth handler token.");
            }
        }

        private String loadCookieToken() {
            Context context = getStartActivityDelegate().getActivityContext();
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    WEB_VIEW_AUTH_HANDLER_STORE,
                    Context.MODE_PRIVATE);
            return sharedPreferences.getString(WEB_VIEW_AUTH_HANDLER_TOKEN_KEY, "");
        }
    }

    class GetTokenAuthHandler extends AuthHandler {
        private static final long serialVersionUID = 1L;
        private transient GetTokenClient getTokenClient;

        @Override
        void cancel() {
            if (getTokenClient != null) {
                getTokenClient.cancel();
                getTokenClient = null;
            }
        }

        boolean tryAuthorize(final AuthorizationRequest request) {
            getTokenClient = new GetTokenClient(context, request.getApplicationId());
            if (!getTokenClient.start()) {
                return false;
            }

            notifyBackgroundProcessingStart();

            GetTokenClient.CompletedListener callback = new GetTokenClient.CompletedListener() {
                @Override
                public void completed(Bundle result) {
                    getTokenCompleted(request, result);
                }
            };

            getTokenClient.setCompletedListener(callback);
            return true;
        }

        void getTokenCompleted(AuthorizationRequest request, Bundle result) {
            getTokenClient = null;

            notifyBackgroundProcessingStop();

            if (result != null) {
                ArrayList<String> currentPermissions = result.getStringArrayList(NativeProtocol.EXTRA_PERMISSIONS);
                List<String> permissions = request.getPermissions();
                if ((currentPermissions != null) &&
                        ((permissions == null) || currentPermissions.containsAll(permissions))) {
                    // We got all the permissions we needed, so we can complete the auth now.
                    AccessToken token = AccessToken
                            .createFromNativeLogin(result, AccessTokenSource.FACEBOOK_APPLICATION_SERVICE);
                    Result outcome = Result.createTokenResult(token);
                    completeAndValidate(outcome);
                    return;
                }

                // We didn't get all the permissions we wanted, so update the request with just the permissions
                // we still need.
                ArrayList<String> newPermissions = new ArrayList<String>();
                for (String permission : permissions) {
                    if (!currentPermissions.contains(permission)) {
                        newPermissions.add(permission);
                    }
                }
                request.setPermissions(newPermissions);
            }

            tryNextHandler();
        }
    }

    abstract class KatanaAuthHandler extends AuthHandler {
        private static final long serialVersionUID = 1L;

        protected boolean tryIntent(Intent intent, int requestCode) {
            if (intent == null) {
                return false;
            }

            try {
                getStartActivityDelegate().startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                return false;
            }

            return true;
        }
    }

    class KatanaLoginDialogAuthHandler extends KatanaAuthHandler {
        private static final long serialVersionUID = 1L;

        @Override
        boolean tryAuthorize(AuthorizationRequest request) {
            Intent intent = NativeProtocol.createLoginDialog20121101Intent(context, request.getApplicationId(),
                    new ArrayList<String>(request.getPermissions()),
                    request.getDefaultAudience().getNativeProtocolAudience());

            return tryIntent(intent, request.getRequestCode());
        }

        @Override
        boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            Result outcome;

            if (data == null) {
                // This happens if the user presses 'Back'.
                outcome = Result.createCancelResult("Operation canceled");
            } else if (NativeProtocol.isServiceDisabledResult20121101(data)) {
                outcome = null;
            } else if (resultCode == Activity.RESULT_CANCELED) {
                outcome = Result.createCancelResult(
                        data.getStringExtra(NativeProtocol.STATUS_ERROR_DESCRIPTION));
            } else if (resultCode != Activity.RESULT_OK) {
                outcome = Result.createErrorResult("Unexpected resultCode from authorization.", null);
            } else {
                outcome = handleResultOk(data);
            }

            if (outcome != null) {
                completeAndValidate(outcome);
            } else {
                tryNextHandler();
            }

            return true;
        }

        private Result handleResultOk(Intent data) {
            Bundle extras = data.getExtras();
            String errorType = extras.getString(NativeProtocol.STATUS_ERROR_TYPE);
            if (errorType == null) {
                return Result.createTokenResult(
                        AccessToken.createFromNativeLogin(extras, AccessTokenSource.FACEBOOK_APPLICATION_NATIVE));
            } else if (NativeProtocol.ERROR_SERVICE_DISABLED.equals(errorType)) {
                return null;
            } else if (NativeProtocol.ERROR_USER_CANCELED.equals(errorType)) {
                return Result.createCancelResult(null);
            } else {
                return Result.createErrorResult(errorType, extras.getString("error_description"));
            }
        }
    }

    class KatanaProxyAuthHandler extends KatanaAuthHandler {
        private static final long serialVersionUID = 1L;

        @Override
        boolean tryAuthorize(AuthorizationRequest request) {
            Intent intent = NativeProtocol.createProxyAuthIntent(context,
                    request.getApplicationId(), request.getPermissions());

            return tryIntent(intent, request.getRequestCode());
        }

        @Override
        boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            // Handle stuff
            Result outcome;

            if (data == null) {
                // This happens if the user presses 'Back'.
                outcome = Result.createCancelResult("Operation canceled");
            } else if (resultCode == Activity.RESULT_CANCELED) {
                outcome = Result.createCancelResult(data.getStringExtra("error"));
            } else if (resultCode != Activity.RESULT_OK) {
                outcome = Result.createErrorResult("Unexpected resultCode from authorization.", null);
            } else {
                outcome = handleResultOk(data);
            }

            if (outcome != null) {
                completeAndValidate(outcome);
            } else {
                tryNextHandler();
            }
            return true;
        }

        private Result handleResultOk(Intent data) {
            Bundle extras = data.getExtras();
            String error = extras.getString("error");
            if (error == null) {
                error = extras.getString("error_type");
            }

            if (error == null) {
                AccessToken token = AccessToken.createFromWebBundle(pendingRequest.getPermissions(), extras,
                        AccessTokenSource.FACEBOOK_APPLICATION_WEB);
                return Result.createTokenResult(token);
            } else if (ServerProtocol.errorsProxyAuthDisabled.contains(error)) {
                return null;
            } else if (ServerProtocol.errorsUserCanceled.contains(error)) {
                return Result.createCancelResult(null);
            } else {
                return Result.createErrorResult(error, extras.getString("error_description"));
            }
        }
    }

    static class AuthDialogBuilder extends WebDialog.Builder {
        private static final String OAUTH_DIALOG = "oauth";
        static final String REDIRECT_URI = "fbconnect://success";

        public AuthDialogBuilder(Context context, String applicationId, Bundle parameters) {
            super(context, applicationId, OAUTH_DIALOG, parameters);
        }

        @Override
        public WebDialog build() {
            Bundle parameters = getParameters();
            parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, REDIRECT_URI);
            parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, getApplicationId());

            return new WebDialog(getContext(), OAUTH_DIALOG, parameters, getTheme(), getListener());
        }
    }

    static class AuthorizationRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private transient final StartActivityDelegate startActivityDelegate;
        private SessionLoginBehavior loginBehavior;
        private int requestCode;
        private boolean isLegacy = false;
        private List<String> permissions;
        private SessionDefaultAudience defaultAudience;
        private String applicationId;
        private String previousAccessToken;

        AuthorizationRequest(SessionLoginBehavior loginBehavior, int requestCode, boolean isLegacy,
                List<String> permissions, SessionDefaultAudience defaultAudience, String applicationId,
                String validateSameFbidAsToken, StartActivityDelegate startActivityDelegate) {
            this.loginBehavior = loginBehavior;
            this.requestCode = requestCode;
            this.isLegacy = isLegacy;
            this.permissions = permissions;
            this.defaultAudience = defaultAudience;
            this.applicationId = applicationId;
            this.previousAccessToken = validateSameFbidAsToken;
            this.startActivityDelegate = startActivityDelegate;

        }

        StartActivityDelegate getStartActivityDelegate() {
            return startActivityDelegate;
        }

        List<String> getPermissions() {
            return permissions;
        }

        void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }

        SessionLoginBehavior getLoginBehavior() {
            return loginBehavior;
        }

        int getRequestCode() {
            return requestCode;
        }

        SessionDefaultAudience getDefaultAudience() {
            return defaultAudience;
        }

        String getApplicationId() {
            return applicationId;
        }

        boolean isLegacy() {
            return isLegacy;
        }

        void setIsLegacy(boolean isLegacy) {
            this.isLegacy = isLegacy;
        }

        String getPreviousAccessToken() {
            return previousAccessToken;
        }

        boolean needsNewTokenValidation() {
            return previousAccessToken != null && !isLegacy;
        }
    }


    static class Result implements Serializable {
        private static final long serialVersionUID = 1L;

        enum Code {
            SUCCESS,
            CANCEL,
            ERROR
        }

        final Code code;
        final AccessToken token;
        final String errorMessage;

        private Result(Code code, AccessToken token, String errorMessage) {
            this.token = token;
            this.errorMessage = errorMessage;
            this.code = code;
        }

        static Result createTokenResult(AccessToken token) {
            return new Result(Code.SUCCESS, token, null);
        }

        static Result createCancelResult(String message) {
            return new Result(Code.CANCEL, null, message);
        }

        static Result createErrorResult(String errorType, String errorDescription) {
            String message = errorType;
            if (errorDescription != null) {
                message += ": " + errorDescription;
            }
            return new Result(Code.ERROR, null, message);
        }
    }
}
