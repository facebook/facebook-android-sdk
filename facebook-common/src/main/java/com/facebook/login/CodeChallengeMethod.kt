/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

enum class CodeChallengeMethod(s: String = "S256") {
  S256("S256"),
  PLAIN("plain")
}
