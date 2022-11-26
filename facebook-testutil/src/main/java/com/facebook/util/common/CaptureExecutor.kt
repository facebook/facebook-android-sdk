/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.util.common

import java.util.concurrent.Executor

/**
 * CaptureExecutor will capture the runnable for execution. Then the tests can run the runnable
 * object by themselves.
 */
class CaptureExecutor : Executor {
  var capturedRunnable: Runnable? = null
  override fun execute(task: Runnable?) {
    capturedRunnable = task
  }
}
