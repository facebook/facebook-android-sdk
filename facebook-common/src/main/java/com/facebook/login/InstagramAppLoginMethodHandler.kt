/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.login

import android.os.Parcel
import android.os.Parcelable
import com.facebook.AccessTokenSource
import com.facebook.internal.NativeProtocol
import com.facebook.internal.ServerProtocol

internal class InstagramAppLoginMethodHandler : NativeAppLoginMethodHandler {

  constructor(loginClient: LoginClient) : super(loginClient)
  override val nameForLogging = "instagram_login"

  override val tokenSource: AccessTokenSource = AccessTokenSource.INSTAGRAM_APPLICATION_WEB

  override fun tryAuthorize(request: LoginClient.Request): Int {
    val e2e = LoginClient.getE2E()
    val intent =
        NativeProtocol.createInstagramIntent(
            loginClient.activity,
            request.applicationId,
            request.permissions,
            e2e,
            request.isRerequest,
            request.hasPublishPermission(),
            request.defaultAudience,
            getClientState(request.authId),
            request.authType,
            request.messengerPageId,
            request.resetMessengerState,
            request.isFamilyLogin,
            request.shouldSkipAccountDeduplication())

    addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e)

    val result = tryIntent(intent, LoginClient.getLoginRequestCode())
    return if (result) 1 else 0
  }

  constructor(source: Parcel) : super(source)

  override fun describeContents(): Int = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    super.writeToParcel(dest, flags)
  }

  companion object {
    @JvmField
    val CREATOR =
        object : Parcelable.Creator<InstagramAppLoginMethodHandler> {
          override fun createFromParcel(source: Parcel): InstagramAppLoginMethodHandler {
            return InstagramAppLoginMethodHandler(source)
          }

          override fun newArray(size: Int): Array<InstagramAppLoginMethodHandler?> {
            return arrayOfNulls(size)
          }
        }
  }
}
