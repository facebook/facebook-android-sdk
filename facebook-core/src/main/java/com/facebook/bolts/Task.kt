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

import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Represents the result of an asynchronous operation.
 *
 * @param <TResult> The type of the result of the task. </TResult>
 */
class Task<TResult> {
  /**
   * Interface for handlers invoked when a failed `Task` is about to be finalized, but the exception
   * has not been consumed.
   *
   * The handler will execute in the GC thread, so if the handler needs to do anything time
   * consuming or complex it is a good idea to fire off a `Task` to handle the exception.
   *
   * @see .unobservedExceptionHandler
   */
  fun interface UnobservedExceptionHandler {
    /**
     * Method invoked when the given task has an unobserved exception.
     *
     * Any exception thrown by this method will be ignored.
     *
     * @param t the task
     * @param e the exception
     */
    fun unobservedException(t: Task<*>, e: UnobservedTaskException)
  }

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()
  private var completeField = false
  private var cancelledField = false
  private var resultField: TResult? = null
  private var errorField: Exception? = null
  private var errorHasBeenObserved = false
  private var unobservedErrorNotifier: UnobservedErrorNotifier? = null
  private var continuations: MutableList<Continuation<TResult, Void>>? = ArrayList()

  internal constructor()
  private constructor(result: TResult?) {
    trySetResult(result)
  }

  private constructor(cancelled: Boolean) {
    if (cancelled) {
      trySetCancelled()
    } else {
      trySetResult(null)
    }
  }

  /** `true` if the task completed (has a result, an error, or was cancelled. `false` otherwise. */
  val isCompleted: Boolean
    get() {
      return lock.withLock { completeField }
    }

  /** `true` if the task was cancelled, `false` otherwise. */
  val isCancelled: Boolean
    get() {
      return lock.withLock { cancelledField }
    }

  /** @return `true` if the task has an error, `false` otherwise. */
  val isFaulted: Boolean
    get() {
      return lock.withLock { errorField != null }
    }

  /** @return The result of the task, if set. `null` otherwise. */
  val result: TResult?
    get() {
      return lock.withLock { resultField }
    }

  /** @return The error for the task, if set. `null` otherwise. */
  val error: Exception?
    get() {
      return lock.withLock {
        if (errorField != null) {
          errorHasBeenObserved = true
          unobservedErrorNotifier?.apply {
            this.setObserved()
            unobservedErrorNotifier = null
          }
        }
        errorField
      }
    }

  /** Blocks until the task is complete. */
  @Throws(InterruptedException::class)
  fun waitForCompletion() {
    lock.withLock {
      if (!isCompleted) {
        condition.await()
      }
    }
  }

  /**
   * Blocks until the task is complete or times out.
   *
   * @return `true` if the task completed (has a result, an error, or was cancelled). `false`
   * otherwise.
   */
  @Throws(InterruptedException::class)
  fun waitForCompletion(duration: Long, timeUnit: TimeUnit): Boolean {
    return lock.withLock {
      if (!isCompleted) {
        condition.await(duration, timeUnit)
      }
      isCompleted
    }
  }

  /**
   * Makes a fluent cast of a Task's result possible, avoiding an extra continuation just to cast
   * the type of the result.
   */
  fun <TOut> cast(): Task<TOut> {
    @Suppress("unchecked") return this as Task<TOut>
  }

  /** Turns a Task<T> into a Task<Void>, dropping any result. </Void></T> */
  fun makeVoid(): Task<Void> {
    return this.continueWithTask<Void>(
        Continuation { task ->
          if (task.isCancelled) {
            return@Continuation cancelled<Void>()
          }
          if (task.isFaulted) {
            forError(task.error)
          } else forResult(null)
        })
  }

  /**
   * Continues a task with the equivalent of a Task-based while loop, where the body of the loop is
   * a task continuation.
   */
  fun continueWhile(
      predicate: Callable<Boolean>,
      continuation: Continuation<Void, Task<Void>>
  ): Task<Void> {
    return continueWhile(predicate, continuation, IMMEDIATE_EXECUTOR, null)
  }

