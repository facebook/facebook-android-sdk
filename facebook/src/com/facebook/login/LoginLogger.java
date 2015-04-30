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
import android.text.TextUtils;

import com.facebook.appevents.AppEventsLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class LoginLogger {
    // Constants for logging login-related data.
    static final String EVENT_NAME_LOGIN_METHOD_START = "fb_mobile_login_method_start";
    static final String EVENT_NAME_LOGIN_METHOD_COMPLETE = "fb_mobile_login_method_complete";
    static final String EVENT_PARAM_METHOD_RESULT_SKIPPED = "skipped";
    static final String EVENT_NAME_LOGIN_START = "fb_mobile_login_start";
    static final String EVENT_NAME_LOGIN_COMPLETE = "fb_mobile_login_complete";
    // Note: to ensure stability of column mappings across the four different event types, we
    // prepend a column index to each name, and we log all columns with all events, even if they are
    // empty.
    static final String EVENT_PARAM_AUTH_LOGGER_ID = "0_auth_logger_id";
    static final String EVENT_PARAM_TIMESTAMP = "1_timestamp_ms";
    static final String EVENT_PARAM_LOGIN_RESULT = "2_result";
    static final String EVENT_PARAM_METHOD = "3_method";
    static final String EVENT_PARAM_ERROR_CODE = "4_error_code";
    static final String EVENT_PARAM_ERROR_MESSAGE = "5_error_message";
    static final String EVENT_PARAM_EXTRAS = "6_extras";
    static final String EVENT_EXTRAS_TRY_LOGIN_ACTIVITY = "try_login_activity";
    static final String EVENT_EXTRAS_MISSING_INTERNET_PERMISSION = "no_internet_permission";
    static final String EVENT_EXTRAS_NOT_TRIED = "not_tried";
    static final String EVENT_EXTRAS_NEW_PERMISSIONS = "new_permissions";
    static final String EVENT_EXTRAS_LOGIN_BEHAVIOR = "login_behavior";
    static final String EVENT_EXTRAS_REQUEST_CODE = "request_code";
    static final String EVENT_EXTRAS_PERMISSIONS = "permissions";
    static final String EVENT_EXTRAS_DEFAULT_AUDIENCE = "default_audience";
    static final String EVENT_EXTRAS_IS_REAUTHORIZE = "isReauthorize";

    private final AppEventsLogger appEventsLogger;
    private String applicationId;

    LoginLogger(Context context, String applicationId) {
        this.applicationId = applicationId;

        appEventsLogger = AppEventsLogger.newLogger(context, applicationId);
    }

    public String getApplicationId() {
        return applicationId;
    }

    static Bundle newAuthorizationLoggingBundle(String authLoggerId) {
        // We want to log all parameters for all events, to ensure stability of columns across
        // different event types.
        Bundle bundle = new Bundle();
        bundle.putLong(EVENT_PARAM_TIMESTAMP, System.currentTimeMillis());
        bundle.putString(EVENT_PARAM_AUTH_LOGGER_ID, authLoggerId);
        bundle.putString(EVENT_PARAM_METHOD, "");
        bundle.putString(EVENT_PARAM_LOGIN_RESULT, "");
        bundle.putString(EVENT_PARAM_ERROR_MESSAGE, "");
        bundle.putString(EVENT_PARAM_ERROR_CODE, "");
        bundle.putString(EVENT_PARAM_EXTRAS, "");
        return bundle;
    }

    public void logStartLogin(LoginClient.Request pendingLoginRequest) {
        Bundle bundle = newAuthorizationLoggingBundle(pendingLoginRequest.getAuthId());

        // Log what we already know about the call in start event
        try {
            JSONObject extras = new JSONObject();
            extras.put(EVENT_EXTRAS_LOGIN_BEHAVIOR,
                    pendingLoginRequest.getLoginBehavior().toString());
            extras.put(EVENT_EXTRAS_REQUEST_CODE, LoginClient.getLoginRequestCode());
            extras.put(EVENT_EXTRAS_PERMISSIONS,
                    TextUtils.join(",", pendingLoginRequest.getPermissions()));
            extras.put(EVENT_EXTRAS_DEFAULT_AUDIENCE,
                    pendingLoginRequest.getDefaultAudience().toString());
            extras.put(EVENT_EXTRAS_IS_REAUTHORIZE, pendingLoginRequest.isRerequest());
            bundle.putString(EVENT_PARAM_EXTRAS, extras.toString());
        } catch (JSONException e) {
        }

        appEventsLogger.logSdkEvent(EVENT_NAME_LOGIN_START, null, bundle);
    }

    public void logCompleteLogin(String loginRequestId, Map<String, String> loggingExtras,
        LoginClient.Result.Code result, Map<String, String> resultExtras, Exception exception) {

        Bundle bundle = newAuthorizationLoggingBundle(loginRequestId);
        if (result != null) {
            bundle.putString(EVENT_PARAM_LOGIN_RESULT, result.getLoggingValue());
        }
        if (exception != null && exception.getMessage() != null) {
            bundle.putString(EVENT_PARAM_ERROR_MESSAGE, exception.getMessage());
        }

        // Combine extras from the request and from the result.
        JSONObject jsonObject = null;
        if (loggingExtras.isEmpty() == false) {
            jsonObject = new JSONObject(loggingExtras);
        }
        if (resultExtras != null) {
            if (jsonObject == null) {
                jsonObject = new JSONObject();
            }
            try {
                for (Map.Entry<String, String> entry : resultExtras.entrySet()) {
                    jsonObject.put(entry.getKey(), entry.getValue());
                }
            } catch (JSONException e) {
            }
        }
        if (jsonObject != null) {
            bundle.putString(EVENT_PARAM_EXTRAS, jsonObject.toString());
        }

        appEventsLogger.logSdkEvent(EVENT_NAME_LOGIN_COMPLETE, null, bundle);
    }

    public void logAuthorizationMethodStart(String authId, String method) {
        Bundle bundle = LoginLogger.newAuthorizationLoggingBundle(authId);
        bundle.putString(EVENT_PARAM_METHOD, method);

        appEventsLogger.logSdkEvent(EVENT_NAME_LOGIN_METHOD_START, null, bundle);
    }

    public void logAuthorizationMethodComplete(String authId, String method, String result,
        String errorMessage, String errorCode, Map<String, String> loggingExtras) {

        Bundle bundle;
        bundle = LoginLogger.newAuthorizationLoggingBundle(authId);
        if (result != null) {
            bundle.putString(LoginLogger.EVENT_PARAM_LOGIN_RESULT, result);
        }
        if (errorMessage != null) {
            bundle.putString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE, errorMessage);
        }
        if (errorCode != null) {
            bundle.putString(LoginLogger.EVENT_PARAM_ERROR_CODE, errorCode);
        }
        if (loggingExtras != null && !loggingExtras.isEmpty()) {
            JSONObject jsonObject = new JSONObject(loggingExtras);
            bundle.putString(LoginLogger.EVENT_PARAM_EXTRAS, jsonObject.toString());
        }
        bundle.putString(EVENT_PARAM_METHOD, method);

        appEventsLogger.logSdkEvent(EVENT_NAME_LOGIN_METHOD_COMPLETE, null, bundle);
    }

    public void logUnexpectedError(String eventName, String errorMessage) {
        logUnexpectedError(eventName, errorMessage, "");
    }

    public void logUnexpectedError(String eventName, String errorMessage, String method) {
        Bundle bundle = newAuthorizationLoggingBundle("");
        bundle.putString(EVENT_PARAM_LOGIN_RESULT,
                LoginClient.Result.Code.ERROR.getLoggingValue());
        bundle.putString(EVENT_PARAM_ERROR_MESSAGE, errorMessage);
        bundle.putString(EVENT_PARAM_METHOD, method);

        appEventsLogger.logSdkEvent(eventName, null, bundle);
    }
}
