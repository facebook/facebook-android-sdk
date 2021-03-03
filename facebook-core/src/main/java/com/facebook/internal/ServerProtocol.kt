/*
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
  const val DIALOG_PATH = "dialog/"
  const val DIALOG_PARAM_ACCESS_TOKEN = "access_token"
  const val DIALOG_PARAM_APP_ID = "app_id"
  const val DIALOG_PARAM_AUTH_TYPE = "auth_type"
  const val DIALOG_PARAM_CBT = "cbt"
  const val DIALOG_PARAM_CLIENT_ID = "client_id"
  const val DIALOG_PARAM_CUSTOM_TABS_PREFETCHING = "cct_prefetching"
  const val DIALOG_PARAM_DISPLAY = "display"
  const val DIALOG_PARAM_DISPLAY_TOUCH = "touch"
  const val DIALOG_PARAM_E2E = "e2e"
  const val DIALOG_PARAM_IES = "ies"
  const val DIALOG_PARAM_LEGACY_OVERRIDE = "legacy_override"
  const val DIALOG_PARAM_LOGIN_BEHAVIOR = "login_behavior"
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
  const val DIALOG_REREQUEST_AUTH_TYPE = "rerequest"
  const val DIALOG_RESPONSE_TYPE_TOKEN_AND_SIGNED_REQUEST = "token,signed_request,graph_domain"
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

  // URL components
  private const val GRAPH_VIDEO_URL_FORMAT = "https://graph-video.%s"
  private const val GRAPH_URL_FORMAT = "https://graph.%s"
  @JvmStatic fun getDefaultAPIVersion() = "v9.0"

  @JvmStatic
  val errorsProxyAuthDisabled: Collection<String> =
      Utility.unmodifiableCollection("service_disabled", "AndroidAuthKillSwitchException")
  @JvmStatic
  val errorsUserCanceled: Collection<String> =
      Utility.unmodifiableCollection("access_denied", "OAuthAccessDeniedException")
  @JvmStatic val errorConnectionFailure = "CONNECTION_FAILURE"
  @JvmStatic
  fun getDialogAuthority() = String.format(DIALOG_AUTHORITY_FORMAT, FacebookSdk.getFacebookDomain())
  @JvmStatic fun getGraphUrlBase() = String.format(GRAPH_URL_FORMAT, FacebookSdk.getGraphDomain())
  @JvmStatic
  fun getGraphVideoUrlBase() = String.format(GRAPH_VIDEO_URL_FORMAT, FacebookSdk.getGraphDomain())

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
