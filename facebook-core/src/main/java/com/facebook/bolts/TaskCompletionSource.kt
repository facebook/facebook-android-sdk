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
  fun trySetResult(result: TResult): Boolean {
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
  fun setResult(result: TResult) {
    check(trySetResult(result)) { "Cannot set the result of a completed task." }
  }

  /** Sets the error of the Task, throwing if the Task has already been completed. */
  fun setError(error: Exception?) {
    check(trySetError(error)) { "Cannot set the error on a completed task." }
  }
}
