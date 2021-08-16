// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

// @lint-ignore LICENSELINT
/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.gamingservices.cloudgaming.internal;

/**
 * com.facebook.gamingservices.cloudgaming.internal is solely for the use of other packages within
 * the Facebook SDK for Android. Use of any of the classes in this package is unsupported, and they
 * may be modified or removed without warning at any time.
 */
public class SDKAnalyticsEvents {
  public static final String EVENT_PREPARING_REQUEST = "cloud_games_preparing_request";
  public static final String EVENT_SENT_REQUEST = "cloud_games_sent_request";
  public static final String EVENT_SENDING_SUCCESS_RESPONSE =
      "cloud_games_sending_success_response";
  public static final String EVENT_SENDING_ERROR_RESPONSE = "cloud_games_sending_error_response";
  public static final String EVENT_LOGIN_SUCCESS = "cloud_games_login_success";
  public static final String EVENT_INTERNAL_ERROR = "cloud_games_internal_error";
  public static final String PARAMETER_FUNCTION_TYPE = "function_type";
  public static final String PARAMETER_PAYLOAD = "payload";
  public static final String PARAMETER_ERROR_CODE = "error_code";
  public static final String PARAMETER_ERROR_TYPE = "error_type";
  public static final String PARAMETER_ERROR_MESSAGE = "error_message";
  public static final String PARAMETER_APP_ID = "app_id";
  public static final String PARAMETER_USER_ID = "user_id";
  public static final String PARAMETER_REQUEST_ID = "request_id";
  public static final String PARAMETER_SESSION_ID = "session_id";
}
