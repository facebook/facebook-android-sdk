/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.requests

import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.GraphRequest
import com.facebook.GraphRequest.Companion.newGraphPathRequest
import com.facebook.HttpMethod

object PermissionRequest {
  private const val PERMISSIONS_ENDPOINT = "/me/permissions"
  private const val APP = "app"

  @JvmStatic
  fun makeRevokePermRequest(permission: String, callback: GraphRequest.Callback?) {
    val graphPath =
        if (permission == APP) {
          PERMISSIONS_ENDPOINT
        } else {
          "$PERMISSIONS_ENDPOINT/$permission"
        }
    val request = newGraphPathRequest(getCurrentAccessToken(), graphPath, callback)
    request.httpMethod = HttpMethod.DELETE
    request.executeAsync()
  }
}
