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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.FacebookException;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class GetTokenLoginMethodHandler extends LoginMethodHandler {
    private GetTokenClient getTokenClient;

    GetTokenLoginMethodHandler(LoginClient loginClient) {
        super(loginClient);
    }

    @Override
    String getNameForLogging() {
        return "get_token";
    }

    @Override
    void cancel() {
        if (getTokenClient != null) {
            getTokenClient.cancel();
            getTokenClient.setCompletedListener(null);
            getTokenClient = null;
        }
    }

    boolean tryAuthorize(final LoginClient.Request request) {
        getTokenClient = new GetTokenClient(loginClient.getActivity(),
            request.getApplicationId());
        if (!getTokenClient.start()) {
            return false;
        }

        loginClient.notifyBackgroundProcessingStart();

        GetTokenClient.CompletedListener callback = new GetTokenClient.CompletedListener() {
            @Override
            public void completed(Bundle result) {
                getTokenCompleted(request, result);
            }
        };

        getTokenClient.setCompletedListener(callback);
        return true;
    }

    void getTokenCompleted(LoginClient.Request request, Bundle result) {
        if (getTokenClient != null) {
            getTokenClient.setCompletedListener(null);
        }
        getTokenClient = null;

        loginClient.notifyBackgroundProcessingStop();

        if (result != null) {
            ArrayList<String> currentPermissions =
                    result.getStringArrayList(NativeProtocol.EXTRA_PERMISSIONS);
            Set<String> permissions = request.getPermissions();
            if ((currentPermissions != null) &&
                    ((permissions == null) || currentPermissions.containsAll(permissions))) {
                // We got all the permissions we needed, so we can complete the auth now.
                complete(request, result);
                return;
            }

            // We didn't get all the permissions we wanted, so update the request with just the
            // permissions we still need.
            Set<String> newPermissions = new HashSet<String>();
            for (String permission : permissions) {
                if (!currentPermissions.contains(permission)) {
                    newPermissions.add(permission);
                }
            }
            if (!newPermissions.isEmpty()) {
                addLoggingExtra(
                    LoginLogger.EVENT_EXTRAS_NEW_PERMISSIONS,
                    TextUtils.join(",", newPermissions)
                );
            }

            request.setPermissions(newPermissions);
        }

        loginClient.tryNextHandler();
    }

    void onComplete(final LoginClient.Request request, final Bundle result) {
        AccessToken token = createAccessTokenFromNativeLogin(
                result,
                AccessTokenSource.FACEBOOK_APPLICATION_SERVICE,
                request.getApplicationId());
        LoginClient.Result outcome =
                LoginClient.Result.createTokenResult(loginClient.getPendingRequest(), token);
        loginClient.completeAndValidate(outcome);
    }

    // Workaround for old facebook apps that don't return the userid.
    void complete(final LoginClient.Request request, final Bundle result) {
        String userId = result.getString(NativeProtocol.EXTRA_USER_ID);
        // If the result is missing the UserId request it
        if (userId == null || userId.isEmpty()) {
            loginClient.notifyBackgroundProcessingStart();

            String accessToken = result.getString(NativeProtocol.EXTRA_ACCESS_TOKEN);
            Utility.getGraphMeRequestWithCacheAsync(
                    accessToken,
                    new Utility.GraphMeRequestWithCacheCallback() {
                        @Override
                        public void onSuccess(JSONObject userInfo) {
                            try {
                                String userId = userInfo.getString("id");
                                result.putString(NativeProtocol.EXTRA_USER_ID, userId);
                                onComplete(request, result);
                            } catch (JSONException ex) {
                                loginClient.complete(LoginClient.Result.createErrorResult(
                                        loginClient.getPendingRequest(),
                                        "Caught exception",
                                        ex.getMessage()));
                            }
                        }

                        @Override
                        public void onFailure(FacebookException error) {
                            loginClient.complete(LoginClient.Result.createErrorResult(
                                    loginClient.getPendingRequest(),
                                    "Caught exception",
                                    error.getMessage()));
                        }
                    });
        } else {
            onComplete(request, result);
        }

    }

    GetTokenLoginMethodHandler(Parcel source) {
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

    public static final Parcelable.Creator<GetTokenLoginMethodHandler> CREATOR =
            new Parcelable.Creator() {

        @Override
        public GetTokenLoginMethodHandler createFromParcel(Parcel source) {
            return new GetTokenLoginMethodHandler(source);
        }

        @Override
        public GetTokenLoginMethodHandler[] newArray(int size) {
            return new GetTokenLoginMethodHandler[size];
        }
    };
}
