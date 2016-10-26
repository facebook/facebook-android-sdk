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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.facebook.AccessTokenSource;
import com.facebook.CustomTabMainActivity;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.FacebookServiceException;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomTabLoginMethodHandler extends WebLoginMethodHandler {
    private static final int CUSTOM_TAB_REQUEST_CODE = 1;
    private static final int CHALLENGE_LENGTH = 20;
    private static final int API_EC_DIALOG_CANCEL = 4201;
    private static final String CUSTOM_TABS_SERVICE_ACTION =
            "android.support.customtabs.action.CustomTabsService";
    private static final String[] CHROME_PACKAGES = {
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
    };

    private String currentPackage;
    private String expectedChallenge;

    CustomTabLoginMethodHandler(LoginClient loginClient) {
        super(loginClient);
        expectedChallenge = Utility.generateRandomString(CHALLENGE_LENGTH);
    }

    @Override
    String getNameForLogging() {
        return "custom_tab";
    }

    @Override
    AccessTokenSource getTokenSource() {
        return AccessTokenSource.CHROME_CUSTOM_TAB;
    }

    @Override
    protected String getSSODevice() {
        return "chrome_custom_tab";
    }

    @Override
    boolean tryAuthorize(final LoginClient.Request request) {
        if (!isCustomTabsAllowed()) {
            return false;
        }

        Bundle parameters = getParameters(request);
        parameters = addExtraParameters(parameters, request);
        Activity activity = loginClient.getActivity();

        Intent intent = new Intent(activity, CustomTabMainActivity.class);
        intent.putExtra(CustomTabMainActivity.EXTRA_PARAMS, parameters);
        intent.putExtra(CustomTabMainActivity.EXTRA_CHROME_PACKAGE, getChromePackage());
        loginClient.getFragment().startActivityForResult(intent, CUSTOM_TAB_REQUEST_CODE);

        return true;
    }

    private boolean isCustomTabsAllowed() {
        return isCustomTabsEnabled()
                && getChromePackage() != null
                && Validate.hasCustomTabRedirectActivity(FacebookSdk.getApplicationContext());
    }

    private boolean isCustomTabsEnabled() {
        final String appId = Utility.getMetadataApplicationId(loginClient.getActivity());
        final FetchedAppSettings settings = FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId);
        return settings != null && settings.getCustomTabsEnabled();
    }

    private String getChromePackage() {
        if (currentPackage != null) {
            return currentPackage;
        }
        Context context = loginClient.getActivity();
        Intent serviceIntent = new Intent(CUSTOM_TABS_SERVICE_ACTION);
        List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentServices(serviceIntent, 0);
        if (resolveInfos != null) {
            Set<String> chromePackages = new HashSet<>(Arrays.asList(CHROME_PACKAGES));
            for (ResolveInfo resolveInfo : resolveInfos) {
                ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                if (serviceInfo != null && chromePackages.contains(serviceInfo.packageName)) {
                    currentPackage = serviceInfo.packageName;
                    return currentPackage;
                }
            }
        }
        return null;
    }

    @Override
    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CUSTOM_TAB_REQUEST_CODE) {
            return super.onActivityResult(requestCode, resultCode, data);
        }
        LoginClient.Request request = loginClient.getPendingRequest();
        if (resultCode == Activity.RESULT_OK) {
            onCustomTabComplete(data.getStringExtra(CustomTabMainActivity.EXTRA_URL), request);
            return true;
        }
        super.onComplete(request, null, new FacebookOperationCanceledException());
        return false;
    }

    private void onCustomTabComplete(String url, LoginClient.Request request) {
        if (url != null && url.startsWith(CustomTabMainActivity.getRedirectUrl())) {
            Uri uri = Uri.parse(url);
            Bundle values = Utility.parseUrlQueryString(uri.getQuery());
            values.putAll(Utility.parseUrlQueryString(uri.getFragment()));

            if (!validateChallengeParam(values)) {
                super.onComplete(request, null, new FacebookException("Invalid state parameter"));
                return;
            }

            String error = values.getString("error");
            if (error == null) {
                error = values.getString("error_type");
            }

            String errorMessage = values.getString("error_msg");
            if (errorMessage == null) {
                errorMessage = values.getString("error_message");
            }
            if (errorMessage == null) {
                errorMessage = values.getString("error_description");
            }
            String errorCodeString = values.getString("error_code");
            int errorCode = FacebookRequestError.INVALID_ERROR_CODE;
            if (!Utility.isNullOrEmpty(errorCodeString)) {
                try {
                    errorCode = Integer.parseInt(errorCodeString);
                } catch (NumberFormatException ex) {
                    errorCode = FacebookRequestError.INVALID_ERROR_CODE;
                }
            }

            if (Utility.isNullOrEmpty(error) && Utility.isNullOrEmpty(errorMessage)
                    && errorCode == FacebookRequestError.INVALID_ERROR_CODE) {
                super.onComplete(request, values, null);
            } else if (error != null && (error.equals("access_denied") ||
                    error.equals("OAuthAccessDeniedException"))) {
                super.onComplete(request, null, new FacebookOperationCanceledException());
            } else if (errorCode == API_EC_DIALOG_CANCEL) {
                super.onComplete(request, null, new FacebookOperationCanceledException());
            } else {
                FacebookRequestError requestError =
                        new FacebookRequestError(errorCode, error, errorMessage);
                super.onComplete(
                        request,
                        null,
                        new FacebookServiceException(requestError, errorMessage));
            }
        }
    }

    @Override
    protected void putChallengeParam(JSONObject param) throws JSONException {
        param.put(LoginLogger.EVENT_PARAM_CHALLENGE, expectedChallenge);
    }

    private boolean validateChallengeParam(Bundle values) {
        try {
            String stateString = values.getString(ServerProtocol.DIALOG_PARAM_STATE);
            if (stateString == null) {
                return false;
            }
            JSONObject state = new JSONObject(stateString);
            String challenge = state.getString(LoginLogger.EVENT_PARAM_CHALLENGE);
            return challenge.equals(expectedChallenge);
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    CustomTabLoginMethodHandler(Parcel source) {
        super(source);
        expectedChallenge = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(expectedChallenge);
    }

    public static final Parcelable.Creator<CustomTabLoginMethodHandler> CREATOR =
            new Parcelable.Creator() {

                @Override
                public CustomTabLoginMethodHandler createFromParcel(Parcel source) {
                    return new CustomTabLoginMethodHandler(source);
                }

                @Override
                public CustomTabLoginMethodHandler[] newArray(int size) {
                    return new CustomTabLoginMethodHandler[size];
                }
            };
}
