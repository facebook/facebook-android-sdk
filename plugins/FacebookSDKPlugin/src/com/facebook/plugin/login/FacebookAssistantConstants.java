/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
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
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.plugin.login;

/**
 * Constants needed for IDE-wide FBLogin to Facebook Assistant app
 */
public final class FacebookAssistantConstants {

    public static final String PREFERENCES_PATH = "/com/facebook/login";

    public static final String LOG_IN_TEXT = "Log in to Facebook";
    public static final String LOGGED_IN_TEXT = "Logged in as %s";
    public static final String LOGGED_IN_DESCRIPTION = "<html><h2>%s</h2><span>%s</span></html>";

    public static final String BUTTON_LOGIN = "Log In";
    public static final String BUTTON_LOGOUT = "Log Out";

    public static final String GRAPH_API_URL = "https://graph.facebook.com/v2.10";
    public static final String LOGIN_REQUEST_PATH = "/device/login";
    public static final String LOGIN_STATUS_PATH = "/device/login_status";
    public static final String PROFILE_PATH = "/me";

    public static final String HEADER_NAME = "Accept";
    public static final String HEADER_VALUE = "application/json";

    public static final String PARAM_ACCESS_TOKEN = "access_token";
    public static final String PARAM_CODE = "code";
    public static final String PARAM_SCOPE = "scope";
    public static final String PARAM_REDIRECT_URI = "redirect_uri";
    public static final String PARAM_FIELDS = "fields";

    public static final String RESPONSE_CODE = "code";
    public static final String RESPONSE_USER_CODE = "user_code";
    public static final String RESPONSE_VERIFICATION_URL = "verification_uri";
    public static final String RESPONSE_EXPIRES_IN = "expires_in";
    public static final String RESPONSE_INTERVAL = "interval";
    public static final String RESPONSE_ACCESS_TOKEN = "access_token";
    public static final String RESPONSE_ERROR = "error";
    public static final String RESPONSE_ERROR_SUBCODE = "error_subcode";

    public static final int ERROR_SUBCODE_NOT_AUTHORIZED = 1349174;
    public static final int ERROR_SUBCODE_TOO_FREQUENT = 1349172;
    public static final int ERROR_SUBCODE_LOGIN_EXPIRED = 1349152;

    public static final String DEFAULT_VERIFICATION_URL = "https://www.facebook.com/device";

    public static final String DEVICE_CODE_QUERY = "?user_code=%s";

    public static final String APP_ID = "266842207168836";
    public static final String CLIENT_TOKEN = "370ca2dca3dd544117f1b0b8792fab6f";
    public static final String ACCESS_TOKEN_FORMAT = "%s|%s";

    public static final String LOGIN_SCOPES = "public_profile,email";
    public static final String PROFILE_FIELDS = "name,email,picture";

    public static final String REDIRECT_URL = "https://developers.facebook.com/apps/";

    public static final String FIELD_NAME = "name";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PICTURE = "picture";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_URL = "url";
}
