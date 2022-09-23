/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.content.Context
import android.os.Bundle
import com.facebook.internal.NativeProtocol
import com.facebook.internal.PlatformServiceClient

internal class GetTokenClient(context: Context, request: LoginClient.Request) :
    PlatformServiceClient(
        context,
        NativeProtocol.MESSAGE_GET_ACCESS_TOKEN_REQUEST,
        NativeProtocol.MESSAGE_GET_ACCESS_TOKEN_REPLY,
        NativeProtocol.PROTOCOL_VERSION_20121101,
        request.applicationId,
        request.nonce) {
  override fun populateRequestBundle(data: Bundle) = Unit
}
