/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
    @JvmStatic fun background(): ExecutorService = INSTANCE.background

    @JvmStatic internal fun scheduled(): ScheduledExecutorService = INSTANCE.scheduled

    /**
     * An [java.util.concurrent.Executor] that executes tasks in the current thread unless the stack
     * runs too deep, at which point it will delegate to [BoltsExecutors.background] in order to
     * trim the stack.
     */
    @JvmStatic internal fun immediate(): Executor = INSTANCE.immediate
  }
}
