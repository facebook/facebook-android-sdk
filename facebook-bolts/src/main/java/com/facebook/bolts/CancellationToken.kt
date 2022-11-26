/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import java.util.Locale
import java.util.concurrent.CancellationException

/**
 * Propagates notification that operations should be canceled.
 *
 * Create an instance of `CancellationTokenSource` and pass the token returned from
 * `CancellationTokenSource.token` to the asynchronous operation(s). Call
 * `CancellationTokenSource#cancel()` to cancel the operations.
 *
 * A `CancellationToken` can only be cancelled once - it should not be passed to future operations
 * once cancelled.
 *
 * @see CancellationTokenSource
 *
 * @see CancellationTokenSource.token
 * @see CancellationTokenSource.cancel
 * @see CancellationToken.register
 */
class CancellationToken internal constructor(private val tokenSource: CancellationTokenSource) {
  /** @return `true` if the cancellation was requested from the source, `false` otherwise. */
  val isCancellationRequested: Boolean
    get() = tokenSource.isCancellationRequested

  /**
   * Registers a runnable that will be called when this CancellationToken is canceled. If this token
   * is already in the canceled state, the runnable will be run immediately and synchronously.
   *
   * @param action the runnable to be run when the token is cancelled.
   * @return a [CancellationTokenRegistration] instance that can be used to unregister the action.
   */
  fun register(action: Runnable?): CancellationTokenRegistration {
    return tokenSource.register(action)
  }

  /**
   * @throws CancellationException if this token has had cancellation requested. May be used to stop
   * execution of a thread or runnable.
   */
  @Throws(CancellationException::class)
  fun throwIfCancellationRequested() {
    tokenSource.throwIfCancellationRequested()
  }

  override fun toString(): String {
    return String.format(
        Locale.US,
        "%s@%s[cancellationRequested=%s]",
        javaClass.name,
        Integer.toHexString(hashCode()),
        java.lang.Boolean.toString(tokenSource.isCancellationRequested))
  }
}
