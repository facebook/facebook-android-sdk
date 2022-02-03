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
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.internal.CustomTabUtils.getChromePackage
import com.facebook.internal.NativeProtocol.createProxyAuthIntents
import com.facebook.internal.ServerProtocol

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class KatanaProxyLoginMethodHandler : NativeAppLoginMethodHandler {
  constructor(loginClient: LoginClient?) : super(loginClient)

  override val nameForLogging = "katana_proxy_auth"

  override fun tryAuthorize(request: LoginClient.Request): Int {
    val behavior = request.loginBehavior
    val ignoreAppSwitchToLoggedOut =
        (FacebookSdk.ignoreAppSwitchToLoggedOut &&
            getChromePackage() != null &&
            behavior.allowsCustomTabAuth())
    val e2e = LoginClient.getE2E()
    val intents =
        createProxyAuthIntents(
            loginClient.activity,
            request.applicationId,
            request.permissions,
            e2e,
            request.isRerequest,
            request.hasPublishPermission(),
            request.defaultAudience,
            getClientState(request.authId),
            request.authType,
            ignoreAppSwitchToLoggedOut,
            request.messengerPageId,
            request.resetMessengerState,
            request.isFamilyLogin,
            request.shouldSkipAccountDeduplication(),
            request.nonce,
            request.codeChallenge,
            request.codeChallengeMethod?.name)
    addLoggingExtra(ServerProtocol.DIALOG_PARAM_E2E, e2e)
    for ((i, intent) in intents.withIndex()) {
      val launchedIntent = tryIntent(intent, LoginClient.getLoginRequestCode())
      if (launchedIntent) {
        return i + 1
      }
    }
    return 0
  }

  override fun shouldKeepTrackOfMultipleIntents(): Boolean = true

  constructor(source: Parcel?) : super(source)

  override fun describeContents(): Int = 0

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<KatanaProxyLoginMethodHandler> =
        object : Parcelable.Creator<KatanaProxyLoginMethodHandler> {
          override fun createFromParcel(source: Parcel): KatanaProxyLoginMethodHandler {
            return KatanaProxyLoginMethodHandler(source)
          }

          override fun newArray(size: Int): Array<KatanaProxyLoginMethodHandler?> {
            return arrayOfNulls(size)
          }
        }
  }
}
