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
import com.facebook.internal.FacebookDialogFragment
import com.facebook.internal.ServerProtocol
import com.facebook.internal.Utility
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
    val parameters = getParameters(request)

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
            .setHttpsRedirectURI(request.redirectURI)
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
    private var httpsRedirectURI: String? = null

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

    fun setHttpsRedirectURI(httpsRedirectURI: String?): AuthDialogBuilder {
      this.httpsRedirectURI = httpsRedirectURI
      return this
    }

    override fun build(): WebDialog {
      val parameters = this.parameters as Bundle
      parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, this.redirect_uri)
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

      // Set HTTP Redirect URI param if it was configured in the application
      if (!this.httpsRedirectURI.isNullOrEmpty()) {
        parameters.putString("https_redirect_uri", this.httpsRedirectURI)
      }

      return WebDialog.newInstance(
          this.context as Context, OAUTH_DIALOG, parameters, theme, this.targetApp, listener)
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
