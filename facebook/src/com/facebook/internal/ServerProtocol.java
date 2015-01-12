/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.internal;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.facebook.LoggingBehavior;
import com.facebook.Settings;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.EnumSet;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 */
public final class ServerProtocol {
    private static final String TAG = ServerProtocol.class.getName();

    private static final String DIALOG_AUTHORITY_FORMAT = "m.%s";
    public static final String DIALOG_PATH = "dialog/";
    public static final String DIALOG_PARAM_ACCESS_TOKEN = "access_token";
    public static final String DIALOG_PARAM_APP_ID = "app_id";
    public static final String DIALOG_PARAM_AUTH_TYPE = "auth_type";
    public static final String DIALOG_PARAM_CLIENT_ID = "client_id";
    public static final String DIALOG_PARAM_DISPLAY = "display";
    public static final String DIALOG_PARAM_E2E = "e2e";
    public static final String DIALOG_PARAM_LEGACY_OVERRIDE = "legacy_override";
    public static final String DIALOG_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String DIALOG_PARAM_RESPONSE_TYPE = "response_type";
    public static final String DIALOG_PARAM_RETURN_SCOPES = "return_scopes";
    public static final String DIALOG_PARAM_SCOPE = "scope";
    public static final String DIALOG_PARAM_DEFAULT_AUDIENCE = "default_audience";
    public static final String DIALOG_REREQUEST_AUTH_TYPE = "rerequest";
    public static final String DIALOG_RESPONSE_TYPE_TOKEN = "token";
    public static final String DIALOG_RETURN_SCOPES_TRUE = "true";

    public static final String FALLBACK_DIALOG_PARAM_APP_ID = "app_id";
    public static final String FALLBACK_DIALOG_PARAM_BRIDGE_ARGS = "bridge_args";
    public static final String FALLBACK_DIALOG_PARAM_KEY_HASH = "android_key_hash";
    public static final String FALLBACK_DIALOG_PARAM_METHOD_ARGS = "method_args";
    public static final String FALLBACK_DIALOG_PARAM_METHOD_RESULTS = "method_results";
    public static final String FALLBACK_DIALOG_PARAM_VERSION = "version";
    public static final String FALLBACK_DIALOG_DISPLAY_VALUE_TOUCH = "touch";

    // URL components
    private static final String GRAPH_VIDEO_URL_FORMAT = "https://graph-video.%s";
    private static final String GRAPH_URL_FORMAT = "https://graph.%s";
    public static final String GRAPH_API_VERSION = "v2.2";

    private static final String LEGACY_API_VERSION = "v1.0";

    public static final Collection<String> errorsProxyAuthDisabled =
            Utility.unmodifiableCollection("service_disabled", "AndroidAuthKillSwitchException");
    public static final Collection<String> errorsUserCanceled =
            Utility.unmodifiableCollection("access_denied", "OAuthAccessDeniedException");

    public static final String getDialogAuthority() {
        return String.format(DIALOG_AUTHORITY_FORMAT, Settings.getFacebookDomain());
    }

    public static final String getGraphUrlBase() {
        return String.format(GRAPH_URL_FORMAT, Settings.getFacebookDomain());
    }

    public static final String getGraphVideoUrlBase() {
        return String.format(GRAPH_VIDEO_URL_FORMAT, Settings.getFacebookDomain());
    }

    public static final String getAPIVersion() {
        if (Settings.getPlatformCompatibilityEnabled()) {
            return LEGACY_API_VERSION;
        }
        return GRAPH_API_VERSION;
    }

    public static Bundle getQueryParamsForPlatformActivityIntentWebFallback(
            Context context,
            String callId,
            int version,
            String applicationName,
            Bundle methodArgs) {

        String keyHash = Settings.getApplicationSignature(context);
        if (Utility.isNullOrEmpty(keyHash)) {
            return null;
        }

        Bundle webParams = new Bundle();

        webParams.putString(FALLBACK_DIALOG_PARAM_KEY_HASH, keyHash);
        webParams.putString(FALLBACK_DIALOG_PARAM_APP_ID, Settings.getApplicationId());
        webParams.putInt(FALLBACK_DIALOG_PARAM_VERSION, version);
        webParams.putString(DIALOG_PARAM_DISPLAY, FALLBACK_DIALOG_DISPLAY_VALUE_TOUCH);

        Bundle bridgeArguments = new Bundle();
        bridgeArguments.putString(NativeProtocol.BRIDGE_ARG_ACTION_ID_STRING, callId);
        bridgeArguments.putString(NativeProtocol.BRIDGE_ARG_APP_NAME_STRING, applicationName);

        methodArgs = (methodArgs == null) ? new Bundle() : methodArgs;

        try {
            JSONObject bridgeArgsJSON = BundleJSONConverter.convertToJSON(bridgeArguments);
            JSONObject methodArgsJSON = BundleJSONConverter.convertToJSON(methodArgs);

            if (bridgeArgsJSON == null || methodArgsJSON == null) {
                return null;
            }

            webParams.putString(FALLBACK_DIALOG_PARAM_BRIDGE_ARGS, bridgeArgsJSON.toString());
            webParams.putString(FALLBACK_DIALOG_PARAM_METHOD_ARGS, methodArgsJSON.toString());
        } catch (JSONException je) {
            webParams = null;
            Logger.log(LoggingBehavior.DEVELOPER_ERRORS, Log.ERROR, TAG,
                    "Error creating Url -- " + je);
        }

        return webParams;
    }
}
