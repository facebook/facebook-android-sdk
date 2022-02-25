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

import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/** Collection of [Executor]s to use in conjunction with [Task]. */
internal class BoltsExecutors private constructor() {
  private val background: ExecutorService =
      if (!isAndroidRuntime) Executors.newCachedThreadPool()
      else AndroidExecutors.newCachedThreadPool()
  private val scheduled: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private val immediate: Executor = ImmediateExecutor()

  /**
   * An [java.util.concurrent.Executor] that runs a runnable inline (rather than scheduling it on a
   * thread pool) as long as the recursion depth is less than MAX_DEPTH. If the executor has
   * recursed too deeply, it will instead delegate to the [Task.BACKGROUND_EXECUTOR] in order to
   * trim the stack.
   */
  private class ImmediateExecutor : Executor {
    private val executionDepth = ThreadLocal<Int>()

    /**
     * Increments the depth.
     *
     * @return the new depth value.
     */
    private fun incrementDepth(): Int {
      var oldDepth = executionDepth.get()
      if (oldDepth == null) {
        oldDepth = 0
      }
      val newDepth = oldDepth + 1
      executionDepth.set(newDepth)
      return newDepth
    }

    /**
     * Decrements the depth.
     *
     * @return the new depth value.
     */
    private fun decrementDepth(): Int {
      var oldDepth = executionDepth.get()
      if (oldDepth == null) {
        oldDepth = 0
      }
      val newDepth = oldDepth - 1
      if (newDepth == 0) {
        executionDepth.remove()
      } else {
        executionDepth.set(newDepth)
      }
      return newDepth
    }

    override fun execute(command: Runnable) {
      val depth = incrementDepth()
      try {
        if (depth <= MAX_DEPTH) {
          command.run()
        } else {
          background().execute(command)
        }
      } finally {
        decrementDepth()
      }
    }

    companion object {
      private const val MAX_DEPTH = 15
    }
  }

  companion object {
    private val INSTANCE = BoltsExecutors()
    private val isAndroidRuntime: Boolean
      private get() {
        val javaRuntimeName = System.getProperty("java.runtime.name") ?: return false
        return javaRuntimeName.toLowerCase(Locale.US).contains("android")
      }

    /** An [java.util.concurrent.Executor] that executes tasks in parallel. */
    @JvmStatic
    fun background(): ExecutorService {
      return INSTANCE.background
    }

    @JvmStatic
    internal fun scheduled(): ScheduledExecutorService {
      return INSTANCE.scheduled
    }

    /**
     * An [java.util.concurrent.Executor] that executes tasks in the current thread unless the stack
     * runs too deep, at which point it will delegate to [BoltsExecutors.background] in order to
     * trim the stack.
     */
    @JvmStatic
    internal fun immediate(): Executor {
      return INSTANCE.immediate
    }
  }
}
