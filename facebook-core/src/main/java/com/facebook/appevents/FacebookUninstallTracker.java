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

package com.facebook.appevents;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class FacebookUninstallTracker {
    private static final String TAG = FacebookUninstallTracker.class.getCanonicalName();
    private static final String PLATFORM = "android";
    private static final String SUCCESS = "success";
    private static final String UPLOADED_TOKEN_STORE =
            "com.facebook.appevents.FacebookUninstallTracker.UPLOADED_TOKEN";
    private static final SharedPreferences uploadedTokenSharedPrefs =
            FacebookSdk.getApplicationContext().getSharedPreferences(
                    UPLOADED_TOKEN_STORE,
                    Context.MODE_PRIVATE);

    /**
     * Persist token to servers.
     *
     * @param deviceToken The new token.
     */
    public static void updateDeviceToken(String deviceToken) {
        AppEventsLogger.setPushNotificationsRegistrationId(deviceToken);
        final String appId = FacebookSdk.getApplicationId();
        final FetchedAppSettings appSettings =
                FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId);
        if (appSettings == null) {
            return;
        }
        boolean nowTrackUninstallEnabled = appSettings.getTrackUninstallEnabled();
        String uploadedToken = uploadedTokenSharedPrefs.getString("uploaded_token", null);
        boolean preTrackUninstallEnabled =
                uploadedTokenSharedPrefs.getBoolean("pre_track_uninstall_enabled", false);
        if (nowTrackUninstallEnabled &&
                (!preTrackUninstallEnabled || !deviceToken.equals(uploadedToken))) {
            sendToServer(deviceToken);
        } else if (!nowTrackUninstallEnabled && preTrackUninstallEnabled) {
            SharedPreferences.Editor editor = uploadedTokenSharedPrefs.edit();
            editor.putBoolean("pre_track_uninstall_enabled", false).apply();
        }
    }

    /**
     * Update token to app_push_device_token.
     *
     * @param deviceToken The new token.
     */
    private static void sendToServer(final String deviceToken) {
        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final String appId = FacebookSdk.getApplicationId();

                GraphRequest request = buildPushDeviceTokenRequest(appId, deviceToken);
                if (request != null) {
                    GraphResponse res = request.executeAndWait();
                    try {
                        JSONObject jsonRes = res.getJSONObject();
                        if (jsonRes != null) {
                            if (jsonRes.has(SUCCESS)
                                    && jsonRes.getString(SUCCESS).equals("true")) {
                                SharedPreferences.Editor editor = uploadedTokenSharedPrefs.edit();
                                editor.putString("uploaded_token", deviceToken)
                                        .putBoolean("pre_track_uninstall_enabled", true)
                                        .apply();
                            } else {
                                Log.e(TAG, "Error sending device token to Facebook: "
                                        + res.getError());
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error decoding server response.", e);
                    }
                }
            }
        });
    }

    @Nullable
    private static GraphRequest buildPushDeviceTokenRequest(
            final String appId,
            final String deviceToken) {
        final GraphRequest postRequest = GraphRequest.newPostRequest(null,
                String.format(Locale.US, "%s/app_push_device_token", appId),
                null,
                null);
        postRequest.setSkipClientToken(true);

        final Context context = FacebookSdk.getApplicationContext();
        AttributionIdentifiers identifiers =
                AttributionIdentifiers.getAttributionIdentifiers(context);
        if (identifiers == null) {
            return null;
        }
        final String deviceId = identifiers.getAndroidAdvertiserId();
        Bundle requestParameters = postRequest.getParameters();
        if (requestParameters == null) {
            requestParameters = new Bundle();
        }
        requestParameters.putString("device_id", deviceId);
        requestParameters.putString("device_token", deviceToken);
        requestParameters.putString("platform", PLATFORM);
        postRequest.setParameters(requestParameters);

        return postRequest;
    }
}
