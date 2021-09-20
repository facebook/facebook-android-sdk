/*
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.fragment.app.Fragment;
import com.facebook.AccessToken;
import com.facebook.AuthenticationToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookActivity;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphResponse;
import com.facebook.LoginStatusCallback;
import com.facebook.Profile;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** This class manages login and permissions for Facebook. */
public class LoginManager {
  private static final String PUBLISH_PERMISSION_PREFIX = "publish";
  private static final String MANAGE_PERMISSION_PREFIX = "manage";
  private static final String EXPRESS_LOGIN_ALLOWED = "express_login_allowed";
  private static final String PREFERENCE_LOGIN_MANAGER = "com.facebook.loginManager";
  private static final Set<String> OTHER_PUBLISH_PERMISSIONS = getOtherPublishPermissions();
  private static final String TAG = LoginManager.class.toString();

  private static volatile LoginManager instance;

  private LoginBehavior loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK;
  private DefaultAudience defaultAudience = DefaultAudience.FRIENDS;
  private final SharedPreferences sharedPreferences;
  private String authType = ServerProtocol.DIALOG_REREQUEST_AUTH_TYPE;
  @Nullable private String messengerPageId;
  private boolean resetMessengerState;
  private LoginTargetApp targetApp = LoginTargetApp.FACEBOOK;
  private boolean isFamilyLogin = false;
  private boolean shouldSkipAccountDeduplication = false;

  LoginManager() {
    Validate.sdkInitialized();
    sharedPreferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(PREFERENCE_LOGIN_MANAGER, Context.MODE_PRIVATE);

    if (FacebookSdk.hasCustomTabsPrefetching && CustomTabUtils.getChromePackage() != null) {
      // Pre-load CCT in case they are going to be used. It's happening in background,
      // and shouldn't slow anything down. Actual CCT launch should happen much faster.
      CustomTabPrefetchHelper prefetchHelper = new CustomTabPrefetchHelper();
      CustomTabsClient.bindCustomTabsService(
          FacebookSdk.getApplicationContext(), "com.android.chrome", prefetchHelper);
      CustomTabsClient.connectAndInitialize(
          FacebookSdk.getApplicationContext(),
          FacebookSdk.getApplicationContext().getPackageName());
    }
  }

  /**
   * Getter for the login manager.
   *
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
        new ActivityStartActivityDelegate(activity), createLoginRequestFromResponse(response));
  }

  /**
   * Starts the login process to resolve the error defined in the response. The registered login
   * callbacks will be called on completion.
   *
   * <p>This method is deprecated and it's better to use the method with a CallbackManager as the
   * second parameter. The new method with the CallbackManager as a parameter will use AndroidX
   * activity result APIs so you won't need to override onActivityResult method on the fragment.
   *
   * @param fragment The fragment which is starting the login process.
   * @param response The response that has the error.
   */
  @Deprecated
  public void resolveError(Fragment fragment, final GraphResponse response) {
    this.resolveError(new FragmentWrapper(fragment), response);
  }

  /**
   * Starts the login process to resolve the error defined in the response. The registered login
   * callbacks will be called on completion.
   *
   * @param fragment The fragment which is starting the login process.
   * @param callbackManager The callback manager which is used to register callbacks.
   * @param response The response that has the error.
   */
  public void resolveError(
      @NonNull Fragment fragment,
      @NonNull CallbackManager callbackManager,
      @NonNull final GraphResponse response) {
    ComponentActivity activity = fragment.getActivity();
    if (activity != null) {
      this.resolveError(activity, callbackManager, response);
    } else {
      throw new FacebookException(
          "Cannot obtain activity context on the fragment " + fragment.toString());
    }
  }

  /**
   * Starts the login process to resolve the error defined in the response. The registered login
   * callbacks will be called on completion.
   *
   * @param fragment The android.app.Fragment which is starting the login process.
   * @param response The response that has the error.
   */
  public void resolveError(final android.app.Fragment fragment, final GraphResponse response) {
    this.resolveError(new FragmentWrapper(fragment), response);
  }

  /**
   * Starts the login process to resolve the error defined in the response. The registered login
   * callbacks will be called on completion.
   *
   * @param fragment The fragment which is starting the login process.
   * @param response The response that has the error.
   */
  private void resolveError(final FragmentWrapper fragment, final GraphResponse response) {
    startLogin(
        new FragmentStartActivityDelegate(fragment), createLoginRequestFromResponse(response));
  }

  /**
   * Starts the login process to resolve the error defined in the response. The registered login
   * callbacks will be called on completion.
   *
   * @param activityResultRegistryOwner The activity result register owner. Normally it is an
   *     androidx ComponentActivity.
   * @param callbackManager The callback manager from Facebook SDK.
   * @param response The response that has the error.
   */
  public void resolveError(
      @NonNull final ActivityResultRegistryOwner activityResultRegistryOwner,
      @NonNull final CallbackManager callbackManager,
      @NonNull final GraphResponse response) {
    startLogin(
        new AndroidxActivityResultRegistryOwnerStartActivityDelegate(
            activityResultRegistryOwner, callbackManager),
        createLoginRequestFromResponse(response));
  }

