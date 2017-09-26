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

import android.os.Parcel;
import android.os.Parcelable;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class DeviceAuthMethodHandler extends LoginMethodHandler {
    private static ScheduledThreadPoolExecutor backgroundExecutor;

    DeviceAuthMethodHandler(LoginClient loginClient) {
        super(loginClient);
    }

    @Override
    boolean tryAuthorize(LoginClient.Request request) {
        showDialog(request);
        return true;
    }

    private void showDialog(final LoginClient.Request request) {
        DeviceAuthDialog dialog = new DeviceAuthDialog();
        dialog.show(
                loginClient.getActivity().getSupportFragmentManager(),
                "login_with_facebook");
        dialog.startLogin(request);
    }

    public void onCancel() {
        LoginClient.Result outcome = LoginClient.Result.createCancelResult(
                loginClient.getPendingRequest(),
                "User canceled log in.");
        loginClient.completeAndValidate(outcome);
    }

    public void onError(Exception ex) {
        LoginClient.Result outcome = LoginClient.Result.createErrorResult(
                loginClient.getPendingRequest(),
                null,
                ex.getMessage());
        loginClient.completeAndValidate(outcome);
    }

    public void onSuccess(
            String accessToken,
            String applicationId,
            String userId,
            Collection<String> permissions,
            Collection<String> declinedPermissions,
            AccessTokenSource accessTokenSource,
            Date expirationTime,
            Date lastRefreshTime) {
        AccessToken token = new AccessToken(
                accessToken,
                applicationId,
                userId,
                permissions,
                declinedPermissions,
                accessTokenSource,
                expirationTime,
                lastRefreshTime);

        LoginClient.Result outcome = LoginClient.Result.createTokenResult(
                loginClient.getPendingRequest(),
                token);
        loginClient.completeAndValidate(outcome);
    }

    public static synchronized ScheduledThreadPoolExecutor getBackgroundExecutor() {
        if (backgroundExecutor == null) {
            backgroundExecutor = new ScheduledThreadPoolExecutor(1);
        }

        return backgroundExecutor;
    }

    protected DeviceAuthMethodHandler(Parcel parcel) {
        super(parcel);
    }

    @Override
    String getNameForLogging() {
        return "device_auth";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static final Parcelable.Creator<DeviceAuthMethodHandler> CREATOR =
            new Parcelable.Creator() {

                @Override
                public DeviceAuthMethodHandler createFromParcel(Parcel source) {
                    return new DeviceAuthMethodHandler(source);
                }

                @Override
                public DeviceAuthMethodHandler[] newArray(int size) {
                    return new DeviceAuthMethodHandler[size];
                }
            };
}
