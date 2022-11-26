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
import com.facebook.AccessTokenSource
import com.facebook.FacebookSdk
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
            loginClient.activity ?: FacebookSdk.getApplicationContext(),
            request.applicationId,
            request.permissions,
            e2e,
            request.isRerequest,
            request.hasPublishPermission(),
            request.defaultAudience ?: DefaultAudience.NONE,
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
