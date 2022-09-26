/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fbloginsample.requests

import android.os.Bundle
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.GraphRequest
import com.facebook.HttpMethod.GET

object UserRequest {
  private const val ME_ENDPOINT = "/me"

  fun makeUserRequest(callback: GraphRequest.Callback?) {
    val params = Bundle()
    params.putString("fields", "picture,name,id,email,permissions")
    val request = GraphRequest(getCurrentAccessToken(), ME_ENDPOINT, params, GET, callback)
    request.executeAsync()
  }
}
