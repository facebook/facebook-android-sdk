/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
  public static final String EVENT_GAME_LOAD_COMPLETE = "cloud_games_load_complete";
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
