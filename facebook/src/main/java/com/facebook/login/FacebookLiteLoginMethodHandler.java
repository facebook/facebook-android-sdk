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
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;


class FacebookLiteLoginMethodHandler extends NativeAppLoginMethodHandler {

    FacebookLiteLoginMethodHandler(LoginClient loginClient) {
        super(loginClient);
    }

    @Override
    String getNameForLogging() {
        return "fb_lite_login";
    }

    @Override
    boolean tryAuthorize(LoginClient.Request request) {
        String e2e = LoginClient.getE2E();
        Intent intent = NativeProtocol.createFacebookLiteIntent(
                loginClient.getActivity(),
                request.getApplicationId(),
                request.getPermissions(),
                e2e,
                request.isRerequest(),
                request.hasPublishPermission(),
                request.getDefaultAudience(),
                getClientState(request.getAuthId()));

        addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e);

        return tryIntent(intent, LoginClient.getLoginRequestCode());
    }

    FacebookLiteLoginMethodHandler(Parcel source) {
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

    public static final Parcelable.Creator<FacebookLiteLoginMethodHandler> CREATOR =
            new Parcelable.Creator() {

                @Override
                public FacebookLiteLoginMethodHandler createFromParcel(Parcel source) {
                    return new FacebookLiteLoginMethodHandler(source);
                }

                @Override
                public FacebookLiteLoginMethodHandler[] newArray(int size) {
                    return new FacebookLiteLoginMethodHandler[size];
                }
            };
}
