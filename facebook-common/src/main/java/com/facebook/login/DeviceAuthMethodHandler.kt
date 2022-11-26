/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import java.lang.Exception
import java.util.Date
import java.util.concurrent.ScheduledThreadPoolExecutor

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class DeviceAuthMethodHandler : LoginMethodHandler {
  constructor(loginClient: LoginClient) : super(loginClient)
  protected constructor(parcel: Parcel) : super(parcel)

  override fun tryAuthorize(request: LoginClient.Request): Int {
    showDialog(request)
    return 1
  }

  private fun showDialog(request: LoginClient.Request) {
    val activity = loginClient.activity
    if (activity == null || activity.isFinishing) {
      return
    }
    val dialog = createDeviceAuthDialog()
    dialog.show(activity.supportFragmentManager, "login_with_facebook")
    dialog.startLogin(request)
  }

  protected open fun createDeviceAuthDialog(): DeviceAuthDialog {
    return DeviceAuthDialog()
  }

  /** Invoke it when the user cancels the login. */
  open fun onCancel() {
    val outcome =
        LoginClient.Result.createCancelResult(loginClient.pendingRequest, "User canceled log in.")
    loginClient.completeAndValidate(outcome)
  }

  /** Invoke it when an error is received. */
  open fun onError(ex: Exception) {
    val outcome = LoginClient.Result.createErrorResult(loginClient.pendingRequest, null, ex.message)
    loginClient.completeAndValidate(outcome)
  }

  /** Invoke it when the login flow succeeds and the access token is returned. */
  open fun onSuccess(
      accessToken: String,
      applicationId: String,
      userId: String,
      permissions: Collection<String?>?,
      declinedPermissions: Collection<String?>?,
      expiredPermissions: Collection<String?>?,
      accessTokenSource: AccessTokenSource?,
      expirationTime: Date?,
      lastRefreshTime: Date?,
      dataAccessExpirationTime: Date?
  ) {
    val token =
        AccessToken(
            accessToken,
            applicationId,
            userId,
            permissions,
            declinedPermissions,
            expiredPermissions,
            accessTokenSource,
            expirationTime,
            lastRefreshTime,
            dataAccessExpirationTime)
    val outcome = LoginClient.Result.createTokenResult(loginClient.pendingRequest, token)
    loginClient.completeAndValidate(outcome)
  }

  override val nameForLogging: String = "device_auth"

  override fun describeContents(): Int = 0

  companion object {
    private lateinit var backgroundExecutor: ScheduledThreadPoolExecutor

    @JvmStatic
    @Synchronized
    fun getBackgroundExecutor(): ScheduledThreadPoolExecutor {
      if (!this::backgroundExecutor.isInitialized) {
        backgroundExecutor = ScheduledThreadPoolExecutor(1)
      }
      return backgroundExecutor
    }

    @JvmField
    val CREATOR: Parcelable.Creator<DeviceAuthMethodHandler> =
        object : Parcelable.Creator<DeviceAuthMethodHandler> {
          override fun createFromParcel(source: Parcel): DeviceAuthMethodHandler {
            return DeviceAuthMethodHandler(source)
          }

          override fun newArray(size: Int): Array<DeviceAuthMethodHandler?> {
            return arrayOfNulls(size)
          }
        }
  }
}
