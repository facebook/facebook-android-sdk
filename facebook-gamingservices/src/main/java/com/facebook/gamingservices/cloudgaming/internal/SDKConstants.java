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
public class SDKConstants {
  // A2U Notifications
  public static final String PARAM_A2U_TITLE = "title";
  public static final String PARAM_A2U_BODY = "body";
  public static final String PARAM_A2U_TIME_INTERVAL = "time_interval";
  public static final String PARAM_A2U_PAYLOAD = "payload:";
  public static final String PARAM_A2U_MEDIA_ID = "media_id";
  public static final String PARAM_A2U_RESPONSE_ID = "id";
  public static final String PARAM_A2U_CAPTION = "A2U Image";
  public static final String PARAM_A2U_GRAPH_PATH = "me/schedule_gaming_app_to_user_update";

  // In App Purchase
  public static final String PARAM_PRODUCT_ID = "productID";
  public static final String PARAM_PURCHASE_TOKEN = "purchaseToken";
  public static final String PARAM_DEVELOPER_PAYLOAD = "developerPayload";

  // In App Ads
  public static final String PARAM_PLACEMENT_ID = "placementID";

  // FB Login
  public static final String PARAM_ACCESS_TOKEN = "accessToken";
  public static final String PARAM_ACCESS_TOKEN_SOURCE = "accessTokenSource";
  public static final String PARAM_APP_ID = "appID";
  public static final String PARAM_CONTEXT_TOKEN = "contextToken";
  public static final String PARAM_DATA_ACCESS_EXPIRATION_TIME = "dataAccessExpirationTime";
  public static final String PARAM_DECLINED_PERMISSIONS = "declinedPermissions";
  public static final String PARAM_EXPIRED_PERMISSIONS = "expiredPermissions";
  public static final String PARAM_EXPIRATION_TIME = "expirationTime";
  public static final String PARAM_GRAPH_DOMAIN = "graphDomain";
  public static final String PARAM_LAST_REFRESH_TIME = "lastRefreshTime";
  public static final String PARAM_PAYLOAD = "payload";
  public static final String PARAM_PERMISSIONS = "permissions";
  public static final String PARAM_USER_ID = "userID";

  // Start Info
  public static final String PARAM_DAEMON_PACKAGE_NAME = "daemonPackageName";
  public static final String PARAM_GAME_PACKAGE_NAME = "gamePackageName";
  public static final String PARAM_SESSION_ID = "sessionID";

  // Request
  public static final String REQUEST_ID = "requestID";
  public static final String REQUEST_ACTION = "com.facebook.gamingservices.DAEMON_REQUEST";
  public static final String PARAM_TYPE = "type";

  // Receiver
  public static final String RECEIVER_PAYLOAD = "returnPayload";
  public static final String RECEIVER_INTENT = "com.facebook.gamingservices.DAEMON_RESPONSE";
  public static final String RECEIVER_HANDLER =
      "com.facebook.gamingservices.DAEMON_RESPONSE_HANDLER";

  // Shared Preferences
  public static final String PREF_DAEMON_PACKAGE_NAME =
      "com.facebook.gamingservices.cloudgaming:preferences";

  // Game features
  public static final String PARAM_KEY = "key";
  public static final String PARAM_VALUE = "value";
  public static final String PARAM_SCORE = "score";

  // Share
  public static final String PARAM_INTENT = "intent";
  public static final String PARAM_IMAGE = "image";
  public static final String PARAM_TEXT = "text";
  public static final String PARAM_DATA = "data";

  // Context
  public static final String PARAM_CONTEXT_MIN_SIZE = "minSize";
  public static final String PARAM_CONTEXT_MAX_SIZE = "maxSize";
  public static final String PARAM_CONTEXT_ID = "id";
  public static final String PARAM_CONTEXT_FILTERS = "filters";

  // Update
  public static final String PARAM_UPDATE_ACTION = "action";
  public static final String PARAM_UPDATE_TEMPLATE = "template";
  public static final String PARAM_UPDATE_TEXT = "text";
  public static final String PARAM_UPDATE_IMAGE = "image";

  // Gaming Services Deep Links
  public static final String PARAM_DEEP_LINK = "deepLink";
  public static final String PARAM_DEEP_LINK_ID = "id";
}