  private LoginClient.Request createLoginRequestFromResponse(final GraphResponse response) {
    Validate.notNull(response, "response");
    AccessToken failedToken = response.getRequest().getAccessToken();
    return createLoginRequest(failedToken != null ? failedToken.getPermissions() : null);
  }

  /**
   * Registers a login callback to the given callback manager.
   *
   * @param callbackManager The callback manager that will encapsulate the callback.
   * @param callback The login callback that will be called on login completion.
   */
  public void registerCallback(
      final CallbackManager callbackManager, final FacebookCallback<LoginResult> callback) {
    if (!(callbackManager instanceof CallbackManagerImpl)) {
      throw new FacebookException(
          "Unexpected CallbackManager, " + "please use the provided Factory.");
    }
    ((CallbackManagerImpl) callbackManager)
        .registerCallback(
            CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
            new CallbackManagerImpl.Callback() {
              @Override
              public boolean onActivityResult(int resultCode, Intent data) {
                return LoginManager.this.onActivityResult(resultCode, data, callback);
              }
            });
  }

  /**
   * Unregisters a login callback to the given callback manager.
   *
   * @param callbackManager The callback manager that will encapsulate the callback.
   */
  public void unregisterCallback(final CallbackManager callbackManager) {
    if (!(callbackManager instanceof CallbackManagerImpl)) {
      throw new FacebookException(
          "Unexpected CallbackManager, " + "please use the provided Factory.");
    }
    ((CallbackManagerImpl) callbackManager)
        .unregisterCallback(CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode());
  }

  boolean onActivityResult(int resultCode, Intent data) {
    return onActivityResult(resultCode, data, null);
  }

