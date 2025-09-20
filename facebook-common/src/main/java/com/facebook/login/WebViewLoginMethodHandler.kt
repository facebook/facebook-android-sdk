/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.facebook.AccessTokenSource
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookDialogFragment
import com.facebook.internal.ServerProtocol
import com.facebook.internal.ServerProtocol.getDialogAuthority
import com.facebook.internal.ServerProtocol.getInstagramDialogAuthority
import com.facebook.internal.Utility
import com.facebook.internal.Utility.buildUri
import com.facebook.internal.WebDialog

/** This class is for internal use. SDK users should not access it directly. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class WebViewLoginMethodHandler : WebLoginMethodHandler {
  var loginDialog: WebDialog? = null
  var e2e: String? = null

  constructor(loginClient: LoginClient) : super(loginClient)

  override val nameForLogging = "web_view"

  override val tokenSource: AccessTokenSource = AccessTokenSource.WEB_VIEW

  override fun needsInternetPermission(): Boolean = true

  override fun cancel() {
    if (loginDialog != null) {
      loginDialog?.cancel()
      loginDialog = null
    }
  }

  override fun tryAuthorize(request: LoginClient.Request): Int {
    var parameters = getParameters(request)
    parameters = addExtraParameters(parameters, request)

    val listener =
        object : WebDialog.OnCompleteListener {
          override fun onComplete(values: Bundle?, error: FacebookException?) =
              onWebDialogComplete(request, values, error)
        }

    e2e = LoginClient.getE2E()
    addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e)

    val fragmentActivity = loginClient.activity ?: return 0
    val isChromeOS = Utility.isChromeOS(fragmentActivity)

    val builder =
        AuthDialogBuilder(fragmentActivity, request.applicationId, parameters)
            .setE2E(e2e as String)
            .setIsChromeOS(isChromeOS)
            .setAuthType(request.authType)
            .setLoginBehavior(request.loginBehavior)
            .setLoginTargetApp(request.loginTargetApp)
            .setFamilyLogin(request.isFamilyLogin)
            .setShouldSkipDedupe(request.shouldSkipAccountDeduplication())
            .setOnCompleteListener(listener)
    loginDialog = builder.build()

    val dialogFragment = FacebookDialogFragment()
    dialogFragment.retainInstance = true
    dialogFragment.innerDialog = loginDialog
    dialogFragment.show(fragmentActivity.getSupportFragmentManager(), FacebookDialogFragment.TAG)

    return 1
  }

  fun onWebDialogComplete(
      request: LoginClient.Request,
      values: Bundle?,
      error: FacebookException?
  ) {
    super.onComplete(request, values, error)
  }

  inner class AuthDialogBuilder : WebDialog.Builder {

    private var redirect_uri = ServerProtocol.DIALOG_REDIRECT_URI
    private var loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK
    private var targetApp = LoginTargetApp.FACEBOOK
    private var isFamilyLogin = false
    private var shouldSkipDedupe = false

    lateinit var e2e: String
    lateinit var authType: String

    constructor(
        context: Context,
        applicationId: String,
        parameters: Bundle
    ) : super(context, applicationId, OAUTH_DIALOG, parameters)

    fun setE2E(e2e: String): AuthDialogBuilder {
      this.e2e = e2e
      return this
    }

    /**
     * @deprecated This is no longer used
     * @return the AuthDialogBuilder
     */
    fun setIsRerequest(isRerequest: Boolean): AuthDialogBuilder = this

    fun setIsChromeOS(isChromeOS: Boolean): AuthDialogBuilder {
      this.redirect_uri =
          if (isChromeOS) ServerProtocol.DIALOG_REDIRECT_CHROME_OS_URI
          else ServerProtocol.DIALOG_REDIRECT_URI
      return this
    }

    fun setAuthType(authType: String): AuthDialogBuilder {
      this.authType = authType
      return this
    }

    fun setLoginBehavior(loginBehavior: LoginBehavior): AuthDialogBuilder {
      this.loginBehavior = loginBehavior
      return this
    }

    fun setLoginTargetApp(targetApp: LoginTargetApp): AuthDialogBuilder {
      this.targetApp = targetApp
      return this
    }

    fun setFamilyLogin(isFamilyLogin: Boolean): AuthDialogBuilder {
      this.isFamilyLogin = isFamilyLogin
      return this
    }

    fun setShouldSkipDedupe(shouldSkip: Boolean): AuthDialogBuilder {
      this.shouldSkipDedupe = shouldSkip
      return this
    }

    override fun build(): WebDialog {
      val parameters = this.parameters as Bundle

      // Check if custom redirect URI was provided
      val customRedirectUri = parameters.getString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI)

      // Only set redirect_uri if it wasn't already provided (preserves custom redirect URI from addExtraParameters)
      if (!parameters.containsKey(ServerProtocol.DIALOG_PARAM_REDIRECT_URI)) {
        parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, this.redirect_uri)
      }

      parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, this.applicationId)
      parameters.putString(ServerProtocol.DIALOG_PARAM_E2E, this.e2e)
      parameters.putString(
          ServerProtocol.DIALOG_PARAM_RESPONSE_TYPE,
          if (this.targetApp == LoginTargetApp.INSTAGRAM)
              ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SCOPES
          else ServerProtocol.DIALOG_RESPONSE_TYPE_TOKEN_AND_SIGNED_REQUEST)
      parameters.putString(
          ServerProtocol.DIALOG_PARAM_RETURN_SCOPES, ServerProtocol.DIALOG_RETURN_SCOPES_TRUE)
      parameters.putString(ServerProtocol.DIALOG_PARAM_AUTH_TYPE, this.authType)
      parameters.putString(ServerProtocol.DIALOG_PARAM_LOGIN_BEHAVIOR, this.loginBehavior.name)
      if (this.isFamilyLogin) {
        parameters.putString(ServerProtocol.DIALOG_PARAM_FX_APP, this.targetApp.toString())
      }
      if (this.shouldSkipDedupe) {
        parameters.putString(ServerProtocol.DIALOG_PARAM_SKIP_DEDUPE, "true")
      }

      // Create a custom WebDialog that respects our redirect URI
      return if (!customRedirectUri.isNullOrEmpty() && customRedirectUri != ServerProtocol.DIALOG_REDIRECT_URI) {
        CustomRedirectWebDialog.create(this.context as Context, OAUTH_DIALOG, parameters, theme, this.targetApp, listener, customRedirectUri)
      } else {
        WebDialog.newInstance(this.context as Context, OAUTH_DIALOG, parameters, theme, this.targetApp, listener)
      }
    }
  }

  /**
   * Custom WebDialog that properly handles custom redirect URIs
   */
  private class CustomRedirectWebDialog(
      context: Context,
      url: String,
      private val customRedirectUri: String
  ) : WebDialog(context, url) {

    init {
      // Set the custom redirect URI as the expected one
      setExpectedRedirectUrl(customRedirectUri)
    }

    companion object {
      fun create(
          context: Context,
          action: String?,
          parameters: Bundle?,
          theme: Int,
          targetApp: LoginTargetApp,
          listener: OnCompleteListener?,
          customRedirectUri: String
      ): CustomRedirectWebDialog {
        // Build the URL with our custom parameters
        val modifiedParameters = Bundle(parameters ?: Bundle())
        modifiedParameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, customRedirectUri)
        modifiedParameters.putString(ServerProtocol.DIALOG_PARAM_DISPLAY, "touch")
        modifiedParameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, FacebookSdk.getApplicationId())
        modifiedParameters.putString(
            ServerProtocol.DIALOG_PARAM_SDK_VERSION,
            "android-${FacebookSdk.getSdkVersion()}")

        val uri = when (targetApp) {
          LoginTargetApp.INSTAGRAM ->
              buildUri(
                  getInstagramDialogAuthority(),
                  ServerProtocol.INSTAGRAM_OAUTH_PATH,
                  modifiedParameters)
          else ->
              buildUri(
                  getDialogAuthority(),
                  FacebookSdk.getGraphApiVersion() + "/" + ServerProtocol.DIALOG_PATH + action,
                  modifiedParameters)
        }

        val dialog = CustomRedirectWebDialog(context, uri.toString(), customRedirectUri)
        if (theme != 0) {
          // Note: Custom theme handling would need to be implemented if needed
        }
        dialog.onCompleteListener = listener
        return dialog
      }
    }
  }

  companion object {
    private const val OAUTH_DIALOG = "oauth"
    @JvmField
    val CREATOR =
        object : Parcelable.Creator<WebViewLoginMethodHandler> {

          override fun createFromParcel(source: Parcel): WebViewLoginMethodHandler {
            return WebViewLoginMethodHandler(source)
          }

          override fun newArray(size: Int): Array<WebViewLoginMethodHandler?> {
            return arrayOfNulls(size)
          }
        }
  }

  constructor(source: Parcel) : super(source) {
    this.e2e = source.readString()
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    super.writeToParcel(dest, flags)
    dest.writeString(this.e2e)
  }
}
