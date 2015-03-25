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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.content.Context;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookActivity;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.Validate;
import com.facebook.appevents.AppEventsConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This class manages login and permissions for Facebook.
 */
public class LoginManager {
    private static final String PUBLISH_PERMISSION_PREFIX = "publish";
    private static final String MANAGE_PERMISSION_PREFIX = "manage";
    private static final Set<String> OTHER_PUBLISH_PERMISSIONS = getOtherPublishPermissions();

    private static volatile LoginManager instance;

    private LoginBehavior loginBehavior = LoginBehavior.SSO_WITH_FALLBACK;
    private DefaultAudience defaultAudience = DefaultAudience.FRIENDS;
    private LoginClient.Request pendingLoginRequest;
    private HashMap<String, String> pendingLoggingExtras;
    private Context context;
    private LoginLogger loginLogger;

    LoginManager() {
        Validate.sdkInitialized();
    }

    /**
     * Getter for the login manager.
     * @return The login manager.
     */
    public static LoginManager getInstance() {
        if (instance == null) {
            synchronized (LoginManager.class) {
                if (instance == null) {
                    instance = new LoginManager();
                }
            }
        }

        return instance;
    }

    /**
     * Starts the login process to resolve the error defined in the response. The registered login
     * callbacks will be called on completion.
     *
     * @param activity The activity which is starting the login process.
     * @param response The response that has the error.
     */
    public void resolveError(final Activity activity, final GraphResponse response) {
        startLogin(
                new ActivityStartActivityDelegate(activity),
                createLoginRequestFromResponse(response)
        );
    }

    /**
     * Starts the login process to resolve the error defined in the response. The registered login
     * callbacks will be called on completion.
     *
     * @param fragment The fragment which is starting the login process.
     * @param response The response that has the error.
     */
    public void resolveError(final Fragment fragment, final GraphResponse response) {
        startLogin(
                new FragmentStartActivityDelegate(fragment),
                createLoginRequestFromResponse(response)
        );
    }

    private LoginClient.Request createLoginRequestFromResponse(final GraphResponse response) {
        Validate.notNull(response, "response");
        AccessToken failedToken = response.getRequest().getAccessToken();
        return createLoginRequest(failedToken != null ? failedToken.getPermissions() : null);
    }

