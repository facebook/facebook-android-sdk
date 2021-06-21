/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.bolts

import androidx.annotation.VisibleForTesting
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
class CancellationToken
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
constructor(private val tokenSource: CancellationTokenSource) {
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
