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
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.FacebookException;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;

class KatanaProxyLoginMethodHandler extends LoginMethodHandler {

    KatanaProxyLoginMethodHandler(LoginClient loginClient) {
        super(loginClient);
    }

    @Override
    String getNameForLogging() {
        return "katana_proxy_auth";
    }

    @Override
    boolean tryAuthorize(LoginClient.Request request) {
        String e2e = LoginClient.getE2E();
        Intent intent = NativeProtocol.createProxyAuthIntent(
                loginClient.getActivity(),
                request.getApplicationId(),
                request.getPermissions(),
                e2e,
                request.isRerequest(),
                request.hasPublishPermission(),
                request.getDefaultAudience());

        addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e);

        return tryIntent(intent, LoginClient.getLoginRequestCode());
    }

    @Override
    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle stuff
        LoginClient.Result outcome;

        LoginClient.Request request = loginClient.getPendingRequest();

        if (data == null) {
            // This happens if the user presses 'Back'.
            outcome = LoginClient.Result.createCancelResult(request, "Operation canceled");
        } else if (resultCode == Activity.RESULT_CANCELED) {
            outcome = handleResultCancel(request, data);
        } else if (resultCode != Activity.RESULT_OK) {
            outcome = LoginClient.Result.createErrorResult(request,
                    "Unexpected resultCode from authorization.", null);
        } else {
            outcome = handleResultOk(request, data);
        }

        if (outcome != null) {
            loginClient.completeAndValidate(outcome);
        } else {
            loginClient.tryNextHandler();
        }
        return true;
    }

    private LoginClient.Result handleResultOk(LoginClient.Request request, Intent data) {
        Bundle extras = data.getExtras();
        String error = getError(extras);
        String errorCode = extras.getString("error_code");
        String errorMessage = getErrorMessage(extras);

        String e2e = extras.getString(NativeProtocol.FACEBOOK_PROXY_AUTH_E2E_KEY);
        if (!Utility.isNullOrEmpty(e2e)) {
            logWebLoginCompleted(e2e);
        }

        if (error == null && errorCode == null && errorMessage == null) {
            try {
                AccessToken token = createAccessTokenFromWebBundle(request.getPermissions(),
                        extras, AccessTokenSource.FACEBOOK_APPLICATION_WEB,
                        request.getApplicationId());
                return LoginClient.Result.createTokenResult(request, token);
            } catch (FacebookException ex) {
                return LoginClient.Result.createErrorResult(request, null, ex.getMessage());
            }
        } else if (ServerProtocol.errorsProxyAuthDisabled.contains(error)) {
            return null;
        } else if (ServerProtocol.errorsUserCanceled.contains(error)) {
            return LoginClient.Result.createCancelResult(request, null);
        } else {
            return LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode);
        }
    }

    private LoginClient.Result handleResultCancel(LoginClient.Request request, Intent data) {
        Bundle extras = data.getExtras();
        String error = getError(extras);
        String errorCode = extras.getString("error_code");

        // If the device has lost network, the result will be a cancel with a connection failure
        // error. We want our consumers to be notified of this as an error so they can tell their
        // users to "reconnect and try again".
        if (ServerProtocol.errorConnectionFailure.equals(errorCode)) {
            String errorMessage = getErrorMessage(extras);

            return LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode);
        }

        return LoginClient.Result.createCancelResult(request, error);
    }

    private String getError(Bundle extras) {
        String error = extras.getString("error");
        if (error == null) {
            error = extras.getString("error_type");
        }
        return error;
    }

    private String getErrorMessage(Bundle extras) {
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
            // We don't expect this to happen, since we've already validated the intent and bailed
            // out before now if it couldn't be resolved.
            return false;
        }

        return true;
    }

    KatanaProxyLoginMethodHandler(Parcel source) {
        super(source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static final Parcelable.Creator<KatanaProxyLoginMethodHandler> CREATOR =
            new Parcelable.Creator() {

                @Override
                public KatanaProxyLoginMethodHandler createFromParcel(Parcel source) {
                    return new KatanaProxyLoginMethodHandler(source);
                }

                @Override
                public KatanaProxyLoginMethodHandler[] newArray(int size) {
                    return new KatanaProxyLoginMethodHandler[size];
                }
            };
}
