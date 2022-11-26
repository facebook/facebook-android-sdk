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
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.internal.CustomTabUtils.getChromePackage
import com.facebook.internal.NativeProtocol.createProxyAuthIntents
import com.facebook.internal.ServerProtocol

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class KatanaProxyLoginMethodHandler : NativeAppLoginMethodHandler {
  constructor(loginClient: LoginClient) : super(loginClient)

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
            request.defaultAudience ?: DefaultAudience.NONE,
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

  constructor(source: Parcel) : super(source)

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
