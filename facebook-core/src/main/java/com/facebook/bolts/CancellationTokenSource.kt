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
import java.io.Closeable
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Signals to a [CancellationToken] that it should be canceled. To create a `CancellationToken`
 * first create a `CancellationTokenSource` then use [token] to retrieve the token for the source.
 *
 * @see CancellationToken
 * @see CancellationTokenSource.token
 */
class CancellationTokenSource : Closeable {
  private val lock = Any()
  private val registrations = mutableListOf<CancellationTokenRegistration>()
  private val executor = BoltsExecutors.scheduled()
  private var scheduledCancellation: ScheduledFuture<*>? = null
  private var cancellationRequested = false
  private var closed = false

  /** @return `true` if cancellation has been requested for this `CancellationTokenSource`. */
  val isCancellationRequested: Boolean
    get() {
      synchronized(lock) {
        throwIfClosed()
        return cancellationRequested
      }
    }

  /** The token that can be passed to asynchronous method to control cancellation. */
  val token: CancellationToken
    get() {
      synchronized(lock) {
        throwIfClosed()
        return CancellationToken(this)
      }
    }

  /** Cancels the token if it has not already been cancelled. */
  fun cancel() {
    var registrations: List<CancellationTokenRegistration>
    synchronized(lock) {
      throwIfClosed()
      if (cancellationRequested) {
        return
      }
      cancelScheduledCancellation()
      cancellationRequested = true
      registrations = ArrayList(this.registrations)
    }
    notifyListeners(registrations)
  }

  /**
   * Schedules a cancel operation on this `CancellationTokenSource` after the specified number of
   * milliseconds.
   *
   * @param delay The number of milliseconds to wait before completing the returned task. If delay
   * is `0` the cancel is executed immediately. If delay is `-1` any scheduled cancellation is
   * stopped.
   */
  fun cancelAfter(delay: Long) {
    cancelAfter(delay, TimeUnit.MILLISECONDS)
  }

  private fun cancelAfter(delay: Long, timeUnit: TimeUnit) {
    require(delay >= -1) { "Delay must be >= -1" }
    if (delay == 0L) {
      cancel()
      return
    }
    synchronized(lock) {
      if (cancellationRequested) {
        return
      }
      cancelScheduledCancellation()
      if (delay != -1L) {
        scheduledCancellation =
            executor.schedule(
                Runnable {
                  synchronized(lock) { scheduledCancellation = null }
                  cancel()
                },
                delay,
                timeUnit)
      }
    }
  }

  override fun close() {
    synchronized(lock) {
      if (closed) {
        return
      }
      cancelScheduledCancellation()
      for (registration: CancellationTokenRegistration in registrations) {
        registration.close()
      }
      registrations.clear()
      closed = true
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  fun register(action: Runnable?): CancellationTokenRegistration {
    var ctr: CancellationTokenRegistration
    synchronized(lock) {
      throwIfClosed()
      ctr = CancellationTokenRegistration(this, action)
      if (cancellationRequested) {
        ctr.runAction()
      } else {
        registrations.add(ctr)
      }
    }
    return ctr
  }

  /**
   * @throws CancellationException if this token has had cancellation requested. May be used to stop
   * execution of a thread or runnable.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  @Throws(CancellationException::class)
  fun throwIfCancellationRequested() {
    synchronized(lock) {
      throwIfClosed()
      if (cancellationRequested) {
        throw CancellationException()
      }
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  fun unregister(registration: CancellationTokenRegistration) {
    synchronized(lock) {
      throwIfClosed()
      registrations.remove(registration)
    }
  }

  // This method makes no attempt to perform any synchronization or state checks itself and once
  // invoked will notify all runnables unconditionally. As such if you require the notification
  // event
  // to be synchronized with state changes you should provide external synchronization.
  // If this is invoked without external synchronization there is a probability the token becomes
  // cancelled concurrently.
  private fun notifyListeners(registrations: List<CancellationTokenRegistration>) {
    for (registration in registrations) {
      registration.runAction()
    }
  }

  override fun toString(): String {
    return String.format(
        Locale.US,
        "%s@%s[cancellationRequested=%s]",
        javaClass.name,
        Integer.toHexString(hashCode()),
        java.lang.Boolean.toString(isCancellationRequested))
  }

  // This method makes no attempt to perform any synchronization itself - you should ensure
  // accesses to this method are synchronized if you want to ensure correct behaviour in the
  // face of a concurrent invocation of the close method.
  private fun throwIfClosed() {
    check(!closed) { "Object already closed" }
  }

  // Performs no synchronization.
  private fun cancelScheduledCancellation() {
    val scheduledCancellation = this.scheduledCancellation ?: return
    scheduledCancellation.cancel(true)
    this.scheduledCancellation = null
  }
}
