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
        LoginClient.Result.createCancelResult(
            loginClient.getPendingRequest(), "User canceled log in.")
    loginClient.completeAndValidate(outcome)
  }

  /** Invoke it when an error is received. */
  open fun onError(ex: Exception) {
    val outcome =
        LoginClient.Result.createErrorResult(loginClient.getPendingRequest(), null, ex.message)
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
    val outcome = LoginClient.Result.createTokenResult(loginClient.getPendingRequest(), token)
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
