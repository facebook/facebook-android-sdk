/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * This was created because the helper methods in [java.util.concurrent.Executors] do not work as
 * people would normally expect.
 *
 * Normally, you would think that a cached thread pool would create new threads when necessary,
 * queue them when the pool is full, and kill threads when they've been inactive for a certain
 * period of time. This is not how [java.util.concurrent.Executors.newCachedThreadPool] works.
 *
 * Instead, [java.util.concurrent.Executors.newCachedThreadPool] executes all tasks on a new or
 * cached thread immediately because corePoolSize is 0, SynchronousQueue is a queue with size 0 and
 * maxPoolSize is Integer.MAX_VALUE. This is dangerous because it can create an unchecked amount of
 * threads.
 */
internal class AndroidExecutors private constructor() {
  private val uiThread: Executor = UIThreadExecutor()

  /** An [java.util.concurrent.Executor] that runs tasks on the UI thread. */
  private class UIThreadExecutor : Executor {
    override fun execute(command: Runnable) {
      Handler(Looper.getMainLooper()).post(command)
    }
  }

  companion object {
    private val INSTANCE = AndroidExecutors()

    /**
     * Nexus 5: Quad-Core Moto X: Dual-Core
     *
     * AsyncTask: CORE_POOL_SIZE = CPU_COUNT + 1 MAX_POOL_SIZE = CPU_COUNT * 2 + 1
     *
     * https://github.com/android/platform_frameworks_base/commit/719c44e03b97e850a46136ba336d729f5fbd1f47
     */
    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()

    private val CORE_POOL_SIZE = CPU_COUNT + 1

    private val MAX_POOL_SIZE = CPU_COUNT * 2 + 1

    private const val KEEP_ALIVE_TIME = 1L

    /**
     * Creates a proper Cached Thread Pool. Tasks will reuse cached threads if available or create
     * new threads until the core pool is full. tasks will then be queued. If an task cannot be
     * queued, a new thread will be created unless this would exceed max pool size, then the task
     * will be rejected. Threads will time out after 1 second.
     *
     * Core thread timeout is only available on android-9+.
     *
     * @return the newly created thread pool
     */
    @JvmStatic
    fun newCachedThreadPool(): ExecutorService {
      val executor =
          ThreadPoolExecutor(
              CORE_POOL_SIZE,
              MAX_POOL_SIZE,
              KEEP_ALIVE_TIME,
              TimeUnit.SECONDS,
              LinkedBlockingQueue())
      executor.allowCoreThreadTimeOut(true)
      return executor
    }

    /** An [java.util.concurrent.Executor] that executes tasks on the UI thread. */
    @JvmStatic fun uiThread(): Executor = INSTANCE.uiThread
  }
}
