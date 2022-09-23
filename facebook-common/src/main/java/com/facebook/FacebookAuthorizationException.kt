/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** An Exception indicating that Login failed. */
class FacebookAuthorizationException : FacebookException {
  /** Constructs a FacebookAuthorizationException with no additional information. */
  constructor() : super() {}

  /**
   * Constructs a FacebookAuthorizationException with a message.
   *
   * @param message A String to be returned from getMessage.
   */
  constructor(message: String?) : super(message) {}

  /**
   * Constructs a FacebookAuthorizationException with a message and inner error.
   *
   * @param message A String to be returned from getMessage.
   * @param throwable A Throwable to be returned from getCause.
   */
  constructor(message: String?, throwable: Throwable?) : super(message, throwable) {}

  /**
   * Constructs a FacebookAuthorizationException with an inner error.
   *
   * @param throwable A Throwable to be returned from getCause.
   */
  constructor(throwable: Throwable?) : super(throwable) {}

  companion object {
    const val serialVersionUID: Long = 1
  }
}
