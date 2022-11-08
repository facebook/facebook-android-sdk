/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.net.Uri
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

/** This class manages device login and permissions for Facebook. */
@AutoHandleExceptions
class DeviceLoginManager : LoginManager() {

  /**
   * Uri to redirect the user to after they complete the device login flow on the external device.
   *
   * The Uri must be configured in your App Settings -> Advanced -> OAuth Redirect URIs.
   */
  var deviceRedirectUri: Uri? = null

  /** Target user id for the device request, if any. */
  var deviceAuthTargetUserId: String? = null

  override fun createLoginRequest(permissions: Collection<String>?): LoginClient.Request {
    val request = super.createLoginRequest(permissions)
    deviceRedirectUri?.let { request.deviceRedirectUriString = it.toString() }
    deviceAuthTargetUserId?.let { request.deviceAuthTargetUserId = it }
    return request
  }

  companion object {
    val instance: DeviceLoginManager by lazy { DeviceLoginManager() }
  }
}
