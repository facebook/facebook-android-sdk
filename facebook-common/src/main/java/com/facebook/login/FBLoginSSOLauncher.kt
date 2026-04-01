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
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.FacebookCallback
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getIntentUriPackageTarget
import com.facebook.FacebookSdk.getRedirectURI
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.FeatureManager
import com.facebook.internal.Utility
import com.facebook.internal.NativeProtocol
import com.facebook.login.LoginMethodHandler.Companion.createAccessTokenFromWebBundle
import com.facebook.login.LoginMethodHandler.Companion.createAuthenticationTokenFromWebBundle
import java.util.UUID
import org.json.JSONObject

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
  @VisibleForTesting
  internal var appEventsLogger: InternalAppEventsLogger? = null
    get() {
      if (field == null) {
        field = InternalAppEventsLogger(activity, getApplicationId())
      }
      return field
    }

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
    val authId = UUID.randomUUID().toString()
    Utility.logd(TAG, "auth_logger_id: $authId")
    logSsoEvent(LoginLogger.EVENT_NAME_SSO_LAUNCH_ATTEMPTED, authId,
        JSONObject().put("permissions", TextUtils.join(",", permissions)))
    if (!FeatureManager.isEnabled(FeatureManager.Feature.LoginSSO)) {
      pendingSsoContext = "non_sso_login_sso_not_shown"
      logSsoEvent(LoginLogger.EVENT_NAME_SSO_NOT_SHOWN, authId,
          JSONObject().put("reason", "gk_disabled"))
      return false
    }
    val loginConfig = LoginConfiguration(permissions)
    val loginManager = LoginManager.getInstance()

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
        pendingSsoContext = "non_sso_login_sso_shown"
        logSsoEvent(LoginLogger.EVENT_NAME_SSO_DELEGATED_TO_FB4A, request.authId)
        launcher.launch(intent)
        return true
      }
    }
    // SSO activity not available — determine why
    val ssoContext = if (isFb4aInstalled()) "fb4a_outdated" else "fb4a_not_installed"
    if (showWithoutFBApp) {
      val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity ?: return false
      val noAppDialog = FBLoginSSONoAppDialog.newInstance()
      noAppDialog.onContinueListener = {
        logSsoEvent(LoginLogger.EVENT_NAME_SSO_CONTINUE_CLICKED, request.authId,
            JSONObject().put("reason", ssoContext))
        LoginManager.getInstance()
            .startLoginWithForceConfirmation(activity, pendingPermissions, ssoContext)
      }
      noAppDialog.onDismissListener = {
        logSsoEvent(LoginLogger.EVENT_NAME_SSO_DISMISSED, request.authId,
            result = LoginClient.Result.Code.CANCEL.loggingValue)
      }
      noAppDialog.show(fragmentActivity.supportFragmentManager, FBLoginSSONoAppDialog.TAG)
      logSsoEvent(LoginLogger.EVENT_NAME_SSO_SHOWN, request.authId,
          JSONObject().put("reason", ssoContext)
              .put("permissions", TextUtils.join(",", permissions)))
      return true
    }
    logSsoEvent(LoginLogger.EVENT_NAME_SSO_NOT_SHOWN, request.authId,
        JSONObject().put("reason", ssoContext))
    pendingSsoContext = "non_sso_login_sso_not_shown"
    return false
  }

  private fun handleActivityResult(resultCode: Int, data: Intent?) {
    val request = pendingRequest
    val nonce = pendingNonce

    // Check if the user dismissed the SSO popup without tapping Continue
    val ssoDismissed = data?.getBooleanExtra(SSO_DISMISSED_EXTRA, false) == true
    Utility.logd(TAG, "handleActivityResult: resultCode=$resultCode, sso_dismissed=$ssoDismissed")
    if (ssoDismissed) {
      Utility.logd(TAG, "SSO popup dismissed by user (not a login cancellation)")
      logSsoEvent(LoginLogger.EVENT_NAME_SSO_DISMISSED, request?.authId,
          result = LoginClient.Result.Code.CANCEL.loggingValue)
      callback?.onCancel()
      pendingNonce = null
      pendingRequest = null
      return
    }

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
            markFromSso(loginResult)
            resultIntent.putExtra(LoginFragment.RESULT_KEY, loginResult)
            LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
          } catch (ex: Exception) {
            val errorResult =
                LoginClient.Result.createErrorResult(request, null, ex.message)
            markFromSso(errorResult)
            resultIntent.putExtra(LoginFragment.RESULT_KEY, errorResult)
            LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
          }
        } else {
          // Error response from the SSO activity
          val errorResult =
              LoginClient.Result.createErrorResult(request, error, errorMessage, errorCode)
          markFromSso(errorResult)
          resultIntent.putExtra(LoginFragment.RESULT_KEY, errorResult)
          LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
        }
      } else {
        // RESULT_OK but null extras — treat as error
        val errorResult =
            LoginClient.Result.createErrorResult(
                request, null, "Unexpected null extras from SSO activity.")
        markFromSso(errorResult)
        resultIntent.putExtra(LoginFragment.RESULT_KEY, errorResult)
        LoginManager.getInstance().onActivityResult(Activity.RESULT_OK, resultIntent, callback)
      }
    } else {
      // Canceled or no data — user cancelled the OAuth dialog after tapping Continue
      Utility.logd(TAG, "Login cancelled (not SSO dismiss — OAuth dialog was shown)")
      val cancelResult = LoginClient.Result.createCancelResult(request, "Operation canceled")
      markFromSso(cancelResult)
      resultIntent.putExtra(LoginFragment.RESULT_KEY, cancelResult)
      LoginManager.getInstance()
          .onActivityResult(Activity.RESULT_CANCELED, resultIntent, callback)
    }

    pendingNonce = null
    pendingRequest = null
  }

  private fun logSsoEvent(
      eventName: String,
      authLoggerId: String?,
      extras: JSONObject? = null,
      result: String? = null
  ) {
    val bundle = Bundle()
    bundle.putLong(LoginLogger.EVENT_PARAM_TIMESTAMP, System.currentTimeMillis())
    bundle.putString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID, authLoggerId ?: "")
    bundle.putString(LoginLogger.EVENT_PARAM_LOGIN_RESULT, result ?: "")
    bundle.putString(LoginLogger.EVENT_PARAM_METHOD, "")
    bundle.putString(LoginLogger.EVENT_PARAM_ERROR_CODE, "")
    bundle.putString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE, "")
    bundle.putString(LoginLogger.EVENT_PARAM_EXTRAS, extras?.toString() ?: "")
    bundle.putString(LoginLogger.EVENT_PARAM_CHALLENGE, "")
    Utility.logd(TAG, "Event: $eventName | " +
        "auth_logger_id=${authLoggerId ?: ""} | " +
        "result=${result ?: ""} | " +
        "extras=${extras?.toString() ?: ""}")
    appEventsLogger?.logEventImplicitly(eventName, bundle)
  }

  private fun markFromSso(result: LoginClient.Result) {
    val extras = result.loggingExtras?.toMutableMap() ?: HashMap()
    extras[LoginLogger.EVENT_EXTRAS_FROM_SSO] = "true"
    result.loggingExtras = extras
  }

  private fun isFb4aInstalled(): Boolean {
    return try {
      activity.packageManager.getPackageInfo("com.facebook.katana", 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      false
    }
  }

  companion object {
    private const val TAG = "FBLoginSSOLauncher"
    private const val SSO_DISMISSED_EXTRA = "sso_dismissed"

    /** Set after launch() is called; consumed by LoginManager on the next logIn() call. */
    @JvmStatic
    var pendingSsoContext: String? = null
      internal set
  }
}
