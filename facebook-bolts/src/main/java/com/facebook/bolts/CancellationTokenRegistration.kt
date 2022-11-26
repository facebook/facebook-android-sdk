/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import java.io.Closeable

/**
 * Represents a callback delegate that has been registered with a [CancellationToken].
 *
 * @see CancellationToken.register
 */
class CancellationTokenRegistration
internal constructor(tokenSource: CancellationTokenSource, private var action: Runnable?) :
    Closeable {
  private var closed = false
  private var tokenSource: CancellationTokenSource? = tokenSource

  /** Unregisters the callback runnable from the cancellation token. */
  override fun close() {
    synchronized(this) {
      if (closed) {
        return
      }
      closed = true
      tokenSource?.unregister(this)
      tokenSource = null
      action = null
    }
  }

  internal fun runAction() {
    synchronized(this) {
      throwIfClosed()
      action?.run()
      close()
    }
  }

  private fun throwIfClosed() {
    check(!closed) { "Object already closed" }
  }
}
