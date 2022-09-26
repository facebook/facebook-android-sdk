/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.callbacks

import com.facebook.GraphRequest
import com.facebook.GraphResponse

class PermissionCallback(caller: IPermissionResponse) {
  interface IPermissionResponse {
    fun onCompleted(response: GraphResponse?)
  }

  val callback = GraphRequest.Callback { response: GraphResponse? -> caller.onCompleted(response) }
}