  /**
   * Continues a task with the equivalent of a Task-based while loop, where the body of the loop is
   * a task continuation.
   */
  fun continueWhile(
      predicate: Callable<Boolean>,
      continuation: Continuation<Void, Task<Void>>,
      ct: CancellationToken?
  ): Task<Void> {
    return continueWhile(predicate, continuation, IMMEDIATE_EXECUTOR, ct)
  }
  /**
   * Continues a task with the equivalent of a Task-based while loop, where the body of the loop is
   * a task continuation.
   */
  /**
   * Continues a task with the equivalent of a Task-based while loop, where the body of the loop is
   * a task continuation.
   */
  /**
   * Continues a task with the equivalent of a Task-based while loop, where the body of the loop is
   * a task continuation.
   */
  fun continueWhile(
      predicate: Callable<Boolean>,
      continuation: Continuation<Void, Task<Void>>,
      executor: Executor = IMMEDIATE_EXECUTOR,
      ct: CancellationToken? = null
  ): Task<Void> {
    val predicateContinuation: Continuation<Void, Task<Void>> =
        object : Continuation<Void, Task<Void>> {
          @Throws(Exception::class)
          override fun then(task: Task<Void>): Task<Void> {
            if (ct != null && ct.isCancellationRequested) {
              return cancelled()
            }
            return if (predicate.call()) {
              forResult<Void>(null)
                  .onSuccessTask(continuation, executor)
                  .onSuccessTask(this, executor)
            } else forResult(null)
          }
        }
    return makeVoid().continueWithTask(predicateContinuation, executor)
  }

  /**
   * Adds a continuation that will be scheduled using the executor, returning a new task that
   * completes after the continuation has finished running. This allows the continuation to be
   * scheduled on different thread.
   */
  fun <TContinuationResult> continueWith(
      continuation: Continuation<TResult, TContinuationResult>,
      executor: Executor
  ): Task<TContinuationResult> {
    return continueWith(continuation, executor, null)
  }

  /**
   * Adds a continuation that will be scheduled using the executor, returning a new task that
   * completes after the continuation has finished running. This allows the continuation to be
   * scheduled on different thread.
   */
  fun <TContinuationResult> continueWith(
      continuation: Continuation<TResult, TContinuationResult>,
      executor: Executor,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    var completed: Boolean
    val tcs = TaskCompletionSource<TContinuationResult>()
    lock.withLock {
      completed = isCompleted
      if (!completed) {
        continuations?.add(
            Continuation { task ->
              completeImmediately(tcs, continuation, task, executor, ct)
              null
            })
      }
    }
    if (completed) {
      completeImmediately(tcs, continuation, this, executor, ct)
    }
    return tcs.task
  }

  /**
   * Adds a synchronous continuation to this task, returning a new task that completes after the
   * continuation has finished running.
   */
  fun <TContinuationResult> continueWith(
      continuation: Continuation<TResult, TContinuationResult>
  ): Task<TContinuationResult> {
    return continueWith(continuation, IMMEDIATE_EXECUTOR, null)
  }

  /**
   * Adds a synchronous continuation to this task, returning a new task that completes after the
   * continuation has finished running.
   */
  fun <TContinuationResult> continueWith(
      continuation: Continuation<TResult, TContinuationResult>,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    return continueWith(continuation, IMMEDIATE_EXECUTOR, ct)
  }

