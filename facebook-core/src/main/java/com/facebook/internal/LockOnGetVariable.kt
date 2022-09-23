/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import com.facebook.FacebookSdk
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask

class LockOnGetVariable<T> {
  private var storedValue: T? = null
  private var initLatch: CountDownLatch? = null

  val value: T?
    get() {
      waitOnInit()
      return storedValue
    }

  constructor(value: T) {
    this.storedValue = value
  }

  constructor(callable: Callable<T>) {
    initLatch = CountDownLatch(1)
    FacebookSdk.getExecutor()
        .execute(
            FutureTask<Void> {
              try {
                storedValue = callable.call()
              } finally {
                initLatch?.countDown()
              }
              null
            })
  }

  private fun waitOnInit() {
    val latch = initLatch ?: return
    try {
      latch.await()
    } catch (ex: InterruptedException) {
      // ignore
    }
  }
}
