/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

/**
 * A function to be called after a task completes.
 *
 * If you wish to have the Task from a Continuation that does not return a Task be cancelled then
 * throw a [java.util.concurrent.CancellationException] from the Continuation.
 *
 * @see Task
 */
fun interface Continuation<TTaskResult, TContinuationResult> {
  /**
   * The function to be called when the task in the parameter is competed.
   *
   * @param task the completed task. See [Task.continueWith] for more details.
   * @return a nullable result of this function.
   */
  @Throws(Exception::class) fun then(task: Task<TTaskResult>): TContinuationResult?
}
