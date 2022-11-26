/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import androidx.annotation.VisibleForTesting

/**
 * This class is used to retain a faulted task until either its error is observed or it is
 * finalized. If it is finalized with a task, then the uncaught exception handler is executed with
 * an UnobservedTaskException.
 */
internal class UnobservedErrorNotifier(private var task: Task<*>?) {
  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  fun finalize() {
    val faultedTask = task
    if (faultedTask != null) {
      val ueh = Task.getUnobservedExceptionHandler()
      ueh?.unobservedException(faultedTask, UnobservedTaskException(faultedTask.error))
    }
  }

  fun setObserved() {
    task = null
  }
}
