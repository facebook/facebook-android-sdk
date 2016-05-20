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
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.facebook.AccessTokenSource;
import com.facebook.FacebookSdk;
import com.facebook.internal.CustomTab;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class CustomTabLoginMethodHandler extends WebLoginMethodHandler {
    private static final String OAUTH_DIALOG = "oauth";
    private static final String CUSTOM_TABS_SERVICE_ACTION =
            "android.support.customtabs.action.CustomTabsService";
    private static final String CHROME_PACKAGE = "com.android.chrome";

    private CustomTab customTab;

    CustomTabLoginMethodHandler(LoginClient loginClient) {
        super(loginClient);
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
    boolean tryAuthorize(final LoginClient.Request request) {
        if (!isCustomTabsAllowed()) {
            return false;
        }

        Bundle parameters = getParameters(request);
        parameters = addExtraParameters(parameters, request);
        Activity activity = loginClient.getActivity();

        customTab = new CustomTab(OAUTH_DIALOG, parameters);

        customTab.openCustomTab(activity);

        return true;
    }

    @Override
    protected void putChallengeParam(JSONObject param) throws JSONException {
        if (loginClient.getFragment() instanceof LoginFragment) {
            param.put(LoginLogger.EVENT_PARAM_CHALLENGE,
                    ((LoginFragment) loginClient.getFragment()).getChallengeParam());
        }
    }

    private boolean isCustomTabsAllowed() {
        return isCustomTabsEnabled()
                && isChromeCustomTabsSupported(loginClient.getActivity())
                && Validate.hasCustomTabRedirectActivity(FacebookSdk.getApplicationContext());
    }

    private boolean isChromeCustomTabsSupported(final Context context) {
        Intent serviceIntent = new Intent(CUSTOM_TABS_SERVICE_ACTION);
        serviceIntent.setPackage(CHROME_PACKAGE);
        List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentServices(serviceIntent, 0);
        return !(resolveInfos == null || resolveInfos.isEmpty());
    }

    private boolean isCustomTabsEnabled() {
        final String appId = Utility.getMetadataApplicationId(loginClient.getActivity());
        final Utility.FetchedAppSettings settings = Utility.getAppSettingsWithoutQuery(appId);
        return settings != null && settings.getCustomTabsEnabled();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    CustomTabLoginMethodHandler(Parcel source) {
        super(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
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
