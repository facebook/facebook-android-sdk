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

package com.facebook.internal;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
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
    public static final String DIALOG_PARAM_DISPLAY_TOUCH = "touch";
    public static final String DIALOG_PARAM_E2E = "e2e";
    public static final String DIALOG_PARAM_LEGACY_OVERRIDE = "legacy_override";
    public static final String DIALOG_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String DIALOG_PARAM_RESPONSE_TYPE = "response_type";
    public static final String DIALOG_PARAM_RETURN_SCOPES = "return_scopes";
    public static final String DIALOG_PARAM_SCOPE = "scope";
    public static final String DIALOG_PARAM_DEFAULT_AUDIENCE = "default_audience";
    public static final String DIALOG_PARAM_SDK_VERSION = "sdk";
    public static final String DIALOG_REREQUEST_AUTH_TYPE = "rerequest";
    public static final String DIALOG_RESPONSE_TYPE_TOKEN_AND_SIGNED_REQUEST
            = "token,signed_request";
    public static final String DIALOG_RETURN_SCOPES_TRUE = "true";
    public static final String DIALOG_REDIRECT_URI = "fbconnect://success";
    public static final String DIALOG_CANCEL_URI = "fbconnect://cancel";

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
    public static final String GRAPH_API_VERSION = "v2.5";

    public static final Collection<String> errorsProxyAuthDisabled =
            Utility.unmodifiableCollection("service_disabled", "AndroidAuthKillSwitchException");
    public static final Collection<String> errorsUserCanceled =
            Utility.unmodifiableCollection("access_denied", "OAuthAccessDeniedException");
    public static final String errorConnectionFailure = "CONNECTION_FAILURE";

    public static final String getDialogAuthority() {
        return String.format(DIALOG_AUTHORITY_FORMAT, FacebookSdk.getFacebookDomain());
    }

    public static final String getGraphUrlBase() {
        return String.format(GRAPH_URL_FORMAT, FacebookSdk.getFacebookDomain());
    }

    public static final String getGraphVideoUrlBase() {
        return String.format(GRAPH_VIDEO_URL_FORMAT, FacebookSdk.getFacebookDomain());
    }

    public static final String getAPIVersion() {
        return GRAPH_API_VERSION;
    }

    public static Bundle getQueryParamsForPlatformActivityIntentWebFallback(
            String callId,
            int version,
            Bundle methodArgs) {

        Context context = FacebookSdk.getApplicationContext();
        String keyHash = FacebookSdk.getApplicationSignature(context);
        if (Utility.isNullOrEmpty(keyHash)) {
            return null;
        }

        Bundle webParams = new Bundle();

        webParams.putString(FALLBACK_DIALOG_PARAM_KEY_HASH, keyHash);
        webParams.putString(FALLBACK_DIALOG_PARAM_APP_ID, FacebookSdk.getApplicationId());
        webParams.putInt(FALLBACK_DIALOG_PARAM_VERSION, version);
        webParams.putString(DIALOG_PARAM_DISPLAY, FALLBACK_DIALOG_DISPLAY_VALUE_TOUCH);

        Bundle bridgeArguments = new Bundle();
        bridgeArguments.putString(NativeProtocol.BRIDGE_ARG_ACTION_ID_STRING, callId);

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