  boolean onActivityResult(int resultCode, Intent data, FacebookCallback<LoginResult> callback) {
    FacebookException exception = null;
    AccessToken accessToken = null;
    AuthenticationToken authenticationToken = null;
    LoginClient.Result.Code code = LoginClient.Result.Code.ERROR;
    Map<String, String> loggingExtras = null;
    LoginClient.Request originalRequest = null;

    boolean isCanceled = false;
    if (data != null) {
      LoginClient.Result result = data.getParcelableExtra(LoginFragment.RESULT_KEY);
      if (result != null) {
        originalRequest = result.request;
        code = result.code;
        if (resultCode == Activity.RESULT_OK) {
          if (result.code == LoginClient.Result.Code.SUCCESS) {
            accessToken = result.token;
            authenticationToken = result.authenticationToken;
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

    if (exception == null && accessToken == null && !isCanceled) {
      exception = new FacebookException("Unexpected call to LoginManager.onActivityResult");
    }

    boolean wasLoginActivityTried = true;
    Context context = null; // Sadly, there is no way to get activity context at this point.S
    logCompleteLogin(
        context, code, loggingExtras, exception, wasLoginActivityTried, originalRequest);

    finishLogin(accessToken, authenticationToken, originalRequest, exception, isCanceled, callback);

    return true;
  }

  static @Nullable Map<String, String> getExtraDataFromIntent(Intent intent) {
    if (intent == null) {
      return null;
    }
    LoginClient.Result result = intent.getParcelableExtra(LoginFragment.RESULT_KEY);
    if (result == null) {
      return null;
    }
    return result.extraData;
  }

  /**
   * Getter for the login behavior.
   *
   * @return the login behavior.
   */
  public LoginBehavior getLoginBehavior() {
    return loginBehavior;
  }

  /**
   * Setter for the login behavior.
   *
   * @param loginBehavior The login behavior.
   * @return The login manager.
   */
  public LoginManager setLoginBehavior(LoginBehavior loginBehavior) {
    this.loginBehavior = loginBehavior;
    return this;
  }

  /**
   * Getter for the login target app.
   *
   * @return the login target app.
   */
  public LoginTargetApp getLoginTargetApp() {
    return targetApp;
  }

  /**
   * Setter for the login target app.
   *
   * @param targetApp The login target app.
   * @return The login manager.
   */
  public LoginManager setLoginTargetApp(LoginTargetApp targetApp) {
    this.targetApp = targetApp;
    return this;
  }

  /**
   * Getter for the default audience.
   *
   * @return The default audience.
   */
  public DefaultAudience getDefaultAudience() {
    return defaultAudience;
  }

  /**
   * Setter for the default audience.
   *
   * @param defaultAudience The default audience.
   * @return The login manager.
   */
  public LoginManager setDefaultAudience(DefaultAudience defaultAudience) {
    this.defaultAudience = defaultAudience;
    return this;
  }

  /**
   * Getter for the authType
   *
   * @return The authType
   */
  public String getAuthType() {
    return this.authType;
  }

  /**
   * Setter for the authType
   *
   * @param authType The authType
   * @return The login manager.
   */
  public LoginManager setAuthType(final String authType) {
    this.authType = authType;
    return this;
  }

  /**
   * Setter for the messengerPageId
   *
   * @param messengerPageId The messengerPageId
   * @return The login manager.
   */
  public LoginManager setMessengerPageId(@Nullable final String messengerPageId) {
    this.messengerPageId = messengerPageId;
    return this;
  }

  /**
   * Setter for the resetMessengerState. For developers of the app only.
   *
   * @param resetMessengerState Whether to enable resetMessengerState
   * @return The login manager.
   */
  public LoginManager setResetMessengerState(final boolean resetMessengerState) {
    this.resetMessengerState = resetMessengerState;
    return this;
  }

  /**
   * Determines whether we are using the cross Family of Apps login experience
   *
   * @return True if using cross Family of Apps login
   */
  public boolean isFamilyLogin() {
    return isFamilyLogin;
  }

  /**
   * Setter for whether we are using cross Family of Apps login
   *
   * @param isFamilyLogin Whether we are using cross Family of Apps login
   * @return The login manager.
   */
  public LoginManager setFamilyLogin(boolean isFamilyLogin) {
    this.isFamilyLogin = isFamilyLogin;
    return this;
  }

  /**
   * Determines if we should skip deduplicating account during x-FoA login.
   *
   * @return True if Account deduplication is opted out in Family of Apps login
   */
  public boolean getShouldSkipAccountDeduplication() {
    return shouldSkipAccountDeduplication;
  }
  /**
   * Setter for whether we are skipping deduplicating account during x-FoA login.
   *
   * @param shouldSkipAccountDeduplication Whether we want to opt out account deduplication
   *     experience in Family of Apps login
   * @return The login manager.
   */
  public LoginManager setShouldSkipAccountDeduplication(boolean shouldSkipAccountDeduplication) {
    this.shouldSkipAccountDeduplication = shouldSkipAccountDeduplication;
    return this;
  }

  /** Logs out the user. */
  public void logOut() {
    AccessToken.setCurrentAccessToken(null);
    Profile.setCurrentProfile(null);
    setExpressLoginStatus(false);
  }

  /**
   * Retrieves the login status for the user. This will return an access token for the app if a user
   * is logged into the Facebook for Android app on the same device and that user had previously
   * logged into the app. If an access token was retrieved then a toast will be shown telling the
   * user that they have been logged in.
   *
   * @param context An Android context
   * @param responseCallback The callback to be called when the request completes
   */
  public void retrieveLoginStatus(
      final Context context, final LoginStatusCallback responseCallback) {
    retrieveLoginStatus(context, LoginStatusClient.DEFAULT_TOAST_DURATION_MS, responseCallback);
  }

  /**
   * Retrieves the login status for the user. This will return an access token for the app if a user
   * is logged into the Facebook for Android app on the same device and that user had previously
   * logged into the app. If an access token was retrieved then a toast will be shown telling the
   * user that they have been logged in.
   *
   * @param context An Android context
   * @param responseCallback The callback to be called when the request completes
   * @param toastDurationMs The duration to show the success toast in milliseconds
   */
  public void retrieveLoginStatus(
      final Context context,
      final long toastDurationMs,
      final LoginStatusCallback responseCallback) {
    retrieveLoginStatusImpl(context, responseCallback, toastDurationMs);
  }

  /**
   * Logs the user in with the requested read permissions.
   *
   * <p>This method is deprecated and it's better to use the method with a CallbackManager as the
   * second parameter. The new method with the CallbackManager as a parameter will use AndroidX
   * activity result APIs so you won't need to override onActivityResult method on the fragment.
   *
   * @param fragment The androidx.fragment.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  @Deprecated
  public void logInWithReadPermissions(Fragment fragment, Collection<String> permissions) {
    logInWithReadPermissions(new FragmentWrapper(fragment), permissions);
  }

  /**
   * Logs the user in with the requested read permissions.
   *
   * @param fragment The androidx.fragment.Fragment which is starting the login process.
   * @param callbackManager The callback manager which is used to register callbacks.
   * @param permissions The requested permissions.
   */
  public void logInWithReadPermissions(
      @NonNull Fragment fragment,
      @NonNull CallbackManager callbackManager,
      @NonNull Collection<String> permissions) {
    ComponentActivity activity = fragment.getActivity();
    if (activity != null) {
      logInWithReadPermissions(activity, callbackManager, permissions);
    } else {
      throw new FacebookException(
          "Cannot obtain activity context on the fragment " + fragment.toString());
    }
  }

  /**
   * Logs the user in with the requested read permissions.
   *
   * @param fragment The android.app.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logInWithReadPermissions(
      android.app.Fragment fragment, Collection<String> permissions) {
    logInWithReadPermissions(new FragmentWrapper(fragment), permissions);
  }

  /**
   * Logs the user in with the requested read permissions.
   *
   * @param fragment The fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  private void logInWithReadPermissions(FragmentWrapper fragment, Collection<String> permissions) {
    validateReadPermissions(permissions);

    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    logIn(fragment, loginConfig);
  }

  /**
   * Logs the user in with the requested read permissions.
   *
   * @param activity The activity which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logInWithReadPermissions(Activity activity, Collection<String> permissions) {
    validateReadPermissions(permissions);

    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    logIn(activity, loginConfig);
  }

  /**
   * Logs the user in with the requested read permissions.
   *
   * @param activityResultRegistryOwner The activity result register owner. Normally it is an
   *     androidx ComponentActivity.
   * @param callbackManager The callback manager from Facebook SDK.
   * @param permissions The requested permissions.
   */
  public void logInWithReadPermissions(
      @NonNull ActivityResultRegistryOwner activityResultRegistryOwner,
      @NonNull CallbackManager callbackManager,
      @NonNull Collection<String> permissions) {
    validateReadPermissions(permissions);
    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    logIn(activityResultRegistryOwner, callbackManager, loginConfig);
  }
  /**
   * Logs the user in with the requested configuration.
   *
   * @param fragment The android.support.v4.app.Fragment which is starting the login process.
   * @param loginConfig The login configuration
   */
  public void logInWithConfiguration(Fragment fragment, @NonNull LoginConfiguration loginConfig) {
    loginWithConfiguration(new FragmentWrapper(fragment), loginConfig);
  }

  /**
   * Logs the user in with the requested configuration.
   *
   * @param fragment The fragment which is starting the login process.
   * @param loginConfig The login configuration.
   */
  private void loginWithConfiguration(
      FragmentWrapper fragment, @NonNull LoginConfiguration loginConfig) {
    logIn(fragment, loginConfig);
  }

  /**
   * Logs the user in with the requested configuration.
   *
   * @param activity The activity which is starting the login process.
   * @param loginConfig The login configuration
   */
  public void loginWithConfiguration(Activity activity, @NonNull LoginConfiguration loginConfig) {
    logIn(activity, loginConfig);
  }

  /**
   * Reauthorize data access
   *
   * @param activity The activity which is starting the reauthorization process.
   */
  public void reauthorizeDataAccess(Activity activity) {
    LoginClient.Request loginRequest = createReauthorizeRequest();
    startLogin(new ActivityStartActivityDelegate(activity), loginRequest);
  }

  /**
   * Reauthorize data access
   *
   * @param fragment The android.support.v4.app.Fragment starting the reauthorization process.
   */
  public void reauthorizeDataAccess(Fragment fragment) {
    reauthorizeDataAccess(new FragmentWrapper(fragment));
  }

  /**
   * Reauthorize data access
   *
   * @param fragment The fragment which is starting the reauthorization process.
   */
  private void reauthorizeDataAccess(FragmentWrapper fragment) {
    LoginClient.Request loginRequest = createReauthorizeRequest();
    startLogin(new FragmentStartActivityDelegate(fragment), loginRequest);
  }

  /**
   * Logs the user in with the requested publish permissions.
   *
   * <p>This method is deprecated and it's better to use the method with a CallbackManager as the
   * second parameter. The new method with the CallbackManager as a parameter will use AndroidX
   * activity result APIs so you won't need to override onActivityResult method on the fragment.
   *
   * @param fragment The androidx.fragment.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  @Deprecated
  public void logInWithPublishPermissions(Fragment fragment, Collection<String> permissions) {
    logInWithPublishPermissions(new FragmentWrapper(fragment), permissions);
  }

  /**
   * Logs the user in with the requested publish permissions.
   *
   * @param fragment The androidx.fragment.Fragment which is starting the login process.
   * @param callbackManager The callback manager which is used to register callbacks.
   * @param permissions The requested permissions.
   */
  public void logInWithPublishPermissions(
      @NonNull Fragment fragment,
      @NonNull CallbackManager callbackManager,
      @NonNull Collection<String> permissions) {
    ComponentActivity activity = fragment.getActivity();
    if (activity != null) {
      logInWithPublishPermissions(activity, callbackManager, permissions);
    } else {
      throw new FacebookException(
          "Cannot obtain activity context on the fragment " + fragment.toString());
    }
  }

  /**
   * Logs the user in with the requested publish permissions.
   *
   * @param fragment The android.app.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logInWithPublishPermissions(
      android.app.Fragment fragment, Collection<String> permissions) {
    logInWithPublishPermissions(new FragmentWrapper(fragment), permissions);
  }

  /**
   * Logs the user in with the requested publish permissions.
   *
   * @param fragment The fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  private void logInWithPublishPermissions(
      FragmentWrapper fragment, Collection<String> permissions) {
    validatePublishPermissions(permissions);

    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    loginWithConfiguration(fragment, loginConfig);
  }

  /**
   * Logs the user in with the requested publish permissions.
   *
   * @param activity The activity which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logInWithPublishPermissions(Activity activity, Collection<String> permissions) {
    validatePublishPermissions(permissions);

    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    loginWithConfiguration(activity, loginConfig);
  }

  /**
   * Logs the user in with the requested read permissions.
   *
   * @param activityResultRegistryOwner The activity result register owner. Normally it is an
   *     androidx ComponentActivity.
   * @param callbackManager The callback manager from Facebook SDK.
   * @param permissions The requested permissions.
   */
  public void logInWithPublishPermissions(
      @NonNull ActivityResultRegistryOwner activityResultRegistryOwner,
      @NonNull CallbackManager callbackManager,
      @NonNull Collection<String> permissions) {
    validatePublishPermissions(permissions);
    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    logIn(activityResultRegistryOwner, callbackManager, loginConfig);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param fragment The android.support.v4.app.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logIn(Fragment fragment, Collection<String> permissions) {
    logIn(new FragmentWrapper(fragment), permissions);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param fragment The android.support.v4.app.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   * @param loggerID Override the default logger ID for the request
   */
  public void logIn(Fragment fragment, Collection<String> permissions, String loggerID) {
    logIn(new FragmentWrapper(fragment), permissions, loggerID);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param fragment The android.app.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logIn(android.app.Fragment fragment, Collection<String> permissions) {
    logIn(new FragmentWrapper(fragment), permissions);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param fragment The android.app.Fragment which is starting the login process.
   * @param permissions The requested permissions.
   * @param loggerID Override the default logger ID for the request
   */
  public void logIn(
      android.app.Fragment fragment, Collection<String> permissions, String loggerID) {
    logIn(new FragmentWrapper(fragment), permissions, loggerID);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param fragment The fragment which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logIn(FragmentWrapper fragment, Collection<String> permissions) {
    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    logIn(fragment, loginConfig);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param fragment The fragment which is starting the login process.
   * @param permissions The requested permissions.
   * @param loggerID Override the default logger ID for the request
   */
  public void logIn(FragmentWrapper fragment, Collection<String> permissions, String loggerID) {
    LoginClient.Request loginRequest = createLoginRequest(permissions, loggerID);
    startLogin(new FragmentStartActivityDelegate(fragment), loginRequest);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param activity The activity which is starting the login process.
   * @param permissions The requested permissions.
   */
  public void logIn(Activity activity, Collection<String> permissions) {
    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    logIn(activity, loginConfig);
  }

  /**
   * Logs the user in with the requested login configuration.
   *
   * @param fragment The fragment which is starting the login process.
   * @param loginConfig The login config of the request
   */
  public void logIn(FragmentWrapper fragment, @NonNull LoginConfiguration loginConfig) {
    LoginClient.Request loginRequest = createLoginRequestWithConfig(loginConfig);
    startLogin(new FragmentStartActivityDelegate(fragment), loginRequest);
  }

  /**
   * Logs the user in with the requested configuration.
   *
   * @param activity The activity which is starting the login process.
   * @param loginConfig The login config of the request
   */
  public void logIn(Activity activity, @NonNull LoginConfiguration loginConfig) {
    if (activity instanceof ActivityResultRegistryOwner) {
      Log.w(
          TAG,
          "You're calling logging in Facebook with an activity supports androidx activity result APIs. Please follow our document to upgrade to new APIs to avoid overriding onActivityResult().");
    }
    LoginClient.Request loginRequest = createLoginRequestWithConfig(loginConfig);
    startLogin(new ActivityStartActivityDelegate(activity), loginRequest);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param activity The activity which is starting the login process.
   * @param permissions The requested permissions.
   * @param loggerID Override the default logger ID for the request
   */
  public void logIn(Activity activity, Collection<String> permissions, String loggerID) {
    LoginClient.Request loginRequest = createLoginRequest(permissions, loggerID);
    startLogin(new ActivityStartActivityDelegate(activity), loginRequest);
  }

  /**
   * Logs the user in with the requested configuration. This method is specialized for using
   * androidx activity results APIs.
   *
   * @param activityResultRegistryOwner The activity result register owner. Normally it is an
   *     androidx ComponentActivity.
   * @param callbackManager The callback manager from Facebook SDK.
   * @param loginConfig The login config of the request.
   */
  private void logIn(
      @NonNull ActivityResultRegistryOwner activityResultRegistryOwner,
      @NonNull CallbackManager callbackManager,
      @NonNull LoginConfiguration loginConfig) {
    LoginClient.Request loginRequest = createLoginRequestWithConfig(loginConfig);
    startLogin(
        new AndroidxActivityResultRegistryOwnerStartActivityDelegate(
            activityResultRegistryOwner, callbackManager),
        loginRequest);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param activityResultRegistryOwner The activity result register owner. Normally it is an
   *     androidx ComponentActivity.
   * @param callbackManager The callback manager from Facebook SDK.
   * @param permissions The requested permissions.
   * @param loggerID Override the default logger ID for the request
   */
  public void logIn(
      @NonNull ActivityResultRegistryOwner activityResultRegistryOwner,
      @NonNull CallbackManager callbackManager,
      @NonNull Collection<String> permissions,
      String loggerID) {
    LoginClient.Request loginRequest = createLoginRequest(permissions, loggerID);
    startLogin(
        new AndroidxActivityResultRegistryOwnerStartActivityDelegate(
            activityResultRegistryOwner, callbackManager),
        loginRequest);
  }

  /**
   * Logs the user in with the requested permissions.
   *
   * @param activityResultRegistryOwner The activity result register owner. Normally it is an
   *     androidx ComponentActivity.
   * @param callbackManager The callback manager from Facebook SDK.
   * @param permissions The requested permissions.
   */
  public void logIn(
      @NonNull ActivityResultRegistryOwner activityResultRegistryOwner,
      @NonNull CallbackManager callbackManager,
      @NonNull Collection<String> permissions) {
    LoginConfiguration loginConfig = new LoginConfiguration(permissions);
    logIn(activityResultRegistryOwner, callbackManager, loginConfig);
  }

  private void validateReadPermissions(Collection<String> permissions) {
    if (permissions == null) {
      return;
    }
    for (String permission : permissions) {
      if (isPublishPermission(permission)) {
        throw new FacebookException(
            String.format(
                "Cannot pass a publish or manage permission (%s) to a request for read "
                    + "authorization",
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

  static boolean isPublishPermission(String permission) {
    return permission != null
        && (permission.startsWith(PUBLISH_PERMISSION_PREFIX)
            || permission.startsWith(MANAGE_PERMISSION_PREFIX)
            || OTHER_PUBLISH_PERMISSIONS.contains(permission));
  }

  private static Set<String> getOtherPublishPermissions() {
    HashSet<String> set =
        new HashSet<String>() {
          {
            add("ads_management");
            add("create_event");
            add("rsvp_event");
          }
        };
    return Collections.unmodifiableSet(set);
  }

  protected LoginClient.Request createLoginRequestWithConfig(LoginConfiguration loginConfig) {
    LoginClient.Request request =
        new LoginClient.Request(
            loginBehavior,
            Collections.unmodifiableSet(
                loginConfig.getPermissions() != null
                    ? new HashSet(loginConfig.getPermissions())
                    : new HashSet<String>()),
            defaultAudience,
            authType,
            FacebookSdk.getApplicationId(),
            UUID.randomUUID().toString(),
            targetApp,
            loginConfig.getNonce());
    request.setRerequest(AccessToken.isCurrentAccessTokenActive());
    request.setMessengerPageId(messengerPageId);
    request.setResetMessengerState(resetMessengerState);
    request.setFamilyLogin(isFamilyLogin);
    request.setShouldSkipAccountDeduplication(shouldSkipAccountDeduplication);
    return request;
  }

  protected LoginClient.Request createLoginRequest(Collection<String> permissions) {
    LoginClient.Request request =
        new LoginClient.Request(
            loginBehavior,
            Collections.unmodifiableSet(
                permissions != null ? new HashSet(permissions) : new HashSet<String>()),
            defaultAudience,
            authType,
            FacebookSdk.getApplicationId(),
            UUID.randomUUID().toString(),
            targetApp);
    request.setRerequest(AccessToken.isCurrentAccessTokenActive());
    request.setMessengerPageId(messengerPageId);
    request.setResetMessengerState(resetMessengerState);
    request.setFamilyLogin(isFamilyLogin);
    request.setShouldSkipAccountDeduplication(shouldSkipAccountDeduplication);
    return request;
  }

  protected LoginClient.Request createLoginRequest(
      Collection<String> permissions, String loggerID) {
    LoginClient.Request request =
        new LoginClient.Request(
            loginBehavior,
            Collections.unmodifiableSet(
                permissions != null ? new HashSet(permissions) : new HashSet<String>()),
            defaultAudience,
            authType,
            FacebookSdk.getApplicationId(),
            loggerID,
            targetApp);
    request.setRerequest(AccessToken.isCurrentAccessTokenActive());
    request.setMessengerPageId(messengerPageId);
    request.setResetMessengerState(resetMessengerState);
    request.setFamilyLogin(isFamilyLogin);
    request.setShouldSkipAccountDeduplication(shouldSkipAccountDeduplication);
    return request;
  }

  protected LoginClient.Request createReauthorizeRequest() {
    LoginClient.Request request =
        new LoginClient.Request(
            LoginBehavior.DIALOG_ONLY,
            new HashSet<String>(),
            defaultAudience,
            "reauthorize",
            FacebookSdk.getApplicationId(),
            UUID.randomUUID().toString(),
            targetApp);
    request.setFamilyLogin(isFamilyLogin);
    request.setShouldSkipAccountDeduplication(shouldSkipAccountDeduplication);
    return request;
  }

  private void startLogin(StartActivityDelegate startActivityDelegate, LoginClient.Request request)
      throws FacebookException {

    logStartLogin(startActivityDelegate.getActivityContext(), request);

    // Make sure the static handler for login is registered if there isn't an explicit callback
    CallbackManagerImpl.registerStaticCallback(
        CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
        new CallbackManagerImpl.Callback() {
          @Override
          public boolean onActivityResult(int resultCode, Intent data) {
            return LoginManager.this.onActivityResult(resultCode, data);
          }
        });

    boolean started = tryFacebookActivity(startActivityDelegate, request);

    if (!started) {
      FacebookException exception =
          new FacebookException(
              "Log in attempt failed: FacebookActivity could not be started."
                  + " Please make sure you added FacebookActivity to the AndroidManifest.");
      boolean wasLoginActivityTried = false;
      logCompleteLogin(
          startActivityDelegate.getActivityContext(),
          LoginClient.Result.Code.ERROR,
          null,
          exception,
          wasLoginActivityTried,
          request);
      throw exception;
    }
  }

  private void logStartLogin(@Nullable Context context, LoginClient.Request loginRequest) {
    LoginLogger loginLogger = LoginLoggerHolder.getLogger(context);
    if (loginLogger != null && loginRequest != null) {
      loginLogger.logStartLogin(
          loginRequest,
          loginRequest.isFamilyLogin()
              ? LoginLogger.EVENT_NAME_FOA_LOGIN_START
              : LoginLogger.EVENT_NAME_LOGIN_START);
    }
  }

  private void logCompleteLogin(
      @Nullable Context context,
      LoginClient.Result.Code result,
      Map<String, String> resultExtras,
      Exception exception,
      boolean wasLoginActivityTried,
      LoginClient.Request request) {
    LoginLogger loginLogger = LoginLoggerHolder.getLogger(context);
    if (loginLogger == null) {
      return;
    }
    if (request == null) {
      // We don't expect this to happen, but if it does, log an event for diagnostic purposes.
      loginLogger.logUnexpectedError(
          LoginLogger.EVENT_NAME_LOGIN_COMPLETE,
          "Unexpected call to logCompleteLogin with null pendingAuthorizationRequest.");
    } else {
      HashMap<String, String> pendingLoggingExtras = new HashMap<>();
      pendingLoggingExtras.put(
          LoginLogger.EVENT_EXTRAS_TRY_LOGIN_ACTIVITY,
          wasLoginActivityTried
              ? AppEventsConstants.EVENT_PARAM_VALUE_YES
              : AppEventsConstants.EVENT_PARAM_VALUE_NO);
      loginLogger.logCompleteLogin(
          request.getAuthId(),
          pendingLoggingExtras,
          result,
          resultExtras,
          exception,
          request.isFamilyLogin()
              ? LoginLogger.EVENT_NAME_FOA_LOGIN_COMPLETE
              : LoginLogger.EVENT_NAME_LOGIN_COMPLETE);
    }
  }

  private boolean tryFacebookActivity(
      StartActivityDelegate startActivityDelegate, LoginClient.Request request) {

    Intent intent = getFacebookActivityIntent(request);

    if (!resolveIntent(intent)) {
      return false;
    }

    try {
      startActivityDelegate.startActivityForResult(intent, LoginClient.getLoginRequestCode());
    } catch (ActivityNotFoundException e) {
      return false;
    }

    return true;
  }

  private boolean resolveIntent(Intent intent) {
    ResolveInfo resolveInfo =
        FacebookSdk.getApplicationContext().getPackageManager().resolveActivity(intent, 0);
    return resolveInfo != null;
  }

  protected Intent getFacebookActivityIntent(LoginClient.Request request) {
    Intent intent = new Intent();
    intent.setClass(FacebookSdk.getApplicationContext(), FacebookActivity.class);
    intent.setAction(request.getLoginBehavior().toString());

    // Let FacebookActivity populate extras appropriately
    Bundle extras = new Bundle();
    extras.putParcelable(LoginFragment.EXTRA_REQUEST, request);
    intent.putExtra(LoginFragment.REQUEST_KEY, extras);

    return intent;
  }

  static LoginResult computeLoginResult(
      final LoginClient.Request request,
      final AccessToken newToken,
      @Nullable final AuthenticationToken newIdToken) {
    Set<String> requestedPermissions = request.getPermissions();
    Set<String> grantedPermissions = new HashSet<String>(newToken.getPermissions());

    // If it's a reauth, subset the granted permissions to just the requested permissions
    // so we don't report implicit permissions like user_profile as recently granted.
    if (request.isRerequest()) {
      grantedPermissions.retainAll(requestedPermissions);
    }

    Set<String> deniedPermissions = new HashSet<String>(requestedPermissions);
    deniedPermissions.removeAll(grantedPermissions);
    return new LoginResult(newToken, newIdToken, grantedPermissions, deniedPermissions);
  }

  private void finishLogin(
      AccessToken newToken,
      @Nullable AuthenticationToken newIdToken,
      LoginClient.Request origRequest,
      FacebookException exception,
      boolean isCanceled,
      FacebookCallback<LoginResult> callback) {
    if (newToken != null) {
      AccessToken.setCurrentAccessToken(newToken);
      Profile.fetchProfileForCurrentAccessToken();
    }

    if (callback != null) {
      LoginResult loginResult =
          newToken != null ? computeLoginResult(origRequest, newToken, newIdToken) : null;
      // If there are no granted permissions, the operation is treated as cancel.
      if (isCanceled
          || (loginResult != null && loginResult.getRecentlyGrantedPermissions().size() == 0)) {
        callback.onCancel();
      } else if (exception != null) {
        callback.onError(exception);
      } else if (newToken != null) {
        setExpressLoginStatus(true);
        callback.onSuccess(loginResult);
      }
    }
  }

  private void retrieveLoginStatusImpl(
      final Context context,
      final LoginStatusCallback responseCallback,
      final long toastDurationMs) {
    final String applicationId = FacebookSdk.getApplicationId();
    final String loggerRef = UUID.randomUUID().toString();

    final LoginLogger logger = new LoginLogger(context, applicationId);

    if (!isExpressLoginAllowed()) {
      logger.logLoginStatusFailure(loggerRef);
      responseCallback.onFailure();
      return;
    }

    final LoginStatusClient client =
        new LoginStatusClient(
            context,
            applicationId,
            loggerRef,
            FacebookSdk.getGraphApiVersion(),
            toastDurationMs,
            null); // TODO T99739388: replace null with actual nonce

    final LoginStatusClient.CompletedListener callback =
        new LoginStatusClient.CompletedListener() {
          @Override
          public void completed(Bundle result) {
            if (result != null) {

              final String errorType = result.getString(NativeProtocol.STATUS_ERROR_TYPE);
              final String errorDescription =
                  result.getString(NativeProtocol.STATUS_ERROR_DESCRIPTION);
              if (errorType != null) {
                handleLoginStatusError(
                    errorType, errorDescription, loggerRef, logger, responseCallback);
              } else {
                final String token = result.getString(NativeProtocol.EXTRA_ACCESS_TOKEN);
                Date expires =
                    Utility.getBundleLongAsDate(
                        result, NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, new Date(0));
                final ArrayList<String> permissions =
                    result.getStringArrayList(NativeProtocol.EXTRA_PERMISSIONS);
                final String signedRequest =
                    result.getString(NativeProtocol.RESULT_ARGS_SIGNED_REQUEST);
                final String graphDomain =
                    result.getString(NativeProtocol.RESULT_ARGS_GRAPH_DOMAIN);
                Date dataAccessExpirationTime =
                    Utility.getBundleLongAsDate(
                        result, NativeProtocol.EXTRA_DATA_ACCESS_EXPIRATION_TIME, new Date(0));
                String userId = null;
                if (!Utility.isNullOrEmpty(signedRequest)) {
                  userId = LoginMethodHandler.getUserIDFromSignedRequest(signedRequest);
                }

                if (!Utility.isNullOrEmpty(token)
                    && permissions != null
                    && !permissions.isEmpty()
                    && !Utility.isNullOrEmpty(userId)) {
                  final AccessToken accessToken =
                      new AccessToken(
                          token,
                          applicationId,
                          userId,
                          permissions,
                          null,
                          null,
                          null,
                          expires,
                          null,
                          dataAccessExpirationTime,
                          graphDomain);
                  AccessToken.setCurrentAccessToken(accessToken);
                  Profile.fetchProfileForCurrentAccessToken();

                  logger.logLoginStatusSuccess(loggerRef);
                  responseCallback.onCompleted(accessToken);
                } else {
                  logger.logLoginStatusFailure(loggerRef);
                  responseCallback.onFailure();
                }
              }
            } else {
              logger.logLoginStatusFailure(loggerRef);
              responseCallback.onFailure();
            }
          }
        };

    client.setCompletedListener(callback);
    logger.logLoginStatusStart(loggerRef);
    if (!client.start()) {
      logger.logLoginStatusFailure(loggerRef);
      responseCallback.onFailure();
    }
  }

  private void setExpressLoginStatus(boolean isExpressLoginAllowed) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(EXPRESS_LOGIN_ALLOWED, isExpressLoginAllowed);
    editor.apply();
  }

  private boolean isExpressLoginAllowed() {
    return sharedPreferences.getBoolean(EXPRESS_LOGIN_ALLOWED, true);
  }

  private static void handleLoginStatusError(
      final String errorType,
      final String errorDescription,
      final String loggerRef,
      final LoginLogger logger,
      final LoginStatusCallback responseCallback) {
    final Exception exception = new FacebookException(errorType + ": " + errorDescription);
    logger.logLoginStatusError(loggerRef, exception);
    responseCallback.onError(exception);
  }

  private static class AndroidxActivityResultRegistryOwnerStartActivityDelegate
      implements StartActivityDelegate {
    private ActivityResultRegistryOwner activityResultRegistryOwner;
    private CallbackManager callbackManager;

    AndroidxActivityResultRegistryOwnerStartActivityDelegate(
        @NonNull ActivityResultRegistryOwner activityResultRegistryOwner,
        @NonNull CallbackManager callbackManager) {
      this.activityResultRegistryOwner = activityResultRegistryOwner;
      this.callbackManager = callbackManager;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
      class LauncherHolder {
        private ActivityResultLauncher<Intent> launcher = null;
      }
      final LauncherHolder launcherHolder = new LauncherHolder();
      launcherHolder.launcher =
          activityResultRegistryOwner
              .getActivityResultRegistry()
              .register(
                  "facebook-login",
                  new ActivityResultContract<Intent, Pair<Integer, Intent>>() {
                    @NonNull
                    @Override
                    public Intent createIntent(@NonNull Context context, Intent input) {
                      return input;
                    }

                    @Override
                    public Pair<Integer, Intent> parseResult(
                        int resultCode, @Nullable Intent intent) {
                      return Pair.create(resultCode, intent);
                    }
                  },
                  new ActivityResultCallback<Pair<Integer, Intent>>() {
                    @Override
                    public void onActivityResult(Pair<Integer, Intent> result) {
                      callbackManager.onActivityResult(
                          CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode(),
                          result.first,
                          result.second);
                      if (launcherHolder.launcher != null) {
                        launcherHolder.launcher.unregister();
                        launcherHolder.launcher = null;
                      }
                    }
                  });
      launcherHolder.launcher.launch(intent);
    }

    @Override
    public Activity getActivityContext() {
      if (activityResultRegistryOwner instanceof Activity) {
        return (Activity) activityResultRegistryOwner;
      } else {
        return null;
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
    private final FragmentWrapper fragment;

    FragmentStartActivityDelegate(final FragmentWrapper fragment) {
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

  private static class LoginLoggerHolder {
    private static LoginLogger logger;

    private static synchronized LoginLogger getLogger(Context context) {
      context = context != null ? context : FacebookSdk.getApplicationContext();
      if (context == null) {
        return null;
      }
      if (logger == null) {
        logger = new LoginLogger(context, FacebookSdk.getApplicationId());
      }
      return logger;
    }
  }
}
