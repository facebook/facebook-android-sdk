/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** An Exception indicating that an operation was canceled before it completed. */
class FacebookOperationCanceledException : FacebookException {
  /** Constructs a FacebookOperationCanceledException with no additional information. */
  constructor() : super()

  /**
   * Constructs a FacebookOperationCanceledException with a message.
   *
   * @param message A String to be returned from getMessage.
   */
  constructor(message: String?) : super(message)

  /**
   * Constructs a FacebookOperationCanceledException with a message and inner error.
   *
   * @param message A String to be returned from getMessage.
   * @param throwable A Throwable to be returned from getCause.
   */
  constructor(message: String?, throwable: Throwable?) : super(message, throwable)

  /**
   * Constructs a FacebookOperationCanceledException with an inner error.
   *
   * @param throwable A Throwable to be returned from getCause.
   */
  constructor(throwable: Throwable?) : super(throwable)

  companion object {
    const val serialVersionUID: Long = 1
  }
}
