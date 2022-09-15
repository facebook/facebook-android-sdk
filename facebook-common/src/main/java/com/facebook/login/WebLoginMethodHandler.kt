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

package com.facebook.login

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.text.TextUtils
import android.webkit.CookieSyncManager
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.AccessTokenSource
import com.facebook.FacebookException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getAutoLogAppEventsEnabled
import com.facebook.FacebookSdk.getSdkVersion
import com.facebook.FacebookServiceException
import com.facebook.appevents.AppEventsConstants
import com.facebook.internal.ServerProtocol
import com.facebook.internal.Utility.clearFacebookCookies
import com.facebook.internal.Utility.isNullOrEmpty

/** This class is for internal use. SDK users should not access it directly. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class WebLoginMethodHandler : LoginMethodHandler {
  private var e2e: String? = null
  abstract val tokenSource: AccessTokenSource

  internal constructor(loginClient: LoginClient) : super(loginClient)
  internal constructor(source: Parcel) : super(source)

  protected open fun getSSODevice(): String? = null

  protected open fun getParameters(request: LoginClient.Request): Bundle {
    val parameters = Bundle()
    if (!isNullOrEmpty(request.permissions)) {
      val scope = TextUtils.join(",", request.permissions)
      parameters.putString(ServerProtocol.DIALOG_PARAM_SCOPE, scope)
      addLoggingExtra(ServerProtocol.DIALOG_PARAM_SCOPE, scope)
    }
    val audience = request.defaultAudience ?: DefaultAudience.NONE
    parameters.putString(
        ServerProtocol.DIALOG_PARAM_DEFAULT_AUDIENCE, audience.nativeProtocolAudience)
    parameters.putString(ServerProtocol.DIALOG_PARAM_STATE, getClientState(request.authId))
    val previousToken = getCurrentAccessToken()
    val previousTokenString = previousToken?.token
    if (previousTokenString != null && previousTokenString == loadCookieToken()) {
      parameters.putString(ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, previousTokenString)
      // Don't log the actual access token, just its presence or absence.
      addLoggingExtra(
          ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, AppEventsConstants.EVENT_PARAM_VALUE_YES)
    } else {
      // The call to clear cookies will create the first instance of CookieSyncManager if
      // necessary
      loginClient.activity?.let { clearFacebookCookies(it) }
      addLoggingExtra(
          ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, AppEventsConstants.EVENT_PARAM_VALUE_NO)
    }
    parameters.putString(ServerProtocol.DIALOG_PARAM_CBT, System.currentTimeMillis().toString())
    parameters.putString(
        ServerProtocol.DIALOG_PARAM_IES, if (getAutoLogAppEventsEnabled()) "1" else "0")
    return parameters
  }

  protected open fun addExtraParameters(parameters: Bundle, request: LoginClient.Request): Bundle {
    parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, getRedirectUrl())
    if (request.isInstagramLogin) {
      parameters.putString(ServerProtocol.DIALOG_PARAM_APP_ID, request.applicationId)
    } else {
      // Client id is a legacy name. IG Login doesn't support it. This line is kept
      // for FB Login for consistency with old SDKs
      parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, request.applicationId)
    }
    parameters.putString(ServerProtocol.DIALOG_PARAM_E2E, LoginClient.getE2E())
    if (request.isInstagramLogin) {
      parameters.putString(
          ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE,
          ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SCOPES)
    } else {
      if (request.permissions.contains(LoginConfiguration.OPENID)) {
        parameters.putString(ServerProtocol.DIALOG_PARAM_NONCE, request.nonce)
      }
      parameters.putString(
          ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE,
          ServerProtocol.DIALOG_RESPONSE_TYPE_ID_TOKEN_AND_SIGNED_REQUEST)
    }

    // PKCE params
    parameters.putString(ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE, request.codeChallenge)
    parameters.putString(
        ServerProtocol.DIALOG_PARAM_CODE_CHALLENGE_METHOD, request.codeChallengeMethod?.name)

    parameters.putString(
        ServerProtocol.DIALOG_PARAM_RETURN_SCOPES, ServerProtocol.DIALOG_RETURN_SCOPES_TRUE)
    parameters.putString(ServerProtocol.DIALOG_PARAM_AUTH_TYPE, request.authType)
    parameters.putString(ServerProtocol.DIALOG_PARAM_LOGIN_BEHAVIOR, request.loginBehavior.name)
    parameters.putString(ServerProtocol.DIALOG_PARAM_SDK_VERSION, "android-${getSdkVersion()}")
    if (getSSODevice() != null) {
      parameters.putString(ServerProtocol.DIALOG_PARAM_SSO_DEVICE, getSSODevice())
    }
    parameters.putString(
        ServerProtocol.DIALOG_PARAM_CUSTOM_TABS_PREFETCHING,
        if (FacebookSdk.hasCustomTabsPrefetching) "1" else "0")
    if (request.isFamilyLogin) {
      parameters.putString(ServerProtocol.DIALOG_PARAM_FX_APP, request.loginTargetApp.toString())
    }
    if (request.shouldSkipAccountDeduplication()) {
      parameters.putString(ServerProtocol.DIALOG_PARAM_SKIP_DEDUPE, "true")
    }

    // Set Login Connect parameters if they are present
    if (request.messengerPageId != null) {
      parameters.putString(ServerProtocol.DIALOG_PARAM_MESSENGER_PAGE_ID, request.messengerPageId)
      parameters.putString(
          ServerProtocol.DIALOG_PARAM_RESET_MESSENGER_STATE,
          if (request.resetMessengerState) "1" else "0")
    }
    return parameters
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  open fun onComplete(request: LoginClient.Request, values: Bundle?, error: FacebookException?) {
    var outcome: LoginClient.Result
    val loginClient = loginClient
    e2e = null
    if (values != null) {
      // Actual e2e we got from the dialog should be used for logging.
      if (values.containsKey(ServerProtocol.DIALOG_PARAM_E2E)) {
        e2e = values.getString(ServerProtocol.DIALOG_PARAM_E2E)
      }
      try {
        val token =
            createAccessTokenFromWebBundle(
                request.permissions, values, tokenSource, request.applicationId)
        val authenticationToken = createAuthenticationTokenFromWebBundle(values, request.nonce)
        outcome =
            LoginClient.Result.createCompositeTokenResult(
                loginClient.pendingRequest, token, authenticationToken)

        // Ensure any cookies set by the dialog are saved
        // This is to work around a bug where CookieManager may fail to instantiate if
        // CookieSyncManager has never been created.
        if (loginClient.activity != null) {
          try {
            val syncManager = CookieSyncManager.createInstance(loginClient.activity)
            syncManager.sync()
          } catch (e: Exception) {
            // A crash happens in clearing the cookies. It's likely that webview is not available.
          }
          if (token != null) {
            saveCookieToken(token.token)
          }
        }
      } catch (ex: FacebookException) {
        outcome = LoginClient.Result.createErrorResult(loginClient.pendingRequest, null, ex.message)
      }
    } else {
      if (error is FacebookOperationCanceledException) {
        outcome =
            LoginClient.Result.createCancelResult(
                loginClient.pendingRequest, USER_CANCELED_LOG_IN_ERROR_MESSAGE)
      } else {
        // Something went wrong, don't log a completion event since it will skew timing
        // results.
        e2e = null
        var errorCode: String? = null
        var errorMessage = error?.message
        if (error is FacebookServiceException) {
          val requestError = error.requestError
          errorCode = requestError.errorCode.toString()
          errorMessage = requestError.toString()
        }
        outcome =
            LoginClient.Result.createErrorResult(
                loginClient.pendingRequest, null, errorMessage, errorCode)
      }
    }
    if (!isNullOrEmpty(e2e)) {
      logWebLoginCompleted(e2e)
    }
    loginClient.completeAndValidate(outcome)
  }

  private fun loadCookieToken(): String? {
    val context: Context = loginClient.activity ?: FacebookSdk.getApplicationContext()
    val sharedPreferences =
        context.getSharedPreferences(WEB_VIEW_AUTH_HANDLER_STORE, Context.MODE_PRIVATE)
    return sharedPreferences.getString(WEB_VIEW_AUTH_HANDLER_TOKEN_KEY, "")
  }

  private fun saveCookieToken(token: String) {
    val context: Context = loginClient.activity ?: FacebookSdk.getApplicationContext()
    context
        .getSharedPreferences(WEB_VIEW_AUTH_HANDLER_STORE, Context.MODE_PRIVATE)
        .edit()
        .putString(WEB_VIEW_AUTH_HANDLER_TOKEN_KEY, token)
        .apply()
  }

  companion object {
    private const val WEB_VIEW_AUTH_HANDLER_STORE =
        "com.facebook.login.AuthorizationClient.WebViewAuthHandler.TOKEN_STORE_KEY"
    private const val WEB_VIEW_AUTH_HANDLER_TOKEN_KEY = "TOKEN"
  }
}
