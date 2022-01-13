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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.facebook.AccessToken;
import com.facebook.AuthenticationToken;
import com.facebook.CustomTabMainActivity;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.common.R;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class LoginClient implements Parcelable {
  LoginMethodHandler[] handlersToTry;
  int currentHandler = -1;
  Fragment fragment;
  OnCompletedListener onCompletedListener;
  BackgroundProcessingListener backgroundProcessingListener;
  boolean checkedInternetPermission;
  Request pendingRequest;
  Map<String, String> loggingExtras;
  Map<String, String> extraData;
  private LoginLogger loginLogger;
  private int numActivitiesReturned = 0;
  private int numTotalIntentsFired = 0;

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

    if (AccessToken.isCurrentAccessTokenActive() && !checkInternetPermission()) {
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

  LoginMethodHandler getCurrentHandler() {
    if (currentHandler >= 0) {
      return handlersToTry[currentHandler];
    } else {
      return null;
    }
  }

  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    numActivitiesReturned++;
    if (pendingRequest != null) {

      if (data != null) {
        // If CustomTabs throws ActivityNotFoundException, then we would eventually return here.
        // In that case, we should treat that as tryAuthorize and try the next handler
        boolean hasNoBrowserException =
            data.getBooleanExtra(CustomTabMainActivity.NO_ACTIVITY_EXCEPTION, false);
        if (hasNoBrowserException) {
          tryNextHandler();
          return false;
        }
      }

      if (!getCurrentHandler().shouldKeepTrackOfMultipleIntents()
          || data != null
          || numActivitiesReturned >= numTotalIntentsFired) {
        return getCurrentHandler().onActivityResult(requestCode, resultCode, data);
      }
    }

    return false;
  }

  protected LoginMethodHandler[] getHandlersToTry(Request request) {
    ArrayList<LoginMethodHandler> handlers = new ArrayList<LoginMethodHandler>();

    final LoginBehavior behavior = request.getLoginBehavior();

    if (request.isInstagramLogin()) {
      if (!FacebookSdk.bypassAppSwitch && behavior.allowsInstagramAppAuth()) {
        handlers.add(new InstagramAppLoginMethodHandler(this));
      }
    } else {
      // Only use get token auth and native FB4A auth for FB logins
      if (behavior.allowsGetTokenAuth()) {
        handlers.add(new GetTokenLoginMethodHandler(this));
      }
      if (!FacebookSdk.bypassAppSwitch && behavior.allowsKatanaAuth()) {
        handlers.add(new KatanaProxyLoginMethodHandler(this));
      }

      if (!FacebookSdk.bypassAppSwitch && behavior.allowsFacebookLiteAuth()) {
        handlers.add(new FacebookLiteLoginMethodHandler(this));
      }
    }

    if (behavior.allowsCustomTabAuth()) {
      handlers.add(new CustomTabLoginMethodHandler(this));
    }

    if (behavior.allowsWebViewAuth()) {
      handlers.add(new WebViewLoginMethodHandler(this));
    }

    if (!request.isInstagramLogin() && behavior.allowsDeviceAuth()) {
      handlers.add(new DeviceAuthMethodHandler(this));
    }

    LoginMethodHandler[] result = new LoginMethodHandler[handlers.size()];
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
      String errorDescription =
          activity.getString(R.string.com_facebook_internet_permission_error_message);
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
          getCurrentHandler().getMethodLoggingExtras());
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

  void addExtraData(String key, String value, boolean accumulate) {
    if (extraData == null) {
      extraData = new HashMap<String, String>();
    }
    if (extraData.containsKey(key) && accumulate) {
      value = extraData.get(key) + "," + value;
    }
    extraData.put(key, value);
  }

  boolean tryCurrentHandler() {
    LoginMethodHandler handler = getCurrentHandler();
    if (handler.needsInternetPermission() && !checkInternetPermission()) {
      addLoggingExtra(
          LoginLogger.EVENT_EXTRAS_MISSING_INTERNET_PERMISSION,
          AppEventsConstants.EVENT_PARAM_VALUE_YES,
          false);
      return false;
    }

    int numTried = handler.tryAuthorize(pendingRequest);
    numActivitiesReturned = 0;
    if (numTried > 0) {
      getLogger()
          .logAuthorizationMethodStart(
              pendingRequest.getAuthId(),
              handler.getNameForLogging(),
              pendingRequest.isFamilyLogin()
                  ? LoginLogger.EVENT_NAME_FOA_LOGIN_METHOD_START
                  : LoginLogger.EVENT_NAME_LOGIN_METHOD_START);
      numTotalIntentsFired = numTried;
    } else {
      // We didn't try it, so we don't get any other completion
      // notification -- log that we skipped it.
      getLogger()
          .logAuthorizationMethodNotTried(
              pendingRequest.getAuthId(),
              handler.getNameForLogging(),
              pendingRequest.isFamilyLogin()
                  ? LoginLogger.EVENT_NAME_FOA_LOGIN_METHOD_NOT_TRIED
                  : LoginLogger.EVENT_NAME_LOGIN_METHOD_NOT_TRIED);
      addLoggingExtra(LoginLogger.EVENT_EXTRAS_NOT_TRIED, handler.getNameForLogging(), true);
    }

    return numTried > 0;
  }

  void completeAndValidate(Result outcome) {
    // Do we need to validate a successful result (as in the case of a reauth)?
    if (outcome.token != null && AccessToken.isCurrentAccessTokenActive()) {
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
      logAuthorizationMethodComplete(
          handler.getNameForLogging(), outcome, handler.getMethodLoggingExtras());
    }

    if (loggingExtras != null) {
      // Pass this back to the caller for logging at the aggregate level.
      outcome.loggingExtras = loggingExtras;
    }
    if (extraData != null) {
      outcome.extraData = extraData;
    }

    handlersToTry = null;
    currentHandler = -1;
    pendingRequest = null;
    loggingExtras = null;
    numActivitiesReturned = 0;
    numTotalIntentsFired = 0;

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
      Result result;
      if (previousToken != null
          && newToken != null
          && previousToken.getUserId().equals(newToken.getUserId())) {
        result =
            Result.createCompositeTokenResult(
                pendingRequest, pendingResult.token, pendingResult.authenticationToken);
      } else {
        result =
            Result.createErrorResult(
                pendingRequest, "User logged in as different Facebook user.", null);
      }
      complete(result);
    } catch (Exception ex) {
      complete(Result.createErrorResult(pendingRequest, "Caught exception", ex.getMessage()));
    }
  }

  private LoginLogger getLogger() {
    if (loginLogger == null
        || !loginLogger.getApplicationId().equals(pendingRequest.getApplicationId())) {

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
      String method, Result result, Map<String, String> loggingExtras) {
    logAuthorizationMethodComplete(
        method,
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
      getLogger()
          .logUnexpectedError(
              LoginLogger.EVENT_NAME_LOGIN_METHOD_COMPLETE,
              "Unexpected call to logCompleteLogin with null pendingAuthorizationRequest.",
              method);
    } else {
      getLogger()
          .logAuthorizationMethodComplete(
              pendingRequest.getAuthId(),
              method,
              result,
              errorMessage,
              errorCode,
              loggingExtras,
              pendingRequest.isFamilyLogin()
                  ? LoginLogger.EVENT_NAME_FOA_LOGIN_METHOD_COMPLETE
                  : LoginLogger.EVENT_NAME_LOGIN_METHOD_COMPLETE);
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
    private String authId;
    private boolean isRerequest = false;
    private String deviceRedirectUriString;
    private String authType;
    private String deviceAuthTargetUserId; // used to target a specific user with device login
    @Nullable private String messengerPageId;
    private boolean resetMessengerState;
    private final LoginTargetApp targetApp;
    private boolean isFamilyLogin = false;
    private boolean shouldSkipAccountDeduplication = false;
    private String nonce;

    Request(
        LoginBehavior loginBehavior,
        Set<String> permissions,
        DefaultAudience defaultAudience,
        String authType,
        String applicationId,
        String authId) {
      this(
          loginBehavior,
          permissions,
          defaultAudience,
          authType,
          applicationId,
          authId,
          LoginTargetApp.FACEBOOK);
    }

    Request(
        LoginBehavior loginBehavior,
        Set<String> permissions,
        DefaultAudience defaultAudience,
        String authType,
        String applicationId,
        String authId,
        LoginTargetApp targetApp) {
      this(
          loginBehavior,
          permissions,
          defaultAudience,
          authType,
          applicationId,
          authId,
          targetApp,
          null);
    }

    Request(
        LoginBehavior loginBehavior,
        Set<String> permissions,
        DefaultAudience defaultAudience,
        String authType,
        String applicationId,
        String authId,
        LoginTargetApp targetApp,
        String nonce) {
      this.loginBehavior = loginBehavior;
      this.permissions = permissions != null ? permissions : new HashSet<String>();
      this.defaultAudience = defaultAudience;
      this.authType = authType;
      this.applicationId = applicationId;
      this.authId = authId;
      this.targetApp = targetApp;
      if (Utility.isNullOrEmpty(nonce)) {
        this.nonce = UUID.randomUUID().toString();
      } else {
        this.nonce = nonce;
      }
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

    LoginTargetApp getLoginTargetApp() {
      return targetApp;
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

    void setAuthId(String authId) {
      this.authId = authId;
    }

    boolean isRerequest() {
      return isRerequest;
    }

    void setRerequest(boolean isRerequest) {
      this.isRerequest = isRerequest;
    }

    boolean isFamilyLogin() {
      return isFamilyLogin;
    }

    void setFamilyLogin(boolean isFamilyLogin) {
      this.isFamilyLogin = isFamilyLogin;
    }

    boolean shouldSkipAccountDeduplication() {
      return shouldSkipAccountDeduplication;
    }

    void setShouldSkipAccountDeduplication(boolean shouldSkipAccountDeduplication) {
      this.shouldSkipAccountDeduplication = shouldSkipAccountDeduplication;
    }

    String getDeviceRedirectUriString() {
      return this.deviceRedirectUriString;
    }

    void setDeviceRedirectUriString(String deviceRedirectUriString) {
      this.deviceRedirectUriString = deviceRedirectUriString;
    }

    String getDeviceAuthTargetUserId() {
      return this.deviceAuthTargetUserId;
    }

    void setDeviceAuthTargetUserId(String deviceAuthTargetUserId) {
      this.deviceAuthTargetUserId = deviceAuthTargetUserId;
    }

    String getAuthType() {
      return authType;
    }

    void setAuthType(final String authType) {
      this.authType = authType;
    }

    public @Nullable String getMessengerPageId() {
      return messengerPageId;
    }

    public void setMessengerPageId(@Nullable final String pageId) {
      this.messengerPageId = pageId;
    }

    public boolean getResetMessengerState() {
      return resetMessengerState;
    }

    public void setResetMessengerState(final boolean resetMessengerState) {
      this.resetMessengerState = resetMessengerState;
    }

    boolean hasPublishPermission() {
      for (String permission : permissions) {
        if (LoginManager.isPublishPermission(permission)) {
          return true;
        }
      }
      return false;
    }

    boolean isInstagramLogin() {
      return targetApp == LoginTargetApp.INSTAGRAM;
    }

    public String getNonce() {
      return nonce;
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
      this.isRerequest = parcel.readByte() != 0;
      this.deviceRedirectUriString = parcel.readString();
      this.authType = parcel.readString();
      this.deviceAuthTargetUserId = parcel.readString();
      this.messengerPageId = parcel.readString();
      this.resetMessengerState = parcel.readByte() != 0;
      enumValue = parcel.readString();
      this.targetApp = enumValue != null ? LoginTargetApp.valueOf(enumValue) : null;
      this.isFamilyLogin = parcel.readByte() != 0;
      this.shouldSkipAccountDeduplication = parcel.readByte() != 0;
      this.nonce = parcel.readString();
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
      dest.writeByte((byte) (isRerequest ? 1 : 0));
      dest.writeString(deviceRedirectUriString);
      dest.writeString(authType);
      dest.writeString(deviceAuthTargetUserId);
      dest.writeString(messengerPageId);
      dest.writeByte((byte) (resetMessengerState ? 1 : 0));
      dest.writeString(targetApp != null ? targetApp.name() : null);
      dest.writeByte((byte) (isFamilyLogin ? 1 : 0));
      dest.writeByte((byte) (shouldSkipAccountDeduplication ? 1 : 0));
      dest.writeString(nonce);
    }

    public static final Parcelable.Creator<Request> CREATOR =
        new Parcelable.Creator<Request>() {
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
    @Nullable final AccessToken token;
    @Nullable final AuthenticationToken authenticationToken;
    @Nullable final String errorMessage;
    @Nullable final String errorCode;
    final Request request;
    public Map<String, String> loggingExtras;
    public Map<String, String> extraData;

    Result(
        Request request,
        Code code,
        @Nullable AccessToken token,
        @Nullable String errorMessage,
        @Nullable String errorCode) {
      this(request, code, token, null, errorMessage, errorCode);
    }

    Result(
        Request request,
        Code code,
        @Nullable AccessToken accessToken,
        @Nullable AuthenticationToken authenticationToken,
        @Nullable String errorMessage,
        @Nullable String errorCode) {
      Validate.notNull(code, "code");
      this.request = request;
      this.token = accessToken;
      this.authenticationToken = authenticationToken;
      this.errorMessage = errorMessage;
      this.code = code;
      this.errorCode = errorCode;
    }

    static Result createTokenResult(Request request, AccessToken token) {
      return new Result(request, Code.SUCCESS, token, null, null);
    }

    static Result createCompositeTokenResult(
        Request request, AccessToken accessToken, AuthenticationToken authenticationToken) {
      return new Result(request, Code.SUCCESS, accessToken, authenticationToken, null, null);
    }

    static Result createCancelResult(Request request, @Nullable String message) {
      return new Result(request, Code.CANCEL, null, message, null);
    }

    static Result createErrorResult(
        Request request, @Nullable String errorType, @Nullable String errorDescription) {
      return createErrorResult(request, errorType, errorDescription, null);
    }

    static Result createErrorResult(
        Request request,
        @Nullable String errorType,
        @Nullable String errorDescription,
        @Nullable String errorCode) {
      String message = TextUtils.join(": ", Utility.asListNoNulls(errorType, errorDescription));
      return new Result(request, Code.ERROR, null, message, errorCode);
    }

    private Result(Parcel parcel) {
      this.code = Code.valueOf(parcel.readString());
      this.token = parcel.readParcelable(AccessToken.class.getClassLoader());
      this.authenticationToken = parcel.readParcelable(AuthenticationToken.class.getClassLoader());
      this.errorMessage = parcel.readString();
      this.errorCode = parcel.readString();
      this.request = parcel.readParcelable(Request.class.getClassLoader());
      this.loggingExtras = Utility.readStringMapFromParcel(parcel);
      this.extraData = Utility.readStringMapFromParcel(parcel);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(code.name());
      dest.writeParcelable(token, flags);
      dest.writeParcelable(authenticationToken, flags);
      dest.writeString(errorMessage);
      dest.writeString(errorCode);
      dest.writeParcelable(request, flags);
      Utility.writeStringMapToParcel(dest, loggingExtras);
      Utility.writeStringMapToParcel(dest, extraData);
    }

    public static final Parcelable.Creator<Result> CREATOR =
        new Parcelable.Creator<Result>() {
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

  // Parcelable implementation

  public LoginClient(Parcel source) {
    Object[] o = source.readParcelableArray(LoginMethodHandler.class.getClassLoader());
    handlersToTry = new LoginMethodHandler[o.length];
    for (int i = 0; i < o.length; ++i) {
      handlersToTry[i] = (LoginMethodHandler) o[i];
      handlersToTry[i].setLoginClient(this);
    }
    currentHandler = source.readInt();
    pendingRequest = source.readParcelable(Request.class.getClassLoader());
    loggingExtras = Utility.readStringMapFromParcel(source);
    extraData = Utility.readStringMapFromParcel(source);
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
    Utility.writeStringMapToParcel(dest, extraData);
  }

  public static final Parcelable.Creator<LoginClient> CREATOR =
      new Parcelable.Creator<LoginClient>() {
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
