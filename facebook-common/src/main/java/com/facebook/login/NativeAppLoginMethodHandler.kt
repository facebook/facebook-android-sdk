/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Parcel
import androidx.annotation.VisibleForTesting
import com.facebook.AccessTokenSource
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getExecutor
import com.facebook.FacebookServiceException
import com.facebook.internal.NativeProtocol
import com.facebook.internal.ServerProtocol.getErrorConnectionFailure
import com.facebook.internal.ServerProtocol.getErrorsProxyAuthDisabled
import com.facebook.internal.ServerProtocol.getErrorsUserCanceled
import com.facebook.internal.Utility.isNullOrEmpty

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
abstract class NativeAppLoginMethodHandler : LoginMethodHandler {
  internal constructor(loginClient: LoginClient) : super(loginClient)
  internal constructor(source: Parcel) : super(source)

  /**
   * handle the success response from the initial request when user confirms on GDP
   *
   * @param request initial request
   * @param extras data returned from initial request
   */
  private fun processSuccessResponse(request: LoginClient.Request, extras: Bundle) {
    if (extras.containsKey("code") && !isNullOrEmpty(extras.getString("code"))) {
      // if contains "code" which mean this is code flow and need to exchange for token
      getExecutor().execute {
        try {
          val processedExtras = processCodeExchange(request, extras)
          handleResultOk(request, processedExtras)
        } catch (ex: FacebookServiceException) {
          val requestError = ex.requestError
          handleResultError(
              request,
              requestError.errorType,
              requestError.errorMessage,
              requestError.errorCode.toString())
        } catch (ex: FacebookException) {
          handleResultError(request, null, ex.message, null)
        }
      }
    } else {
      // Lightweight Login will go through this flow
      handleResultOk(request, extras)
    }
  }

  abstract override fun tryAuthorize(request: LoginClient.Request): Int

  open val tokenSource: AccessTokenSource = AccessTokenSource.FACEBOOK_APPLICATION_WEB

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    val request = loginClient.pendingRequest
    if (data == null) {
      // This happens if the user presses 'Back'.
      completeLogin(LoginClient.Result.createCancelResult(request, "Operation canceled"))
    } else if (resultCode == Activity.RESULT_CANCELED) {
      handleResultCancel(request, data)
    } else if (resultCode != Activity.RESULT_OK) {
      completeLogin(
          LoginClient.Result.createErrorResult(
              request, "Unexpected resultCode from authorization.", null))
    } else {
      val extras = data.extras
      if (extras == null) {
        completeLogin(
            LoginClient.Result.createErrorResult(
                request, "Unexpected null from returned authorization data.", null))
        return true
      }
      val error = getError(extras)
      val errorCode = extras["error_code"]?.toString()
      val errorMessage = getErrorMessage(extras)
      val e2e = extras.getString(NativeProtocol.FACEBOOK_PROXY_AUTH_E2E_KEY)
      if (!isNullOrEmpty(e2e)) {
        logWebLoginCompleted(e2e)
      }
      if (error == null && errorCode == null && errorMessage == null && request != null) {
        processSuccessResponse(request, extras)
      } else {
        handleResultError(request, error, errorMessage, errorCode)
      }
    }
    return true
  }

  private fun completeLogin(outcome: LoginClient.Result?) {
    if (outcome != null) {
      loginClient.completeAndValidate(outcome)
    } else {
      loginClient.tryNextHandler()
    }
  }

  protected open fun handleResultError(
      request: LoginClient.Request?,
      error: String?,
      errorMessage: String?,
      errorCode: String?
  ) {
    if (error != null && error == "logged_out") {
      CustomTabLoginMethodHandler.calledThroughLoggedOutAppSwitch = true
      completeLogin(null)
    } else if (getErrorsProxyAuthDisabled().contains(error)) {
      completeLogin(null)
    } else if (getErrorsUserCanceled().contains(error)) {
      completeLogin(LoginClient.Result.createCancelResult(request, null))
    } else {
      completeLogin(LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode))
    }
  }

  protected open fun handleResultOk(request: LoginClient.Request, extras: Bundle) {
    try {
      val token =
          createAccessTokenFromWebBundle(
              request.permissions, extras, tokenSource, request.applicationId)
      val authenticationToken = createAuthenticationTokenFromWebBundle(extras, request.nonce)
      completeLogin(
          LoginClient.Result.createCompositeTokenResult(request, token, authenticationToken))
    } catch (ex: FacebookException) {
      completeLogin(LoginClient.Result.createErrorResult(request, null, ex.message))
    }
  }

  protected open fun handleResultCancel(request: LoginClient.Request?, data: Intent) {
    val extras = data.extras
    val error = getError(extras)
    val errorCode: String? = extras?.get("error_code")?.toString()

    // If the device has lost network, the result will be a cancel with a connection failure
    // error. We want our consumers to be notified of this as an error so they can tell their
    // users to "reconnect and try again".
    if (getErrorConnectionFailure() == errorCode) {
      val errorMessage = getErrorMessage(extras)
      completeLogin(LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode))
      return
    }
    completeLogin(LoginClient.Result.createCancelResult(request, error))
  }

  protected open fun getError(extras: Bundle?): String? {
    return extras?.getString("error") ?: extras?.getString("error_type")
  }

  protected open fun getErrorMessage(extras: Bundle?): String? {
    return extras?.getString("error_message") ?: extras?.getString("error_description")
  }

  protected open fun tryIntent(intent: Intent?, requestCode: Int): Boolean {
    if (intent == null || !isCallable(intent)) {
      return false
    }

    val loginFragment = loginClient.fragment as? LoginFragment
    loginFragment?.launcher?.launch(intent) ?: return false

    return true
  }

  private fun isCallable(intent: Intent): Boolean {
    val list: List<ResolveInfo> =
        FacebookSdk.getApplicationContext()
            .packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return list.isNotEmpty()
  }
}
