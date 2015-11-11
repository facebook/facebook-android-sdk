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

package com.facebook.login;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.FacebookException;
import com.facebook.HttpMethod;
import com.facebook.R;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class LoginClient implements Parcelable {
    LoginMethodHandler [] handlersToTry;
    int currentHandler = -1;
    Fragment fragment;
    OnCompletedListener onCompletedListener;
    BackgroundProcessingListener backgroundProcessingListener;
    boolean checkedInternetPermission;
    Request pendingRequest;
    Map<String, String> loggingExtras;
    private LoginLogger loginLogger;

    public interface OnCompletedListener {
        void onCompleted(Result result);
    }

    interface BackgroundProcessingListener {
        void onBackgroundProcessingStarted();

        void onBackgroundProcessingStopped();
    }

    public LoginClient(Fragment fragment) {
        this.fragment = fragment;
    }

    public Fragment getFragment() {
        return fragment;
    }

    void setFragment(Fragment fragment) {
        if (this.fragment != null) {
            throw new FacebookException("Can't set fragment once it is already set.");
        }
        this.fragment = fragment;
    }

    FragmentActivity getActivity() {
        return fragment.getActivity();
    }


    public Request getPendingRequest() {
        return pendingRequest;
    }

    public static int getLoginRequestCode() {
        return CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();
    }

    void startOrContinueAuth(Request request) {
        if (!getInProgress()) {
            authorize(request);
        }
    }

    void authorize(Request request) {
        if (request == null) {
            return;
        }

        if (pendingRequest != null) {
            throw new FacebookException("Attempted to authorize while a request is pending.");
        }

        if (AccessToken.getCurrentAccessToken() != null && !checkInternetPermission()) {
            // We're going to need INTERNET permission later and don't have it, so fail early.
            return;
        }
        pendingRequest = request;
        handlersToTry = getHandlersToTry(request);
        tryNextHandler();
    }

    boolean getInProgress() {
        return pendingRequest != null && currentHandler >= 0;
    }

    void cancelCurrentHandler() {
        if (currentHandler >= 0) {
            getCurrentHandler().cancel();
        }
    }

    private LoginMethodHandler getCurrentHandler() {
        if (currentHandler >= 0) {
            return handlersToTry[currentHandler];
        } else {
            return null;
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (pendingRequest != null) {
            return getCurrentHandler()
                    .onActivityResult(requestCode, resultCode, data);
        }
        return false;
    }

    private LoginMethodHandler [] getHandlersToTry(Request request) {
        ArrayList<LoginMethodHandler> handlers = new ArrayList<LoginMethodHandler>();

        final LoginBehavior behavior = request.getLoginBehavior();

        if (behavior.allowsKatanaAuth()) {
            handlers.add(new GetTokenLoginMethodHandler(this));
            handlers.add(new KatanaProxyLoginMethodHandler(this));
        }

        if (behavior.allowsWebViewAuth()) {
            handlers.add(new WebViewLoginMethodHandler(this));
        }

        LoginMethodHandler [] result = new LoginMethodHandler[handlers.size()];
        handlers.toArray(result);
        return result;
    }

    boolean checkInternetPermission() {
        if (checkedInternetPermission) {
            return true;
        }

        int permissionCheck = checkPermission(Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Activity activity = getActivity();
            String errorType = activity.getString(R.string.com_facebook_internet_permission_error_title);
            String errorDescription = activity.getString(R.string.com_facebook_internet_permission_error_message);
            complete(Result.createErrorResult(pendingRequest, errorType, errorDescription));

            return false;
        }

        checkedInternetPermission = true;
        return true;
    }

    void tryNextHandler() {
        if (currentHandler >= 0) {
            logAuthorizationMethodComplete(
                    getCurrentHandler().getNameForLogging(),
                    LoginLogger.EVENT_PARAM_METHOD_RESULT_SKIPPED,
                    null,
                    null,
                    getCurrentHandler().methodLoggingExtras);
        }

        while (handlersToTry != null && currentHandler < (handlersToTry.length - 1)) {
            currentHandler++;

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
        complete(Result.createErrorResult(pendingRequest, "Login attempt failed.", null));
    }

    private void addLoggingExtra(String key, String value, boolean accumulate) {
        if (loggingExtras == null) {
            loggingExtras = new HashMap<String, String>();
        }
        if (loggingExtras.containsKey(key) && accumulate) {
            value = loggingExtras.get(key) + "," + value;
        }
        loggingExtras.put(key, value);
    }

    boolean tryCurrentHandler() {
        LoginMethodHandler handler = getCurrentHandler();
        if (handler.needsInternetPermission() && !checkInternetPermission()) {
            addLoggingExtra(
                LoginLogger.EVENT_EXTRAS_MISSING_INTERNET_PERMISSION,
                AppEventsConstants.EVENT_PARAM_VALUE_YES,
                false
            );
            return false;
        }

        boolean tried = handler.tryAuthorize(pendingRequest);
        if (tried) {
            getLogger().logAuthorizationMethodStart(pendingRequest.getAuthId(),
                    handler.getNameForLogging());
        } else {
            // We didn't try it, so we don't get any other completion
            // notification -- log that we skipped it.
            addLoggingExtra(
                LoginLogger.EVENT_EXTRAS_NOT_TRIED,
                    handler.getNameForLogging(),
                true
            );
        }

        return tried;
    }

    void completeAndValidate(Result outcome) {
        // Do we need to validate a successful result (as in the case of a reauth)?
        if (outcome.token != null && AccessToken.getCurrentAccessToken() != null) {
            validateSameFbidAndFinish(outcome);
        } else {
            // We're done, just notify the listener.
            complete(outcome);
        }
    }

    void complete(Result outcome) {
        LoginMethodHandler handler = getCurrentHandler();

        // This might be null if, for some reason, none of the handlers were successfully tried
        // (in which case we already logged that).
        if (handler != null) {
            logAuthorizationMethodComplete(handler.getNameForLogging(), outcome,
                    handler.methodLoggingExtras);
        }

        if (loggingExtras != null) {
            // Pass this back to the caller for logging at the aggregate level.
            outcome.loggingExtras = loggingExtras;
        }

        handlersToTry = null;
        currentHandler = -1;
        pendingRequest = null;
        loggingExtras = null;

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


    void setBackgroundProcessingListener(
            BackgroundProcessingListener backgroundProcessingListener) {
        this.backgroundProcessingListener = backgroundProcessingListener;
    }

    int checkPermission(String permission) {
        return getActivity().checkCallingOrSelfPermission(permission);
    }

    void validateSameFbidAndFinish(Result pendingResult) {
        if (pendingResult.token == null) {
            throw new FacebookException("Can't validate without a token");
        }

        AccessToken previousToken = AccessToken.getCurrentAccessToken();
        AccessToken newToken = pendingResult.token;

        try {
            Result result = null;
            if (previousToken != null && newToken != null &&
                    previousToken.getUserId().equals(newToken.getUserId())) {
                result = Result.createTokenResult(pendingRequest, pendingResult.token);
            } else {
                result = Result
                        .createErrorResult(
                                pendingRequest,
                                "User logged in as different Facebook user.",
                                null);
            }
            complete(result);
        } catch (Exception ex) {
            complete(Result.createErrorResult(
                    pendingRequest,
                    "Caught exception",
                    ex.getMessage()));
        }
    }

    private static AccessToken createFromTokenWithRefreshedPermissions(
            AccessToken token,
            Collection<String> grantedPermissions,
            Collection<String> declinedPermissions) {
        return new AccessToken(
                token.getToken(),
                token.getApplicationId(),
                token.getUserId(),
                grantedPermissions,
                declinedPermissions,
                token.getSource(),
                token.getExpires(),
                token.getLastRefresh());
    }

    private LoginLogger getLogger() {
        if (loginLogger == null ||
            !loginLogger.getApplicationId().equals(pendingRequest.getApplicationId())) {

            loginLogger = new LoginLogger(getActivity(), pendingRequest.getApplicationId());
        }
        return loginLogger;
    }

    private void notifyOnCompleteListener(Result outcome) {
        if (onCompletedListener != null) {
            onCompletedListener.onCompleted(outcome);
        }
    }

    void notifyBackgroundProcessingStart() {
        if (backgroundProcessingListener != null) {
            backgroundProcessingListener.onBackgroundProcessingStarted();
        }
    }

    void notifyBackgroundProcessingStop() {
        if (backgroundProcessingListener != null) {
            backgroundProcessingListener.onBackgroundProcessingStopped();
        }
    }

    private void logAuthorizationMethodComplete(
            String method,
            Result result,
            Map<String, String> loggingExtras) {
        logAuthorizationMethodComplete(method,
                result.code.getLoggingValue(),
                result.errorMessage,
                result.errorCode,
                loggingExtras);
    }

    private void logAuthorizationMethodComplete(
            String method,
            String result,
            String errorMessage,
            String errorCode,
            Map<String, String> loggingExtras) {
        if (pendingRequest == null) {
            // We don't expect this to happen, but if it does, log an event for diagnostic purposes.
            getLogger().logUnexpectedError(
                    LoginLogger.EVENT_NAME_LOGIN_METHOD_COMPLETE,
                    "Unexpected call to logCompleteLogin with null pendingAuthorizationRequest.",
                    method
            );
        } else {
            getLogger().logAuthorizationMethodComplete(pendingRequest.getAuthId(), method, result,
                    errorMessage, errorCode, loggingExtras);
        }
    }

    static String getE2E() {
        JSONObject e2e = new JSONObject();
        try {
            e2e.put("init", System.currentTimeMillis());
        } catch (JSONException e) {
        }
        return e2e.toString();
    }

    public static class Request implements Parcelable {
        private final LoginBehavior loginBehavior;
        private Set<String> permissions;
        private final DefaultAudience defaultAudience;
        private final String applicationId;
        private final String authId;
        private boolean isRerequest = false;

        Request(
                LoginBehavior loginBehavior,
                Set<String> permissions,
                DefaultAudience defaultAudience,
                String applicationId,
                String authId) {
            this.loginBehavior = loginBehavior;
            this.permissions = permissions != null ? permissions : new HashSet<String>();
            this.defaultAudience = defaultAudience;
            this.applicationId = applicationId;
            this.authId = authId;
        }

        Set<String> getPermissions() {
            return permissions;
        }

        void setPermissions(Set<String> permissions) {
            Validate.notNull(permissions, "permissions");
            this.permissions = permissions;
        }

        LoginBehavior getLoginBehavior() {
            return loginBehavior;
        }

        DefaultAudience getDefaultAudience() {
            return defaultAudience;
        }

        String getApplicationId() {
            return applicationId;
        }

        String getAuthId() {
            return authId;
        }

        boolean isRerequest() {
            return isRerequest;
        }

        void setRerequest(boolean isRerequest) {
            this.isRerequest = isRerequest;
        }

        boolean hasPublishPermission() {
            for (String permission : permissions) {
                if (LoginManager.isPublishPermission(permission)) {
                    return true;
                }
            }
            return false;
        }

        private Request(Parcel parcel) {
            String enumValue = parcel.readString();
            this.loginBehavior = enumValue != null ? LoginBehavior.valueOf(enumValue) : null;
            ArrayList<String> permissionsList = new ArrayList<>();
            parcel.readStringList(permissionsList);
            this.permissions = new HashSet<String>(permissionsList);
            enumValue = parcel.readString();
            this.defaultAudience = enumValue != null ? DefaultAudience.valueOf(enumValue) : null;
            this.applicationId = parcel.readString();
            this.authId = parcel.readString();
            this.isRerequest = parcel.readByte() != 0 ? true : false;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(loginBehavior != null ? loginBehavior.name() : null);
            dest.writeStringList(new ArrayList<String>(permissions));
            dest.writeString(defaultAudience != null ? defaultAudience.name() : null);
            dest.writeString(applicationId);
            dest.writeString(authId);
            dest.writeByte((byte)(isRerequest ? 1 : 0));
        }

        public static final Parcelable.Creator<Request> CREATOR = new Parcelable.Creator() {
            @Override
            public Request createFromParcel(Parcel source) {
                return new Request(source);
            }

            @Override
            public Request[] newArray(int size) {
                return new Request[size];
            }
        };
    }

    public static class Result implements Parcelable {
        enum Code {
            SUCCESS("success"),
            CANCEL("cancel"),
            ERROR("error");

            private final String loggingValue;

            Code(String loggingValue) {
                this.loggingValue = loggingValue;
            }

            // For consistency across platforms, we want to use specific string values when logging
            // these results.
            String getLoggingValue() {
                return loggingValue;
            }
        }

        final Code code;
        final AccessToken token;
        final String errorMessage;
        final String errorCode;
        final Request request;
        public Map<String, String> loggingExtras;

        Result(
                Request request,
                Code code,
                AccessToken token,
                String errorMessage,
                String errorCode) {
            Validate.notNull(code, "code");
            this.request = request;
            this.token = token;
            this.errorMessage = errorMessage;
            this.code = code;
            this.errorCode = errorCode;
        }

        static Result createTokenResult(Request request, AccessToken token) {
            return new Result(request, Code.SUCCESS, token, null, null);
        }

        static Result createCancelResult(Request request, String message) {
            return new Result(request, Code.CANCEL, null, message, null);
        }

        static Result createErrorResult(
                Request request,
                String errorType,
                String errorDescription) {
            return createErrorResult(request, errorType, errorDescription, null);
        }

        static Result createErrorResult(
                Request request,
                String errorType,
                String errorDescription,
                String errorCode) {
            String message = TextUtils.join(
                    ": ",
                    Utility.asListNoNulls(errorType, errorDescription));
            return new Result(request, Code.ERROR, null, message, errorCode);
        }

        private Result(Parcel parcel) {
            this.code = Code.valueOf(parcel.readString());
            this.token = parcel.readParcelable(AccessToken.class.getClassLoader());
            this.errorMessage = parcel.readString();
            this.errorCode = parcel.readString();
            this.request = parcel.readParcelable(Request.class.getClassLoader());
            this.loggingExtras = Utility.readStringMapFromParcel(parcel);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(code.name());
            dest.writeParcelable(token, flags);
            dest.writeString(errorMessage);
            dest.writeString(errorCode);
            dest.writeParcelable(request, flags);
            Utility.writeStringMapToParcel(dest, loggingExtras);
        }

        public static final Parcelable.Creator<Result> CREATOR = new Parcelable.Creator() {
            @Override
            public Result createFromParcel(Parcel source) {
                return new Result(source);
            }

            @Override
            public Result[] newArray(int size) {
                return new Result[size];
            }
        };
    }

    /**
     * Internal helper class that is used to hold two different permission lists (granted and
     * declined)
     */
    private static class PermissionsPair {
        List<String> grantedPermissions;
        List<String> declinedPermissions;

        public PermissionsPair(List<String> grantedPermissions, List<String> declinedPermissions) {
            this.grantedPermissions = grantedPermissions;
            this.declinedPermissions = declinedPermissions;
        }

        public List<String> getGrantedPermissions() {
            return grantedPermissions;
        }

        public List<String> getDeclinedPermissions() {
            return declinedPermissions;
        }
    }

    /**
     * This parses a server response to a call to me/permissions.  It will return the list of
     * granted permissions. It will optionally update an access token with the requested permissions.
     *
     * @param response The server response
     * @return A list of granted permissions or null if an error
     */
    private static PermissionsPair handlePermissionResponse(GraphResponse response) {
        if (response.getError() != null) {
            return null;
        }

        JSONObject result = response.getJSONObject();
        if (result == null) {
            return null;
        }

        JSONArray data = result.optJSONArray("data");
        if (data == null || data.length() == 0) {
            return null;
        }
        List<String> grantedPermissions = new ArrayList<String>(data.length());
        List<String> declinedPermissions = new ArrayList<String>(data.length());

        for (int i = 0; i < data.length(); ++i) {
            JSONObject object = data.optJSONObject(i);
            String permission = object.optString("permission");
            if (permission == null || permission.equals("installed")) {
                continue;
            }
            String status = object.optString("status");
            if (status == null) {
                continue;
            }
            if(status.equals("granted")) {
                grantedPermissions.add(permission);
            } else if (status.equals("declined")) {
                declinedPermissions.add(permission);
            }
        }

        return new PermissionsPair(grantedPermissions, declinedPermissions);
    }

    // Parcelable implementation

    public LoginClient(Parcel source) {
        Object [] o = source.readParcelableArray(LoginMethodHandler.class.getClassLoader());
        handlersToTry = new LoginMethodHandler[o.length];
        for (int i = 0; i < o.length; ++i) {
            handlersToTry[i] = (LoginMethodHandler) o[i];
            handlersToTry[i].setLoginClient(this);
        }
        currentHandler = source.readInt();
        pendingRequest = source.readParcelable(Request.class.getClassLoader());
        loggingExtras = Utility.readStringMapFromParcel(source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelableArray(handlersToTry, flags);
        dest.writeInt(currentHandler);
        dest.writeParcelable(pendingRequest, flags);
        Utility.writeStringMapToParcel(dest, loggingExtras);
    }

    public static final Parcelable.Creator<LoginClient> CREATOR = new Parcelable.Creator() {
        @Override
        public LoginClient createFromParcel(Parcel source) {
            return new LoginClient(source);
        }

        @Override
        public LoginClient[] newArray(int size) {
            return new LoginClient[size];
        }
    };
}