    /**
     * Registers a login callback to the given callback manager.
     * @param callbackManager The callback manager that will encapsulate the callback.
     * @param callback The login callback that will be called on login completion.
     */
    public void registerCallback(
            final CallbackManager callbackManager,
            final FacebookCallback<LoginResult> callback) {
        if (!(callbackManager instanceof CallbackManagerImpl)) {
            throw new FacebookException("Unexpected CallbackManager, " +
                    "please use the provided Factory.");
        }
        ((CallbackManagerImpl) callbackManager).registerCallback(
                CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        return LoginManager.this.onActivityResult(
                                resultCode,
                                data,
                                callback);
                    }
                }
        );
    }

    boolean onActivityResult(int resultCode, Intent data) {
        return onActivityResult(resultCode, data, null);
    }

    boolean onActivityResult(int resultCode, Intent data, FacebookCallback<LoginResult>  callback) {

        if (pendingLoginRequest == null) {
            return false;
        }

        FacebookException exception = null;
        AccessToken newToken = null;
        LoginClient.Result.Code code = LoginClient.Result.Code.ERROR;
        Map<String, String> loggingExtras = null;

        boolean isCanceled = false;
        if (data != null) {
            LoginClient.Result result = (LoginClient.Result)
                    data.getParcelableExtra(LoginFragment.RESULT_KEY);
            if (result != null) {
                code = result.code;
                if (resultCode == Activity.RESULT_OK) {
                    if (result.code == LoginClient.Result.Code.SUCCESS) {
                        newToken = result.token;
                    } else {
                        exception = new FacebookAuthorizationException(result.errorMessage);
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    isCanceled = true;
                }
                loggingExtras = result.loggingExtras;
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            isCanceled = true;
            code = LoginClient.Result.Code.CANCEL;
        }

        if (exception == null && newToken == null && !isCanceled) {
            exception = new FacebookException("Unexpected call to LoginManager.onActivityResult");
        }

        logCompleteLogin(code, loggingExtras, exception);

        finishLogin(newToken, exception, isCanceled, callback);

        return true;
    }

    /**
     * Getter for the login behavior.
     * @return the login behavior.
     */
    public LoginBehavior getLoginBehavior() {
        return loginBehavior;
    }

    /**
     * Setter for the login behavior.
     * @param loginBehavior The login behavior.
     * @return The login manager.
     */
    public LoginManager setLoginBehavior(LoginBehavior loginBehavior) {
        this.loginBehavior = loginBehavior;
        return this;
    }

    /**
     * Getter for the default audience.
     * @return The default audience.
     */
    public DefaultAudience getDefaultAudience() {
        return defaultAudience;
    }

    /**
     * Setter for the default audience.
     * @param defaultAudience The default audience.
     * @return The login manager.
     */
    public LoginManager setDefaultAudience(DefaultAudience defaultAudience) {
        this.defaultAudience = defaultAudience;
        return this;
    }

    /**
     * Logs out the user.
     */
    public void logOut() {
        AccessToken.setCurrentAccessToken(null);
        Profile.setCurrentProfile(null);
    }

    /**
     * Logs the user in with the requested read permissions.
     * @param fragment    The fragment which is starting the login process.
     * @param permissions The requested permissions.
     */
    public void logInWithReadPermissions(Fragment fragment, Collection<String> permissions) {
        validateReadPermissions(permissions);

        LoginClient.Request loginRequest = createLoginRequest(permissions);
        startLogin(new FragmentStartActivityDelegate(fragment), loginRequest);
    }

    /**
     * Logs the user in with the requested read permissions.
     * @param activity    The activity which is starting the login process.
     * @param permissions The requested permissions.
     */
    public void logInWithReadPermissions(Activity activity, Collection<String> permissions) {
        validateReadPermissions(permissions);

        LoginClient.Request loginRequest = createLoginRequest(permissions);
        startLogin(new ActivityStartActivityDelegate(activity), loginRequest);
    }

    /**
     * Logs the user in with the requested publish permissions.
     * @param fragment    The fragment which is starting the login process.
     * @param permissions The requested permissions.
     */
    public void logInWithPublishPermissions(Fragment fragment, Collection<String> permissions) {
        validatePublishPermissions(permissions);

        LoginClient.Request loginRequest = createLoginRequest(permissions);
        startLogin(new FragmentStartActivityDelegate(fragment), loginRequest);
    }

    /**
     * Logs the user in with the requested publish permissions.
     * @param activity    The activity which is starting the login process.
     * @param permissions The requested permissions.
     */
    public void logInWithPublishPermissions(Activity activity, Collection<String> permissions) {
        validatePublishPermissions(permissions);

        LoginClient.Request loginRequest = createLoginRequest(permissions);
        startLogin(new ActivityStartActivityDelegate(activity), loginRequest);
    }

    LoginClient.Request getPendingLoginRequest() {
        return pendingLoginRequest;
    }

    private void validateReadPermissions(Collection<String> permissions) {
        if (permissions == null) {
            return;
        }
        for (String permission : permissions) {
            if (isPublishPermission(permission)) {
                throw new FacebookException(
                    String.format(
                        "Cannot pass a publish or manage permission (%s) to a request for read " +
                                "authorization",
                        permission));
            }
        }
    }

    private void validatePublishPermissions(Collection<String> permissions) {
        if (permissions == null) {
            return;
        }
        for (String permission : permissions) {
            if (!isPublishPermission(permission)) {
                throw new FacebookException(
                    String.format(
                        "Cannot pass a read permission (%s) to a request for publish authorization",
                        permission));
            }
        }
    }

    private static boolean isPublishPermission(String permission) {
        return permission != null &&
            (permission.startsWith(PUBLISH_PERMISSION_PREFIX) ||
                permission.startsWith(MANAGE_PERMISSION_PREFIX) ||
                OTHER_PUBLISH_PERMISSIONS.contains(permission));
    }

    private static Set<String> getOtherPublishPermissions() {
        HashSet<String> set = new HashSet<String>() {{
            add("ads_management");
            add("create_event");
            add("rsvp_event");
        }};
        return Collections.unmodifiableSet(set);
    }

    private LoginClient.Request createLoginRequest(Collection<String> permissions) {
        LoginClient.Request request = new LoginClient.Request(
                loginBehavior,
                Collections.unmodifiableSet(
                        permissions != null ? new HashSet(permissions) : new HashSet<String>()),
                defaultAudience,
                FacebookSdk.getApplicationId(),
                UUID.randomUUID().toString()
        );
        request.setRerequest(AccessToken.getCurrentAccessToken() != null);
        return request;
    }

    private void startLogin(
            StartActivityDelegate startActivityDelegate,
            LoginClient.Request request
    ) throws FacebookException {

        this.pendingLoginRequest = request;
        this.pendingLoggingExtras = new HashMap<>();
        this.context = startActivityDelegate.getActivityContext();

        logStartLogin();

        // Make sure the static handler for login is registered if there isn't an explicit callback
        CallbackManagerImpl.registerStaticCallback(
                CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        return LoginManager.this.onActivityResult(resultCode, data);
                    }
                }
        );

        boolean started = tryLoginActivity(startActivityDelegate, request);

        pendingLoggingExtras.put(
                LoginLogger.EVENT_EXTRAS_TRY_LOGIN_ACTIVITY,
                started ?
                AppEventsConstants.EVENT_PARAM_VALUE_YES : AppEventsConstants.EVENT_PARAM_VALUE_NO
        );

        if (!started) {
            FacebookException exception = new FacebookException(
                    "Log in attempt failed: LoginActivity could not be started");
            logCompleteLogin(LoginClient.Result.Code.ERROR, null, exception);
            this.pendingLoginRequest = null;
            throw exception;
        }
    }

    private LoginLogger getLogger() {
        if (loginLogger == null ||
                !loginLogger.getApplicationId().equals(
                        pendingLoginRequest.getApplicationId())) {
            loginLogger = new LoginLogger(
                    context,
                    pendingLoginRequest.getApplicationId());
        }
        return loginLogger;
    }

    private void logStartLogin() {
        getLogger().logStartLogin(pendingLoginRequest);
    }

    private void logCompleteLogin(LoginClient.Result.Code result, Map<String, String> resultExtras,
                                  Exception exception) {
        if (pendingLoginRequest == null) {
            // We don't expect this to happen, but if it does, log an event for diagnostic purposes.
            getLogger().logUnexpectedError(
                LoginLogger.EVENT_NAME_LOGIN_COMPLETE,
                "Unexpected call to logCompleteLogin with null pendingAuthorizationRequest."
            );
        } else {
            getLogger().logCompleteLogin(
                    pendingLoginRequest.getAuthId(),
                    pendingLoggingExtras,
                    result,
                    resultExtras,
                    exception);
        }
    }

    private boolean tryLoginActivity(
            StartActivityDelegate startActivityDelegate,
            LoginClient.Request request) {

        Intent intent = getLoginActivityIntent(request);

        if (!resolveIntent(intent)) {
            return false;
        }

        try {
            startActivityDelegate.startActivityForResult(
                    intent,
                    LoginClient.getLoginRequestCode());
        } catch (ActivityNotFoundException e) {
            return false;
        }

        return true;
    }

    private boolean resolveIntent(Intent intent) {
        ResolveInfo resolveInfo = FacebookSdk.getApplicationContext().getPackageManager()
            .resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        return true;
    }

    private Intent getLoginActivityIntent(LoginClient.Request request) {
        Intent intent = new Intent();
        intent.setClass(FacebookSdk.getApplicationContext(), FacebookActivity.class);
        intent.setAction(request.getLoginBehavior().toString());

        // Let LoginActivity populate extras appropriately
        LoginClient.Request authClientRequest = request;
        Bundle extras = LoginFragment.populateIntentExtras(authClientRequest);
        intent.putExtras(extras);

        return intent;
    }

    static LoginResult computeLoginResult(
            final LoginClient.Request request,
            final AccessToken newToken
    ) {
        Set<String> requestedPermissions = request.getPermissions();
        Set<String> grantedPermissions = new HashSet<String>(newToken.getPermissions());

        // If it's a reauth, subset the granted permissions to just the requested permissions
        // so we don't report implicit permissions like user_profile as recently granted.
        if (request.isRerequest()) {
            grantedPermissions.retainAll(requestedPermissions);
        }

        Set<String> deniedPermissions = new HashSet<String>(requestedPermissions);
        deniedPermissions.removeAll(grantedPermissions);
        return new LoginResult(newToken, grantedPermissions, deniedPermissions);
    }

    private void finishLogin(
            AccessToken newToken,
            FacebookException exception,
            boolean isCanceled,
            FacebookCallback<LoginResult>  callback) {
        if (newToken != null) {
            AccessToken.setCurrentAccessToken(newToken);
            Profile.fetchProfileForCurrentAccessToken();
        }

        if (callback != null) {
            LoginResult loginResult = newToken != null
                    ? computeLoginResult(pendingLoginRequest, newToken)
                    : null;
            // If there are no granted permissions, the operation is treated as cancel.
            if (isCanceled
                    || (loginResult != null
                           && loginResult.getRecentlyGrantedPermissions().size() == 0)) {
                callback.onCancel();
                return;
            }
            if (exception != null) {
                callback.onError(exception);
            } else if (newToken != null) {
                callback.onSuccess(loginResult);
            }
        }
    }

    private static class ActivityStartActivityDelegate implements StartActivityDelegate {
        private final Activity activity;

        ActivityStartActivityDelegate(final Activity activity) {
            Validate.notNull(activity, "activity");
            this.activity = activity;
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            activity.startActivityForResult(intent, requestCode);
        }

        @Override
        public Activity getActivityContext() {
            return activity;
        }
    }

    private static class FragmentStartActivityDelegate implements StartActivityDelegate {
        private final Fragment fragment;

        FragmentStartActivityDelegate(final Fragment fragment) {
            Validate.notNull(fragment, "fragment");
            this.fragment = fragment;
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            fragment.startActivityForResult(intent, requestCode);
        }

        @Override
        public Activity getActivityContext() {
            return fragment.getActivity();
        }
    }
}
