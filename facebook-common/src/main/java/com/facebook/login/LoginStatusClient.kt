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

internal class LoginStatusClient(
    context: Context,
    applicationId: String,
    redirectURI: String,
    private val loggerRef: String,
    private val graphApiVersion: String,
    private val toastDurationMs: Long,
    nonce: String?
) :
    PlatformServiceClient(
        context,
        NativeProtocol.MESSAGE_GET_LOGIN_STATUS_REQUEST,
        NativeProtocol.MESSAGE_GET_LOGIN_STATUS_REPLY,
        NativeProtocol.PROTOCOL_VERSION_20170411,
        applicationId,
        redirectURI,
        nonce) {
  override fun populateRequestBundle(data: Bundle) {
    data.putString(NativeProtocol.EXTRA_LOGGER_REF, loggerRef)
    data.putString(NativeProtocol.EXTRA_GRAPH_API_VERSION, graphApiVersion)
    data.putLong(NativeProtocol.EXTRA_TOAST_DURATION_MS, toastDurationMs)
  }

  companion object {
    const val DEFAULT_TOAST_DURATION_MS = 5000L
    internal fun newInstance(
        context: Context,
        applicationId: String,
        redirectURI: String,
        loggerRef: String,
        graphApiVersion: String,
        toastDurationMs: Long,
        nonce: String?
    ): LoginStatusClient =
        LoginStatusClient(
            context, applicationId, redirectURI, loggerRef, graphApiVersion, toastDurationMs, nonce)
  }
}
