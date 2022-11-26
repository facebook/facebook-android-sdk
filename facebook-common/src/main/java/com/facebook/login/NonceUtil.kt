/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

object NonceUtil {
  @JvmStatic
  fun isValidNonce(nonce: String?): Boolean {
    if (nonce.isNullOrEmpty()) {
      return false
    }

    val hasWhiteSpace = nonce.indexOf(' ') >= 0
    return !hasWhiteSpace
  }
}
