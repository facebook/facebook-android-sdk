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
  public static final String PARAM_INITIAL_SCORE = "initialScore";
  public static final String PARAM_SORT_ORDER = "sortOrder";
  public static final String PARAM_SCORE_FORMAT = "scoreFormat";
  public static final String PARAM_TITLE = "title";
  public static final String PARAM_END_TIME = "endTime";

  // Share
  public static final String PARAM_INTENT = "intent";
  public static final String PARAM_IMAGE = "image";
  public static final String PARAM_TEXT = "text";
  public static final String PARAM_DATA = "data";

  // Context
  public static final String PARAM_CONTEXT_MIN_SIZE = "minSize";
  public static final String PARAM_CONTEXT_MAX_SIZE = "maxSize";
  public static final String PARAM_CONTEXT_ID = "id";
  public static final String PARAM_CONTEXT_CONTEXT_ID = "context_id";
  public static final String PARAM_CONTEXT_FILTERS = "filters";

  // Update
  public static final String PARAM_UPDATE_ACTION = "action";
  public static final String PARAM_UPDATE_TEMPLATE = "template";
  public static final String PARAM_UPDATE_TEXT = "text";
  public static final String PARAM_UPDATE_IMAGE = "image";

  // Gaming Services Deep Links
  public static final String PARAM_DEEP_LINK = "deepLink";
  public static final String PARAM_DEEP_LINK_ID = "id";

  // Game Requests
  public static final String PARAM_GAME_REQUESTS_ACTION_TYPE = "actionType";
  public static final String PARAM_GAME_REQUESTS_TO = "to";
  public static final String PARAM_GAME_REQUESTS_MESSAGE = "message";
  public static final String PARAM_GAME_REQUESTS_CTA = "cta";
  public static final String PARAM_GAME_REQUESTS_TITLE = "title";
  public static final String PARAM_GAME_REQUESTS_DATA = "data";
  public static final String PARAM_GAME_REQUESTS_OPTIONS = "options";

  // Instant Tournaments
  public static final String PARAM_TOURNAMENTS = "INSTANT_TOURNAMENT";
  public static final String PARAM_TOURNAMENTS_DEEPLINK = "deeplink";
  public static final String PARAM_TOURNAMENTS_APP_ID = "app_id";
  public static final String PARAM_TOURNAMENTS_SCORE = "score";
  public static final String PARAM_TOURNAMENTS_SORT_ORDER = "sort_order";
  public static final String PARAM_TOURNAMENTS_SCORE_FORMAT = "score_format";
  public static final String PARAM_TOURNAMENTS_END_TIME = "end_time";
  public static final String PARAM_TOURNAMENTS_TITLE = "tournament_title";
  public static final String PARAM_TOURNAMENTS_PAYLOAD = "tournament_payload";
  public static final String PARAM_TOURNAMENTS_ID = "tournament_id";

  // Outlinking
  public static final String PARAM_URL = "url";

  // Debug Print
  public static final String PARAM_DEBUG_MESSAGE = "msg";
  public static final String PARAM_DEBUG_MESSAGE_TAG = "tag";
  public static final String PARAM_DEBUG_MESSAGE_SEVERITY = "severity";
  public static final String PARAM_DEBUG_MESSAGE_TIMESTAMP = "timestamp";

  // Tournamet
  public static final String PARAM_TOURNAMENT_ID = "tournamentId";
}
