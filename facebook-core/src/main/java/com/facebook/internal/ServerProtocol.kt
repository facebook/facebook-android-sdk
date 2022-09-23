/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.os.Bundle
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.internal.Logger.Companion.log
import org.json.JSONException

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object ServerProtocol {
  private val TAG = ServerProtocol::class.java.name
  private const val DIALOG_AUTHORITY_FORMAT = "m.%s"
  private const val GAMING_DIALOG_AUTHORITY_FORMAT = "%s"
  const val DIALOG_PATH = "dialog/"
  const val DIALOG_PARAM_ACCESS_TOKEN = "access_token"
  const val DIALOG_PARAM_APP_ID = "app_id"
  const val DIALOG_PARAM_AUTH_TYPE = "auth_type"
  const val DIALOG_PARAM_CBT = "cbt"
  const val DIALOG_PARAM_CLIENT_ID = "client_id"
  const val DIALOG_PARAM_CODE_CHALLENGE = "code_challenge"
  const val DIALOG_PARAM_CODE_CHALLENGE_METHOD = "code_challenge_method"
  const val DIALOG_PARAM_CODE_REDIRECT_URI = "code_redirect_uri"
  const val DIALOG_PARAM_CUSTOM_TABS_PREFETCHING = "cct_prefetching"
  const val DIALOG_PARAM_DISPLAY = "display"
  const val DIALOG_PARAM_DISPLAY_TOUCH = "touch"
  const val DIALOG_PARAM_E2E = "e2e"
  const val DIALOG_PARAM_AUTHENTICATION_TOKEN = "id_token"
  const val DIALOG_PARAM_IES = "ies"
  const val DIALOG_PARAM_LEGACY_OVERRIDE = "legacy_override"
  const val DIALOG_PARAM_LOGIN_BEHAVIOR = "login_behavior"
  const val DIALOG_PARAM_NONCE = "nonce"
  const val DIALOG_PARAM_REDIRECT_URI = "redirect_uri"
  const val DIALOG_PARAM_RESPONSE_TYPE = "response_type"
  const val DIALOG_PARAM_RETURN_SCOPES = "return_scopes"
  const val DIALOG_PARAM_SCOPE = "scope"
  const val DIALOG_PARAM_SSO_DEVICE = "sso"
  const val DIALOG_PARAM_DEFAULT_AUDIENCE = "default_audience"
  const val DIALOG_PARAM_SDK_VERSION = "sdk"
  const val DIALOG_PARAM_STATE = "state"
  const val DIALOG_PARAM_FAIL_ON_LOGGED_OUT = "fail_on_logged_out"
  const val DIALOG_PARAM_CCT_OVER_LOGGED_OUT_APP_SWITCH = "cct_over_app_switch"
  const val DIALOG_PARAM_MESSENGER_PAGE_ID = "messenger_page_id"
  const val DIALOG_PARAM_RESET_MESSENGER_STATE = "reset_messenger_state"
  const val DIALOG_REREQUEST_AUTH_TYPE = "rerequest"
  const val DIALOG_PARAM_FX_APP = "fx_app"
  const val DIALOG_PARAM_SKIP_DEDUPE = "skip_dedupe"
  const val DIALOG_RESPONSE_TYPE_CODE = "code,signed_request,graph_domain"
  const val DIALOG_RESPONSE_TYPE_TOKEN_AND_SCOPES =
      "token,signed_request,graph_domain,granted_scopes"
  const val DIALOG_RESPONSE_TYPE_TOKEN_AND_SIGNED_REQUEST = "token,signed_request,graph_domain"
  const val DIALOG_RESPONSE_TYPE_ID_TOKEN_AND_SIGNED_REQUEST =
      "id_token,token,signed_request,graph_domain"
  const val DIALOG_RETURN_SCOPES_TRUE = "true"
  const val DIALOG_REDIRECT_URI = "fbconnect://success"
  const val DIALOG_REDIRECT_CHROME_OS_URI = "fbconnect://chrome_os_success"
  const val DIALOG_CANCEL_URI = "fbconnect://cancel"
  const val FALLBACK_DIALOG_PARAM_APP_ID = "app_id"
  const val FALLBACK_DIALOG_PARAM_BRIDGE_ARGS = "bridge_args"
  const val FALLBACK_DIALOG_PARAM_KEY_HASH = "android_key_hash"
  const val FALLBACK_DIALOG_PARAM_METHOD_ARGS = "method_args"
  const val FALLBACK_DIALOG_PARAM_METHOD_RESULTS = "method_results"
  const val FALLBACK_DIALOG_PARAM_VERSION = "version"
  const val FALLBACK_DIALOG_DISPLAY_VALUE_TOUCH = "touch"

  const val INSTAGRAM_OAUTH_PATH = "oauth/authorize"

  // URL components
  private const val GRAPH_VIDEO_URL_FORMAT = "https://graph-video.%s"
  private const val GRAPH_URL_FORMAT = "https://graph.%s"
  @JvmStatic fun getDefaultAPIVersion() = "v15.0"

  @JvmStatic
  fun getErrorsProxyAuthDisabled(): Collection<String> =
      listOf("service_disabled", "AndroidAuthKillSwitchException")

  @JvmStatic
  fun getErrorsUserCanceled(): Collection<String> =
      listOf("access_denied", "OAuthAccessDeniedException")

  @JvmStatic fun getErrorConnectionFailure(): String = "CONNECTION_FAILURE"

  @JvmStatic
  fun getDialogAuthority(): String =
      String.format(DIALOG_AUTHORITY_FORMAT, FacebookSdk.getFacebookDomain())
  @JvmStatic
  fun getGamingDialogAuthority(): String =
      String.format(GAMING_DIALOG_AUTHORITY_FORMAT, FacebookSdk.getFacebookGamingDomain())
  @JvmStatic
  fun getInstagramDialogAuthority(): String =
      String.format(DIALOG_AUTHORITY_FORMAT, FacebookSdk.getInstagramDomain())
  @JvmStatic
  fun getGraphUrlBase(): String = String.format(GRAPH_URL_FORMAT, FacebookSdk.getGraphDomain())
  @JvmStatic
  fun getGraphVideoUrlBase(): String =
      String.format(GRAPH_VIDEO_URL_FORMAT, FacebookSdk.getGraphDomain())
  @JvmStatic
  fun getFacebookGraphUrlBase(): String =
      String.format(GRAPH_URL_FORMAT, FacebookSdk.getFacebookDomain())
  @JvmStatic
  fun getGraphUrlBaseForSubdomain(subdomain: String): String =
      String.format(GRAPH_URL_FORMAT, subdomain)

  @JvmStatic
  fun getQueryParamsForPlatformActivityIntentWebFallback(
      callId: String,
      version: Int,
      methodArgs: Bundle?
  ): Bundle? {
    val context = FacebookSdk.getApplicationContext()
    val keyHash = FacebookSdk.getApplicationSignature(context)
    if (Utility.isNullOrEmpty(keyHash)) {
      return null
    }
    val webParams = Bundle()
    webParams.putString(FALLBACK_DIALOG_PARAM_KEY_HASH, keyHash)
    webParams.putString(FALLBACK_DIALOG_PARAM_APP_ID, FacebookSdk.getApplicationId())
    webParams.putInt(FALLBACK_DIALOG_PARAM_VERSION, version)
    webParams.putString(DIALOG_PARAM_DISPLAY, FALLBACK_DIALOG_DISPLAY_VALUE_TOUCH)
    val bridgeArguments = Bundle()
    bridgeArguments.putString(NativeProtocol.BRIDGE_ARG_ACTION_ID_STRING, callId)
    try {
      val bridgeArgsJSON = BundleJSONConverter.convertToJSON(bridgeArguments)
      val methodArgsJSON = BundleJSONConverter.convertToJSON(methodArgs ?: Bundle())
      if (bridgeArgsJSON == null || methodArgsJSON == null) {
        return null
      }
      webParams.putString(FALLBACK_DIALOG_PARAM_BRIDGE_ARGS, bridgeArgsJSON.toString())
      webParams.putString(FALLBACK_DIALOG_PARAM_METHOD_ARGS, methodArgsJSON.toString())
    } catch (je: JSONException) {
      log(LoggingBehavior.DEVELOPER_ERRORS, Log.ERROR, TAG, "Error creating Url -- $je")
      return null
    } catch (je: IllegalArgumentException) {
      log(LoggingBehavior.DEVELOPER_ERRORS, Log.ERROR, TAG, "Error creating Url -- $je")
      return null
    }
    return webParams
  }
}
