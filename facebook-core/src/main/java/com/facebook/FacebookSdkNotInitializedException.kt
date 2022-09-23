/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** An Exception indicating that the Facebook SDK has not been correctly initialized. */
class FacebookSdkNotInitializedException : FacebookException {
  /** Constructs a FacebookSdkNotInitializedException with no additional information. */
  constructor() : super() {}

  /**
   * Constructs a FacebookSdkNotInitializedException with a message.
   *
   * @param message A String to be returned from getMessage.
   */
  constructor(message: String?) : super(message) {}

  /**
   * Constructs a FacebookSdkNotInitializedException with a message and inner error.
   *
   * @param message A String to be returned from getMessage.
   * @param throwable A Throwable to be returned from getCause.
   */
  constructor(message: String?, throwable: Throwable?) : super(message, throwable) {}

  /**
   * Constructs a FacebookSdkNotInitializedException with an inner error.
   *
   * @param throwable A Throwable to be returned from getCause.
   */
  constructor(throwable: Throwable?) : super(throwable) {}

  companion object {
    const val serialVersionUID: Long = 1
  }
}
