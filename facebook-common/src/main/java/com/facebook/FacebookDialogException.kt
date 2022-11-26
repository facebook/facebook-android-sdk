/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** Represents an error condition relating to displaying a Facebook Web dialog. */
class FacebookDialogException
/** Constructs a new FacebookException. */
(
    message: String?,
    /**
     * The error code received by the WebView. See:
     * http://developer.android.com/reference/android/webkit/WebViewClient.html
     */
    val errorCode: Int,
    /** The URL that the dialog was trying to load. */
    val failingUrl: String?
) : FacebookException(message) {
  override fun toString(): String {
    return StringBuilder()
        .append("{FacebookDialogException: ")
        .append("errorCode: ")
        .append(errorCode)
        .append(", message: ")
        .append(message)
        .append(", url: ")
        .append(failingUrl)
        .append("}")
        .toString()
  }

  companion object {
    const val serialVersionUID: Long = 1
  }
}
