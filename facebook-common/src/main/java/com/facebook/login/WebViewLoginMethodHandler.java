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

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.fragment.app.FragmentActivity;
import com.facebook.AccessTokenSource;
import com.facebook.FacebookException;
import com.facebook.internal.FacebookDialogFragment;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.WebDialog;

class WebViewLoginMethodHandler extends WebLoginMethodHandler {

  private WebDialog loginDialog;
  private String e2e;

  WebViewLoginMethodHandler(LoginClient loginClient) {
    super(loginClient);
  }

  @Override
  String getNameForLogging() {
    return "web_view";
  }

  @Override
  AccessTokenSource getTokenSource() {
    return AccessTokenSource.WEB_VIEW;
  }

  @Override
  boolean needsInternetPermission() {
    return true;
  }

  @Override
  void cancel() {
    if (loginDialog != null) {
      loginDialog.cancel();
      loginDialog = null;
    }
  }

  @Override
  int tryAuthorize(final LoginClient.Request request) {
    Bundle parameters = getParameters(request);

    WebDialog.OnCompleteListener listener =
        new WebDialog.OnCompleteListener() {
          @Override
          public void onComplete(Bundle values, FacebookException error) {
            onWebDialogComplete(request, values, error);
          }
        };

    e2e = LoginClient.getE2E();
    addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e);

    FragmentActivity fragmentActivity = loginClient.getActivity();
    final boolean isChromeOS = Utility.isChromeOS(fragmentActivity);

    WebDialog.Builder builder =
        new AuthDialogBuilder(fragmentActivity, request.getApplicationId(), parameters)
            .setE2E(e2e)
            .setIsChromeOS(isChromeOS)
            .setAuthType(request.getAuthType())
            .setLoginBehavior(request.getLoginBehavior())
            .setLoginTargetApp(request.getLoginTargetApp())
            .setFamilyLogin(request.isFamilyLogin())
            .setShouldSkipDedupe(request.shouldSkipAccountDeduplication())
            .setOnCompleteListener(listener);
    loginDialog = builder.build();

    FacebookDialogFragment dialogFragment = new FacebookDialogFragment();
    dialogFragment.setRetainInstance(true);
    dialogFragment.setInnerDialog(loginDialog);
    dialogFragment.show(fragmentActivity.getSupportFragmentManager(), FacebookDialogFragment.TAG);

    return 1;
  }

  void onWebDialogComplete(LoginClient.Request request, Bundle values, FacebookException error) {
    super.onComplete(request, values, error);
  }

  static class AuthDialogBuilder extends WebDialog.Builder {

    private static final String OAUTH_DIALOG = "oauth";
    private String e2e;
    private String authType;
    private String redirect_uri = ServerProtocol.DIALOG_REDIRECT_URI;
    private LoginBehavior loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK;
    private LoginTargetApp targetApp = LoginTargetApp.FACEBOOK;
    private boolean isFamilyLogin = false;
    private boolean shouldSkipDedupe = false;

    public AuthDialogBuilder(Context context, String applicationId, Bundle parameters) {
      super(context, applicationId, OAUTH_DIALOG, parameters);
    }

    public AuthDialogBuilder setE2E(String e2e) {
      this.e2e = e2e;
      return this;
    }

    /**
     * @deprecated This is no longer used
     * @return the AuthDialogBuilder
     */
    public AuthDialogBuilder setIsRerequest(boolean isRerequest) {
      return this;
    }

    public AuthDialogBuilder setIsChromeOS(final boolean isChromeOS) {
      redirect_uri =
          isChromeOS
              ? ServerProtocol.DIALOG_REDIRECT_CHROME_OS_URI
              : ServerProtocol.DIALOG_REDIRECT_URI;
      return this;
    }

    public AuthDialogBuilder setAuthType(final String authType) {
      this.authType = authType;
      return this;
    }

    public AuthDialogBuilder setLoginBehavior(final LoginBehavior loginBehavior) {
      this.loginBehavior = loginBehavior;
      return this;
    }

    public AuthDialogBuilder setLoginTargetApp(final LoginTargetApp targetApp) {
      this.targetApp = targetApp;
      return this;
    }

    public AuthDialogBuilder setFamilyLogin(final boolean isFamilyLogin) {
      this.isFamilyLogin = isFamilyLogin;
      return this;
    }

    public AuthDialogBuilder setShouldSkipDedupe(final boolean shouldSkip) {
      this.shouldSkipDedupe = shouldSkip;
      return this;
    }

    @Override
    public WebDialog build() {
      Bundle parameters = getParameters();
      parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, redirect_uri);
      parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, getApplicationId());
      parameters.putString(ServerProtocol.DIALOG_PARAM_E2E, e2e);
      parameters.putString(
          ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE,
          (targetApp == LoginTargetApp.INSTAGRAM)
              ? ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SCOPES
              : ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SIGNED_REQUEST);
      parameters.putString(
          ServerProtocol.DIALOG_PARAM_RETURN_SCOPES, ServerProtocol.DIALOG_RETURN_SCOPES_TRUE);
      parameters.putString(ServerProtocol.DIALOG_PARAM_AUTH_TYPE, authType);
      parameters.putString(ServerProtocol.DIALOG_PARAM_LOGIN_BEHAVIOR, loginBehavior.name());
      if (isFamilyLogin) {
        parameters.putString(ServerProtocol.DIALOG_PARAM_FX_APP, targetApp.toString());
      }
      if (shouldSkipDedupe) {
        parameters.putString(ServerProtocol.DIALOG_PARAM_SKIP_DEDUPE, "true");
      }

      return WebDialog.newInstance(
          getContext(), OAUTH_DIALOG, parameters, getTheme(), targetApp, getListener());
    }
  }

  WebViewLoginMethodHandler(Parcel source) {
    super(source);
    e2e = source.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(e2e);
  }

  public static final Parcelable.Creator<WebViewLoginMethodHandler> CREATOR =
      new Parcelable.Creator<WebViewLoginMethodHandler>() {

        @Override
        public WebViewLoginMethodHandler createFromParcel(Parcel source) {
          return new WebViewLoginMethodHandler(source);
        }

        @Override
        public WebViewLoginMethodHandler[] newArray(int size) {
          return new WebViewLoginMethodHandler[size];
        }
      };
}
