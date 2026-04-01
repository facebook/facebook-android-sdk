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
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.FacebookCallback
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getIntentUriPackageTarget
import com.facebook.FacebookSdk.getRedirectURI
import com.facebook.internal.FeatureManager
import com.facebook.internal.NativeProtocol
import com.facebook.login.LoginMethodHandler.Companion.createAccessTokenFromWebBundle
import com.facebook.login.LoginMethodHandler.Companion.createAuthenticationTokenFromWebBundle
import java.util.UUID

/**
 * Launches the FBLoginSSO activity and handles the result via AndroidX ActivityResult APIs.
 *
 * This class owns the full lifecycle: launching the SSO activity, receiving the result via
 * [ActivityResultLauncher], parsing raw bundle extras into tokens, and delegating to
 * [LoginManager.onActivityResult] for token persistence and callback dispatch.
 *
 * Must be instantiated during or before the activity's `CREATED` lifecycle state (i.e., early in
 * `onCreate`) because [registerForActivityResult] requires it.
 *
 * @param activity The [ComponentActivity] from which to launch the SSO flow.
 * @param callback An optional [FacebookCallback] that will receive login results.
 * @param showWithoutFBApp Whether to show the SSO no-app dialog when the Facebook app is not
 *   installed on the device. Defaults to true.
 */
class FBLoginSSOLauncher @JvmOverloads constructor(
    private val activity: ComponentActivity,
    private val callback: FacebookCallback<LoginResult>? = null,
    private val showWithoutFBApp: Boolean = true
) {
  private val launcher: ActivityResultLauncher<Intent>
  private var pendingNonce: String? = null
  private var pendingRequest: LoginClient.Request? = null
  private var pendingPermissions: Collection<String> = emptyList()

  init {
    launcher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
          handleActivityResult(result.resultCode, result.data)
        }
  }

  /**
   * Launches the FBLoginSSO activity if available on the device.
   *
   * Creates a fresh nonce and [LoginClient.Request], constructs the appropriate intents using
   * [NativeProtocol.createFBLoginSSOIntents], and launches the first resolvable one.
   *
   * @param permissions The permissions to request. Defaults to empty.
   * @return true if the SSO activity was launched, false if it is not available on this device.
   */
  @JvmOverloads
  fun launch(permissions: Collection<String> = emptyList()): Boolean {
    pendingPermissions = permissions
    if (AccessToken.getCurrentAccessToken() != null) {
      return false
    }
    if (!FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)) {
      return false
    }
    val loginConfig = LoginConfiguration(permissions)
    val loginManager = LoginManager.getInstance()
    val authId = UUID.randomUUID().toString()

    val request =
        LoginClient.Request(
            loginManager.loginBehavior,
            loginConfig.permissions,
            loginManager.defaultAudience,
            loginManager.authType,
            getApplicationId(),
            authId,
            loginManager.loginTargetApp,
            loginConfig.nonce,
            redirectURI = getRedirectURI(),
            intentUriPackageTarget = getIntentUriPackageTarget())
    request.isFamilyLogin = loginManager.isFamilyLogin
    request.setShouldSkipAccountDeduplication(loginManager.shouldSkipAccountDeduplication)

    pendingNonce = loginConfig.nonce
    pendingRequest = request

    val state = "{\"0_auth_logger_id\":\"$authId\"}"
    val intents =
        NativeProtocol.createFBLoginSSOIntents(
            activity,
            getApplicationId(),
            loginConfig.permissions,
            LoginClient.getE2E(),
            false,
            false,
            loginManager.defaultAudience,
            state,
            loginManager.authType,
            false,
            null,
            false,
            loginManager.isFamilyLogin,
            loginManager.shouldSkipAccountDeduplication,
            loginConfig.nonce,
            null,
            null,
            getRedirectURI(),
            getIntentUriPackageTarget())
    for (intent in intents) {
      val resolved = activity.packageManager.resolveActivity(intent, 0)
      if (resolved != null) {
        launcher.launch(intent)
        return true
      }
    }
    if (showWithoutFBApp) {
      val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity ?: return false
      val noAppDialog = FBLoginSSONoAppDialog.newInstance()
      noAppDialog.onContinueListener = {
        LoginManager.getInstance().logInWithReadPermissions(activity, pendingPermissions)
      }
      noAppDialog.show(fragmentActivity.supportFragmentManager, FBLoginSSONoAppDialog.TAG)
      return true
    }
    return false
  }

  private fun handleActivityResult(resultCode: Int, data: Intent?) {
    val request = pendingRequest
    val nonce = pendingNonce

    val resultIntent = Intent()

    if (resultCode == Activity.RESULT_OK && data != null) {
      val extras = data.extras
      if (extras != null) {
        val error = extras.getString("error") ?: extras.getString("error_type")
        val errorCode = extras["error_code"]?.toString()
        val errorMessage =
            extras.getString("error_message") ?: extras.getString("error_description")

        if (error == null && errorCode == null && errorMessage == null) {
          // Success — parse tokens from the bundle
          try {
            val accessToken =
                createAccessTokenFromWebBundle(
                    request?.permissions,
                    extras,
                    AccessTokenSource.FACEBOOK_APPLICATION_WEB,
                    getApplicationId())
            val authenticationToken = createAuthenticationTokenFromWebBundle(extras, nonce)
            val loginResult =
                LoginClient.Result.createCompositeTokenResult(
                    request, accessToken, authenticationToken)
            resultIntent.putExtra(LoginFragment.RESULT_KEY, loginResult)
            LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
          } catch (ex: Exception) {
            val errorResult =
                LoginClient.Result.createErrorResult(request, null, ex.message)
            resultIntent.putExtra(LoginFragment.RESULT_KEY, errorResult)
            LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
          }
        } else {
          // Error response from the SSO activity
          val errorResult =
              LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode)
          resultIntent.putExtra(LoginFragment.RESULT_KEY, errorResult)
          LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
        }
      } else {
        // RESULT_OK but null extras — treat as error
        val errorResult =
            LoginClient.Result.createErrorResult(
                request, null, "Unexpected null extras from SSO activity.")
        resultIntent.putExtra(LoginFragment.RESULT_KEY, errorResult)
        LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
      }
    } else {
      // Canceled or no data
      val cancelResult = LoginClient.Result.createCancelResult(request, "Operation canceled")
      resultIntent.putExtra(LoginFragment.RESULT_KEY, cancelResult)
      LoginManager.getInstance()
          .onActivityResult(Activity.RESULT_CANCELED, resultIntent, callback)
    }

    pendingNonce = null
    pendingRequest = null
  }
}