  /**
   * Adds an Task-based continuation to this task that will be scheduled using the executor,
   * returning a new task that completes after the task returned by the continuation has completed.
   */
  fun <TContinuationResult> continueWithTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>,
      executor: Executor
  ): Task<TContinuationResult> {
    return continueWithTask(continuation, executor, null)
  }

  /**
   * Adds an Task-based continuation to this task that will be scheduled using the executor,
   * returning a new task that completes after the task returned by the continuation has completed.
   */
  fun <TContinuationResult> continueWithTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>,
      executor: Executor,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    var completed: Boolean
    val tcs = TaskCompletionSource<TContinuationResult>()
    lock.withLock {
      completed = isCompleted
      if (!completed) {
        continuations?.add(
            Continuation { task ->
              completeAfterTask(tcs, continuation, task, executor, ct)
              null
            })
      }
    }
    if (completed) {
      completeAfterTask(tcs, continuation, this, executor, ct)
    }
    return tcs.task
  }

  /**
   * Adds an asynchronous continuation to this task, returning a new task that completes after the
   * task returned by the continuation has completed.
   */
  fun <TContinuationResult> continueWithTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>
  ): Task<TContinuationResult> {
    return continueWithTask(continuation, IMMEDIATE_EXECUTOR, null)
  }

  /**
   * Adds an asynchronous continuation to this task, returning a new task that completes after the
   * task returned by the continuation has completed.
   */
  fun <TContinuationResult> continueWithTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    return continueWithTask(continuation, IMMEDIATE_EXECUTOR, ct)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [Exception] or
   * cancellation.
   */
  fun <TContinuationResult> onSuccess(
      continuation: Continuation<TResult, TContinuationResult>,
      executor: Executor
  ): Task<TContinuationResult> {
    return onSuccess(continuation, executor, null)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [Exception] or
   * cancellation.
   */
  fun <TContinuationResult> onSuccess(
      continuation: Continuation<TResult, TContinuationResult>,
      executor: Executor,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    return continueWithTask(
        Continuation { task ->
          if (ct != null && ct.isCancellationRequested) {
            return@Continuation cancelled()
          }
          if (task.isFaulted) {
            forError(task.error)
          } else if (task.isCancelled) {
            cancelled()
          } else {
            task.continueWith(continuation)
          }
        },
        executor)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [Exception]s or
   * cancellation.
   */
  fun <TContinuationResult> onSuccess(
      continuation: Continuation<TResult, TContinuationResult>
  ): Task<TContinuationResult> {
    return onSuccess(continuation, IMMEDIATE_EXECUTOR, null)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [Exception]s or
   * cancellation.
   */
  fun <TContinuationResult> onSuccess(
      continuation: Continuation<TResult, TContinuationResult>,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    return onSuccess(continuation, IMMEDIATE_EXECUTOR, ct)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [Exception]s or
   * cancellation.
   */
  fun <TContinuationResult> onSuccessTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>,
      executor: Executor
  ): Task<TContinuationResult> {
    return onSuccessTask(continuation, executor, null)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [Exception]s or
   * cancellation.
   */
  fun <TContinuationResult> onSuccessTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>,
      executor: Executor,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    return continueWithTask(
        Continuation { task ->
          if (ct != null && ct.isCancellationRequested) {
            return@Continuation cancelled()
          }
          if (task.isFaulted) {
            forError(task.error)
          } else if (task.isCancelled) {
            cancelled()
          } else {
            task.continueWithTask(continuation)
          }
        },
        executor)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [ ]s or cancellation.
   */
  fun <TContinuationResult> onSuccessTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>
  ): Task<TContinuationResult> {
    return onSuccessTask(continuation, IMMEDIATE_EXECUTOR)
  }

  /**
   * Runs a continuation when a task completes successfully, forwarding along [ ]s or cancellation.
   */
  fun <TContinuationResult> onSuccessTask(
      continuation: Continuation<TResult, Task<TContinuationResult>>,
      ct: CancellationToken?
  ): Task<TContinuationResult> {
    return onSuccessTask(continuation, IMMEDIATE_EXECUTOR, ct)
  }

  private fun runContinuations() {
    lock.withLock {
      continuations?.forEach {
        try {
          it.then(this@Task)
        } catch (e: RuntimeException) {
          throw e
        } catch (e: Throwable) {
          throw RuntimeException(e)
        }
      }
      continuations = null
    }
  }

  /** Sets the cancelled flag on the Task if the Task hasn't already been completed. */
  /* package */
  fun trySetCancelled(): Boolean {
    lock.withLock {
      if (completeField) {
        return false
      }
      completeField = true
      cancelledField = true
      condition.signalAll()
      runContinuations()
      return true
    }
  }

  /** Sets the result on the Task if the Task hasn't already been completed. */
  /* package */
  fun trySetResult(result: TResult?): Boolean {
    lock.withLock {
      if (completeField) {
        return false
      }
      completeField = true
      this@Task.resultField = result
      condition.signalAll()
      runContinuations()
      return true
    }
  }

  /** Sets the error on the Task if the Task hasn't already been completed. */
  /* package */
  fun trySetError(error: Exception?): Boolean {
    lock.withLock {
      if (completeField) {
        return false
      }
      completeField = true
      this@Task.errorField = error
      errorHasBeenObserved = false
      condition.signalAll()
      runContinuations()
      if (!errorHasBeenObserved && unobservedExceptionHandler != null) {
        unobservedErrorNotifier = UnobservedErrorNotifier(this)
      }
      return true
    }
  }

  @Deprecated("Please use [com.facebook.bolts.TaskCompletionSource] instead. ")
  inner class TaskCompletionSource internal constructor() :
      com.facebook.bolts.TaskCompletionSource<TResult>()

  companion object {
    /** An [java.util.concurrent.Executor] that executes tasks in parallel. */
    @JvmField val BACKGROUND_EXECUTOR: ExecutorService = BoltsExecutors.background()

    /**
     * An [java.util.concurrent.Executor] that executes tasks in the current thread unless the stack
     * runs too deep, at which point it will delegate to [Task.BACKGROUND_EXECUTOR] in order to trim
     * the stack.
     */
    private val IMMEDIATE_EXECUTOR = BoltsExecutors.immediate()

    /** An [java.util.concurrent.Executor] that executes tasks on the UI thread. */
    @JvmField val UI_THREAD_EXECUTOR: Executor = AndroidExecutors.uiThread()

    // null unless explicitly set
    @Volatile private var unobservedExceptionHandler: UnobservedExceptionHandler? = null

    /** Returns the handler invoked when a task has an unobserved exception or `null`. */
    @JvmStatic
    fun getUnobservedExceptionHandler(): UnobservedExceptionHandler? = unobservedExceptionHandler

    /**
     * Set the handler invoked when a task has an unobserved exception.
     *
     * @param eh the object to use as an unobserved exception handler. If <tt>null</tt> then
     * unobserved exceptions will be ignored.
     */
    @JvmStatic
    fun setUnobservedExceptionHandler(eh: UnobservedExceptionHandler?) {
      unobservedExceptionHandler = eh
    }

    /** Creates a completed task with the given value. */
    @Suppress("unchecked")
    @JvmStatic
    fun <TResult> forResult(value: TResult?): Task<TResult> {
      if (value == null) {
        return TASK_NULL as Task<TResult>
      }
      if (value is Boolean) {
        return (if (value) TASK_TRUE else TASK_FALSE) as Task<TResult>
      }
      val tcs = TaskCompletionSource<TResult>()
      tcs.setResult(value)
      return tcs.task
    }

    /** Creates a faulted task with the given error. */
    @JvmStatic
    fun <TResult> forError(error: Exception?): Task<TResult> {
      val tcs = TaskCompletionSource<TResult>()
      tcs.setError(error)
      return tcs.task
    }

    /** Creates a cancelled task. */
    @Suppress("unchecked")
    @JvmStatic
    fun <TResult> cancelled(): Task<TResult> {
      return TASK_CANCELLED as Task<TResult>
    }

    /**
     * Creates a task that completes after a time delay.
     *
     * @param delay The number of milliseconds to wait before completing the returned task. Zero and
     * negative values are treated as requests for immediate execution.
     */
    @JvmStatic
    fun delay(delay: Long): Task<Void?> {
      return delay(delay, BoltsExecutors.scheduled(), null)
    }

    /**
     * Creates a task that completes after a time delay.
     *
     * @param delay The number of milliseconds to wait before completing the returned task. Zero and
     * negative values are treated as requests for immediate execution.
     * @param cancellationToken The optional cancellation token that will be checked prior to
     * completing the returned task.
     */
    @JvmStatic
    fun delay(delay: Long, cancellationToken: CancellationToken?): Task<Void?> {
      return delay(delay, BoltsExecutors.scheduled(), cancellationToken)
    }

    @JvmStatic
    internal fun delay(
        delay: Long,
        executor: ScheduledExecutorService,
        cancellationToken: CancellationToken?
    ): Task<Void?> {
      if (cancellationToken != null && cancellationToken.isCancellationRequested) {
        return cancelled()
      }
      if (delay <= 0) {
        return forResult(null)
      }
      val tcs = TaskCompletionSource<Void?>()
      val scheduled = executor.schedule({ tcs.trySetResult(null) }, delay, TimeUnit.MILLISECONDS)
      cancellationToken?.register {
        scheduled.cancel(true)
        tcs.trySetCancelled()
      }
      return tcs.task
    }

    /**
     * Invokes the callable on a background thread, returning a Task to represent the operation.
     *
     * If you want to cancel the resulting Task throw a [Exception] from the callable.
     */
    @JvmStatic
    fun <TResult> callInBackground(callable: Callable<TResult?>): Task<TResult> {
      return call(callable, BACKGROUND_EXECUTOR, null)
    }

    /** Invokes the callable on a background thread, returning a Task to represent the operation. */
    @JvmStatic
    fun <TResult> callInBackground(
        callable: Callable<TResult?>,
        ct: CancellationToken?
    ): Task<TResult> {
      return call(callable, BACKGROUND_EXECUTOR, ct)
    }

    /**
     * Invokes the callable using the given executor, returning a Task to represent the operation.
     *
     * If you want to cancel the resulting Task throw a [Exception] from the callable.
     */
    @JvmStatic
    fun <TResult> call(callable: Callable<TResult?>, executor: Executor): Task<TResult> {
      return call(callable, executor, null)
    }

    /**
     * Invokes the callable using the given executor, returning a Task to represent the operation.
     */
    @JvmStatic
    fun <TResult> call(
        callable: Callable<TResult?>,
        executor: Executor,
        ct: CancellationToken?
    ): Task<TResult> {
      val tcs = TaskCompletionSource<TResult>()
      try {
        executor.execute(
            Runnable {
              if (ct != null && ct.isCancellationRequested) {
                tcs.setCancelled()
                return@Runnable
              }
              try {
                tcs.setResult(callable.call())
              } catch (e: CancellationException) {
                tcs.setCancelled()
              } catch (e: Exception) {
                tcs.setError(e)
              }
            })
      } catch (e: Exception) {
        tcs.setError(ExecutorException(e))
      }
      return tcs.task
    }

    /**
     * Invokes the callable on the current thread, producing a Task.
     *
     * If you want to cancel the resulting Task throw a [Exception] from the callable.
     */
    @JvmStatic
    fun <TResult> call(callable: Callable<TResult?>): Task<TResult> {
      return call(callable, IMMEDIATE_EXECUTOR, null)
    }

    /** Invokes the callable on the current thread, producing a Task. */
    @JvmStatic
    fun <TResult> call(callable: Callable<TResult?>, ct: CancellationToken?): Task<TResult> {
      return call(callable, IMMEDIATE_EXECUTOR, ct)
    }

    /**
     * Creates a task that will complete when any of the supplied tasks have completed.
     *
     * The returned task will complete when any of the supplied tasks has completed. The returned
     * task will always end in the completed state with its result set to the first task to
     * complete. This is true even if the first task to complete ended in the canceled or faulted
     * state.
     *
     * @param tasks The tasks to wait on for completion.
     * @return A task that represents the completion of one of the supplied tasks. The return task's
     * result is the task that completed.
     */
    @JvmStatic
    fun <TResult> whenAnyResult(tasks: Collection<Task<TResult>>): Task<Task<TResult>> {
      if (tasks.isEmpty()) {
        return forResult(null)
      }
      val firstCompleted = TaskCompletionSource<Task<TResult>>()
      val isAnyTaskComplete = AtomicBoolean(false)
      for (task in tasks) {
        task.continueWith<Void>(
            Continuation {
              if (isAnyTaskComplete.compareAndSet(false, true)) {
                firstCompleted.setResult(it)
              } else {
                // ensure observed
                it.error
              }
              null
            })
      }
      return firstCompleted.task
    }

    /**
     * Creates a task that will complete when any of the supplied tasks have completed.
     *
     * The returned task will complete when any of the supplied tasks has completed. The returned
     * task will always end in the completed state with its result set to the first task to
     * complete. This is true even if the first task to complete ended in the canceled or faulted
     * state.
     *
     * @param tasks The tasks to wait on for completion.
     * @return A task that represents the completion of one of the supplied tasks. The return task's
     * Result is the task that completed.
     */
    @Suppress("unchecked")
    @JvmStatic
    fun whenAny(tasks: Collection<Task<*>>): Task<Task<*>> {
      if (tasks.isEmpty()) {
        return forResult(null)
      }
      val firstCompleted = TaskCompletionSource<Task<*>>()
      val isAnyTaskComplete = AtomicBoolean(false)
      for (task in tasks) {
        (task as Task<Any>).continueWith<Void>(
            Continuation<Any, Void> {
              if (isAnyTaskComplete.compareAndSet(false, true)) {
                firstCompleted.setResult(it)
              } else {
                // ensure observed
                it.error
              }
              null
            })
      }
      return firstCompleted.task
    }

    /**
     * Creates a task that completes when all of the provided tasks are complete.
     *
     * If any of the supplied tasks completes in a faulted state, the returned task will also
     * complete in a faulted state, where its exception will resolve to that [Exception] if a single
     * task fails or an [AggregateException] of all the [Exception]s if multiple tasks fail.
     *
     * If none of the supplied tasks faulted but at least one of them was cancelled, the returned
     * task will end as cancelled.
     *
     * If none of the tasks faulted and none of the tasks were cancelled, the resulting task will
     * end completed. The result of the returned task will be set to a list containing all of the
     * results of the supplied tasks in the same order as they were provided (e.g. if the input
     * tasks collection contained t1, t2, t3, the output task's result will return an
     * `List&lt;TResult&gt;` where `list.get(0) == t1.getResult(), list.get(1) == t2.getResult(),
     * and list.get(2) == t3.getResult()`).
     *
     * If the supplied collection contains no tasks, the returned task will immediately transition
     * to a completed state before it's returned to the caller. The returned `List&lt;TResult&gt;`
     * will contain 0 elements.
     *
     * @param tasks The tasks that the return value will wait for before completing.
     * @return A Task that will resolve to `List&lt;TResult&gt;` when all the tasks are resolved.
     */
    @JvmStatic
    fun <TResult> whenAllResult(tasks: Collection<Task<TResult>>): Task<List<TResult?>> {
      return whenAll(tasks)
          .onSuccess(
              object : Continuation<Void, List<TResult?>> {
                override fun then(task: Task<Void>): List<TResult?> {
                  if (tasks.isEmpty()) {
                    return emptyList()
                  }
                  val results: MutableList<TResult?> = ArrayList<TResult?>()
                  for (individualTask in tasks) {
                    results.add(individualTask.result)
                  }
                  return results
                }
              })
    }

    /**
     * Creates a task that completes when all of the provided tasks are complete.
     *
     * If any of the supplied tasks completes in a faulted state, the returned task will also
     * complete in a faulted state, where its exception will resolve to that [ ] if a single task
     * fails or an [AggregateException] of all the [ ]s if multiple tasks fail.
     *
     * If none of the supplied tasks faulted but at least one of them was cancelled, the returned
     * task will end as cancelled.
     *
     * If none of the tasks faulted and none of the tasks were canceled, the resulting task will end
     * in the completed state.
     *
     * If the supplied collection contains no tasks, the returned task will immediately transition
     * to a completed state before it's returned to the caller.
     *
     * @param tasks The tasks that the return value will wait for before completing.
     * @return A Task that will resolve to `Void` when all the tasks are resolved.
     */
    @Suppress("unchecked")
    @JvmStatic
    fun whenAll(tasks: Collection<Task<*>>): Task<Void> {
      if (tasks.isEmpty()) {
        return forResult(null)
      }
      val allFinished = TaskCompletionSource<Void>()
      val causes = ArrayList<Exception?>()
      val errorLock = ReentrantLock()
      val count = AtomicInteger(tasks.size)
      val isCancelled = AtomicBoolean(false)
      for (task in tasks) {
        val t = task as Task<Any>
        t.continueWith {
          if (it.isFaulted) {
            errorLock.withLock { causes.add(it.error) }
          }
          if (it.isCancelled) {
            isCancelled.set(true)
          }
          if (count.decrementAndGet() == 0) {
            if (causes.size != 0) {
              if (causes.size == 1) {
                allFinished.setError(causes[0])
              } else {
                val error: Exception =
                    AggregateException(
                        String.format("There were %d exceptions.", causes.size), causes)
                allFinished.setError(error)
              }
            } else if (isCancelled.get()) {
              allFinished.setCancelled()
            } else {
              allFinished.setResult(null)
            }
          }
          null
        }
      }
      return allFinished.task
    }

    /**
     * Handles the non-async (i.e. the continuation doesn't return a Task) continuation case,
     * passing the results of the given Task through to the given continuation and using the results
     * of that call to set the result of the TaskContinuationSource.
     *
     * @param tcs The TaskContinuationSource that will be orchestrated by this call.
     * @param continuation The non-async continuation.
     * @param task The task being completed.
     * @param executor The executor to use when running the continuation (allowing the continuation
     * to be scheduled on a different thread).
     */
    private fun <TContinuationResult, TResult> completeImmediately(
        tcs: com.facebook.bolts.TaskCompletionSource<TContinuationResult>,
        continuation: Continuation<TResult, TContinuationResult>,
        task: Task<TResult>,
        executor: Executor,
        ct: CancellationToken?
    ) {
      try {
        executor.execute(
            Runnable {
              if (ct != null && ct.isCancellationRequested) {
                tcs.setCancelled()
                return@Runnable
              }
              try {
                val result = continuation.then(task)
                tcs.setResult(result)
              } catch (e: CancellationException) {
                tcs.setCancelled()
              } catch (e: Exception) {
                tcs.setError(e)
              }
            })
      } catch (e: Exception) {
        tcs.setError(ExecutorException(e))
      }
    }

    /**
     * Handles the async (i.e. the continuation does return a Task) continuation case, passing the
     * results of the given Task through to the given continuation to get a new Task. The
     * TaskCompletionSource's results are only set when the new Task has completed, unwrapping the
     * results of the task returned by the continuation.
     *
     * @param tcs The TaskContinuationSource that will be orchestrated by this call.
     * @param continuation The async continuation.
     * @param task The task being completed.
     * @param executor The executor to use when running the continuation (allowing the continuation
     * to be scheduled on a different thread).
     */
    private fun <TContinuationResult, TResult> completeAfterTask(
        tcs: com.facebook.bolts.TaskCompletionSource<TContinuationResult>,
        continuation: Continuation<TResult, Task<TContinuationResult>>,
        task: Task<TResult>,
        executor: Executor,
        ct: CancellationToken?
    ) {
      try {
        executor.execute(
            Runnable {
              if (ct != null && ct.isCancellationRequested) {
                tcs.setCancelled()
                return@Runnable
              }
              try {
                val result = continuation.then(task)
                result?.continueWith<Void>(
                    Continuation { task ->
                      if (ct != null && ct.isCancellationRequested) {
                        tcs.setCancelled()
                        return@Continuation null
                      }
                      if (task.isCancelled) {
                        tcs.setCancelled()
                      } else if (task.isFaulted) {
                        tcs.setError(task.error)
                      } else {
                        tcs.setResult(task.result)
                      }
                      null
                    })
                    ?: tcs.setResult(null)
              } catch (e: CancellationException) {
                tcs.setCancelled()
              } catch (e: Exception) {
                tcs.setError(e)
              }
            })
      } catch (e: Exception) {
        tcs.setError(ExecutorException(e))
      }
    }

    private val TASK_NULL: Task<*> = Task(null)
    private val TASK_TRUE = Task(result = true)
    private val TASK_FALSE = Task(result = false)
    private val TASK_CANCELLED: Task<*> = Task<Any>(true)
  }
}
