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

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;

import com.facebook.AccessTokenSource;
import com.facebook.FacebookException;
import com.facebook.internal.FacebookDialogFragment;
import com.facebook.internal.ServerProtocol;
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
    boolean tryAuthorize(final LoginClient.Request request) {
        Bundle parameters = getParameters(request);

        WebDialog.OnCompleteListener listener = new WebDialog.OnCompleteListener() {
            @Override
            public void onComplete(Bundle values, FacebookException error) {
                onWebDialogComplete(request, values, error);
            }
        };

        e2e = LoginClient.getE2E();
        addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e);

        FragmentActivity fragmentActivity = loginClient.getActivity();
        WebDialog.Builder builder = new AuthDialogBuilder(
                fragmentActivity,
                request.getApplicationId(),
                parameters)
                .setE2E(e2e)
                .setIsRerequest(request.isRerequest())
                .setOnCompleteListener(listener);
        loginDialog = builder.build();

        FacebookDialogFragment dialogFragment = new FacebookDialogFragment();
        dialogFragment.setRetainInstance(true);
        dialogFragment.setDialog(loginDialog);
        dialogFragment.show(fragmentActivity.getSupportFragmentManager(),
                FacebookDialogFragment.TAG);

        return true;
    }

    void onWebDialogComplete(LoginClient.Request request, Bundle values,
            FacebookException error) {
        super.onComplete(request, values, error);
    }

    static class AuthDialogBuilder extends WebDialog.Builder {
        private static final String OAUTH_DIALOG = "oauth";
        static final String REDIRECT_URI = "fbconnect://success";
        private String e2e;
        private boolean isRerequest;

        public AuthDialogBuilder(Context context, String applicationId, Bundle parameters) {
            super(context, applicationId, OAUTH_DIALOG, parameters);
        }

        public AuthDialogBuilder setE2E(String e2e) {
            this.e2e = e2e;
            return this;
        }

        public AuthDialogBuilder setIsRerequest(boolean isRerequest) {
            this.isRerequest = isRerequest;
            return this;
        }

        @Override
        public WebDialog build() {
            Bundle parameters = getParameters();
            parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, REDIRECT_URI);
            parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, getApplicationId());
            parameters.putString(ServerProtocol.DIALOG_PARAM_E2E, e2e);
            parameters.putString(
                    ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE,
                    ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SIGNED_REQUEST);
            parameters.putString(
                    ServerProtocol.DIALOG_PARAM_RETURN_SCOPES,
                    ServerProtocol.DIALOG_RETURN_SCOPES_TRUE);
            parameters.putString(
                        ServerProtocol.DIALOG_PARAM_AUTH_TYPE,
                        ServerProtocol.DIALOG_REREQUEST_AUTH_TYPE);

            return new WebDialog(getContext(), OAUTH_DIALOG, parameters, getTheme(), getListener());
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
            new Parcelable.Creator() {

                @Override
                public WebViewLoginMethodHandler createFromParcel(Parcel source) {
                    return new WebViewLoginMethodHandler(source);
                }

                @Override
                public WebViewLoginMethodHandler[] newArray(int size) {
                    return new WebViewLoginMethodHandler[size];
                }
            };}
