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
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.Nullable;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.AuthenticationToken;
import com.facebook.FacebookException;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;

abstract class NativeAppLoginMethodHandler extends LoginMethodHandler {

  NativeAppLoginMethodHandler(LoginClient loginClient) {
    super(loginClient);
  }

  NativeAppLoginMethodHandler(Parcel source) {
    super(source);
  }

  abstract int tryAuthorize(LoginClient.Request request);

  public AccessTokenSource getTokenSource() {
    return AccessTokenSource.FACEBOOK_APPLICATION_WEB;
  }

  @Override
  boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    LoginClient.Request request = loginClient.getPendingRequest();

    if (data == null) {
      // This happens if the user presses 'Back'.
      completeLogin(LoginClient.Result.createCancelResult(request, "Operation canceled"));
    } else if (resultCode == Activity.RESULT_CANCELED) {
      handleResultCancel(request, data);
    } else if (resultCode != Activity.RESULT_OK) {
      completeLogin(
          LoginClient.Result.createErrorResult(
              request, "Unexpected resultCode from authorization.", null));
    } else {
      Bundle extras = data.getExtras();
      if (extras == null) {
        completeLogin(
            LoginClient.Result.createErrorResult(
                request, "Unexpected null from returned authorization data.", null));
        ;
        return true;
      }
      String error = getError(extras);
      String errorCode =
          extras.get("error_code") != null ? extras.get("error_code").toString() : null;
      String errorMessage = getErrorMessage(extras);

      String e2e = extras.getString(NativeProtocol.FACEBOOK_PROXY_AUTH_E2E_KEY);
      if (!Utility.isNullOrEmpty(e2e)) {
        logWebLoginCompleted(e2e);
      }
      if (error == null && errorCode == null && errorMessage == null) {
        handleResultOk(request, extras);
      } else {
        handleResultError(request, error, errorMessage, errorCode);
      }
    }
    return true;
  }

  private void completeLogin(@Nullable LoginClient.Result outcome) {
    if (outcome != null) {
      loginClient.completeAndValidate(outcome);
    } else {
      loginClient.tryNextHandler();
    }
  }

  protected void handleResultError(
      LoginClient.Request request,
      @Nullable String error,
      @Nullable String errorMessage,
      @Nullable String errorCode) {
    if (error != null && error.equals("logged_out")) {
      CustomTabLoginMethodHandler.calledThroughLoggedOutAppSwitch = true;
      completeLogin(null);
    } else if (ServerProtocol.getErrorsProxyAuthDisabled().contains(error)) {
      completeLogin(null);
    } else if (ServerProtocol.getErrorsUserCanceled().contains(error)) {
      completeLogin(LoginClient.Result.createCancelResult(request, null));
    } else {
      completeLogin(LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode));
    }
  }

  protected void handleResultOk(LoginClient.Request request, Bundle extras) {
    try {
      AccessToken token =
          createAccessTokenFromWebBundle(
              request.getPermissions(), extras, getTokenSource(), request.getApplicationId());
      AuthenticationToken authenticationToken = createAuthenticationTokenFromWebBundle(extras);
      completeLogin(
          LoginClient.Result.createCompositeTokenResult(request, token, authenticationToken));
    } catch (FacebookException ex) {
      completeLogin(LoginClient.Result.createErrorResult(request, null, ex.getMessage()));
    }
  }

  protected void handleResultCancel(LoginClient.Request request, Intent data) {
    Bundle extras = data.getExtras();
    String error = getError(extras);
    String errorCode =
        extras.get("error_code") != null ? extras.get("error_code").toString() : null;

    // If the device has lost network, the result will be a cancel with a connection failure
    // error. We want our consumers to be notified of this as an error so they can tell their
    // users to "reconnect and try again".
    if (ServerProtocol.getErrorConnectionFailure().equals(errorCode)) {
      String errorMessage = getErrorMessage(extras);
      completeLogin(LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode));
    }

    completeLogin(LoginClient.Result.createCancelResult(request, error));
  }

  protected @Nullable String getError(@Nullable Bundle extras) {
    if (extras == null) {
      return null;
    }
    String error = extras.getString("error");
    if (error == null) {
      error = extras.getString("error_type");
    }
    return error;
  }

  protected @Nullable String getErrorMessage(@Nullable Bundle extras) {
    if (extras == null) {
      return null;
    }
    String errorMessage = extras.getString("error_message");
    if (errorMessage == null) {
      errorMessage = extras.getString("error_description");
    }
    return errorMessage;
  }

  protected boolean tryIntent(Intent intent, int requestCode) {
    if (intent == null) {
      return false;
    }

    try {
      loginClient.getFragment().startActivityForResult(intent, requestCode);
    } catch (ActivityNotFoundException e) {
      // We do not know if we have the activity until we try starting it.
      // FB is not installed if ActivityNotFoundException is thrown and this might fallback
      // to other handlers
      return false;
    }

    return true;
  }
}
