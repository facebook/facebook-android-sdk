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
package com.facebook.internal

import com.facebook.FacebookException
import com.facebook.FacebookSdk
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class WorkQueue
@JvmOverloads
constructor(
    private val maxConcurrent: Int = DEFAULT_MAX_CONCURRENT,
    private val executor: Executor = FacebookSdk.getExecutor()
) {
  private val workLock = ReentrantLock()
  private var pendingJobs: WorkNode? = null
  private var runningJobs: WorkNode? = null
  private var runningCount = 0

  companion object {
    const val DEFAULT_MAX_CONCURRENT = 8
    private fun assert(condition: Boolean) {
      if (!condition) {
        throw FacebookException("Validation failed")
      }
    }
  }

  @JvmOverloads
  fun addActiveWorkItem(callback: Runnable, addToFront: Boolean = true): WorkItem {
    val node = WorkNode(callback)
    workLock.withLock { pendingJobs = node.addToList(pendingJobs, addToFront) }
    startItem()
    return node
  }

  fun validate() {
    // Verify that all running items know they are running, and counts match
    workLock.withLock {
      var count = 0
      if (runningJobs != null) {
        var walk = runningJobs
        do {
          checkNotNull(walk)
          walk.verify(true)
          count++
          walk = walk.next
        } while (walk !== runningJobs)
      }
      assert(runningCount == count)
    }
  }

  private fun startItem() {
    finishItemAndStartNew(null)
  }

  private fun finishItemAndStartNew(finished: WorkNode?) {
    var ready: WorkNode? = null
    workLock.lock()

    if (finished != null) {
      runningJobs = finished.removeFromList(runningJobs)
      runningCount--
    }
    if (runningCount < maxConcurrent) {
      ready = pendingJobs // Head of the pendingJobs queue
      if (ready != null) {
        // The Queue reassignments are necessary since 'ready' might have been
        // added / removed from the front of either queue, which changes its
        // respective head.
        pendingJobs = ready.removeFromList(pendingJobs)
        runningJobs = ready.addToList(runningJobs, false)
        runningCount++
        ready.isRunning = true
      }
    }
    workLock.unlock()
    if (ready != null) {
      execute(ready)
    }
  }

  private fun execute(node: WorkNode) {
    executor.execute {
      try {
        node.callback.run()
      } finally {
        finishItemAndStartNew(node)
      }
    }
  }

  private inner class WorkNode constructor(val callback: Runnable) : WorkItem {
    var next: WorkNode? = null
      private set
    private var prev: WorkNode? = null
    override var isRunning = false
    override fun cancel(): Boolean {
      workLock.withLock {
        if (!isRunning) {
          pendingJobs = removeFromList(pendingJobs)
          return true
        }
      }
      return false
    }

    override fun moveToFront() {
      workLock.withLock {
        if (!isRunning) {
          pendingJobs = removeFromList(pendingJobs)
          pendingJobs = addToList(pendingJobs, true)
        }
      }
    }

    fun addToList(list: WorkNode?, addToFront: Boolean): WorkNode {
      var list = list
      assert(next == null)
      assert(prev == null)
      if (list == null) {
        prev = this
        next = prev
        list = next
      } else {
        next = list
        prev = list.prev
        prev?.next = this
        next?.prev = prev?.next
      }
      checkNotNull(list)
      return if (addToFront) this else list
    }

    fun removeFromList(list: WorkNode?): WorkNode? {
      var list = list
      assert(next != null)
      assert(prev != null)
      if (list === this) {
        list =
            if (next === this) {
              null
            } else {
              next
            }
      }
      next?.prev = prev
      prev?.next = next
      prev = null
      next = prev
      return list
    }

    fun verify(shouldBeRunning: Boolean) {
      assert(prev?.next ?: this === this)
      assert(next?.prev ?: this === this)
      assert(isRunning == shouldBeRunning)
    }
  }

  interface WorkItem {
    fun cancel(): Boolean
    val isRunning: Boolean
    fun moveToFront()
  }
}
