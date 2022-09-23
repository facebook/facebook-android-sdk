/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

/**
 * Allows safe orchestration of a task's completion, preventing the consumer from prematurely
 * completing the task. Essentially, it represents the producer side of a Task<TResult>, providing
 * access to the consumer side through the getTask() method while isolating the Task's completion
 * mechanisms from the consumer. </TResult>
 */
open class TaskCompletionSource<TResult> {
  /** @return the Task associated with this TaskCompletionSource. */
  val task: Task<TResult> = Task()

  /** Sets the cancelled flag on the Task if the Task hasn't already been completed. */
  fun trySetCancelled(): Boolean {
    return task.trySetCancelled()
  }

  /** Sets the result on the Task if the Task hasn't already been completed. */
  fun trySetResult(result: TResult?): Boolean {
    return task.trySetResult(result)
  }

  /** Sets the error on the Task if the Task hasn't already been completed. */
  fun trySetError(error: Exception?): Boolean {
    return task.trySetError(error)
  }

  /** Sets the cancelled flag on the task, throwing if the Task has already been completed. */
  fun setCancelled() {
    check(trySetCancelled()) { "Cannot cancel a completed task." }
  }

  /** Sets the result of the Task, throwing if the Task has already been completed. */
  fun setResult(result: TResult?) {
    check(trySetResult(result)) { "Cannot set the result of a completed task." }
  }

  /** Sets the error of the Task, throwing if the Task has already been completed. */
  fun setError(error: Exception?) {
    check(trySetError(error)) { "Cannot set the error on a completed task." }
  }
}
