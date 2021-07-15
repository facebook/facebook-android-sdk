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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TaskTest {
  private fun runTaskTest(callable: Callable<Task<*>>) {
    try {
      val task = callable.call()
      task.waitForCompletion()
      if (task.isFaulted) {
        val error = task.error
        if (error is RuntimeException) {
          throw error
        }
        throw RuntimeException(error)
      } else if (task.isCancelled) {
        throw RuntimeException(CancellationException())
      }
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }

  // runs in a separate method to ensure it is out of scope.
  private fun startFailedTask() {
    Task.call<Any> { throw RuntimeException() }.waitForCompletion()
  }

  /**
   * Launches a given number of tasks (of Integer) that will complete either in a completed,
   * cancelled or faulted state (random distribution). Each task will reach completion after a
   * somehow random delay (between 500 and 600 ms). Each task reaching a success completion state
   * will have its result set to a random Integer (between 0 to 1000).
   *
   * @param numberOfTasksToLaunch The number of tasks to launch
   * @return A collection containing all the tasks that have been launched
   */
  private fun launchTasksWithRandomCompletions(numberOfTasksToLaunch: Int): Collection<Task<Int>> {
    val tasks = ArrayList<Task<Int>>()
    for (i in 0 until numberOfTasksToLaunch) {
      val task =
          Task.callInBackground {
            Thread.sleep((500 + Math.random() * 100).toLong())
            val rand = Math.random()
            if (rand >= 0.7) {
              throw RuntimeException("This task failed.")
            } else if (rand >= 0.4) {
              throw CancellationException()
            }
            (Math.random() * 1000).toInt()
          }
      tasks.add(task)
    }
    return tasks
  }

  @Test
  fun testCache() {
    assertThat(Task.forResult<Any>(null)).isSameAs(Task.forResult<Any>(null))
    val trueTask = Task.forResult(true)
    assertThat(trueTask.result).isTrue
    assertThat(trueTask).isSameAs(Task.forResult(true))
    val falseTask = Task.forResult(false)
    assertThat(falseTask.result).isFalse
    assertThat(falseTask).isSameAs(Task.forResult(false))
    assertThat(Task.cancelled<Any>()).isSameAs(Task.cancelled<Any>())
  }

  @Test
  fun testPrimitives() {
    val complete = Task.forResult(5)
    val error = Task.forError<Int>(RuntimeException())
    val cancelled = Task.cancelled<Int>()
    assertThat(complete.isCompleted).isTrue
    assertThat(complete.result).isEqualTo(5)
    assertThat(complete.isFaulted).isFalse
    assertThat(complete.isCancelled).isFalse
    assertThat(error.isCompleted).isTrue
    assertThat(error.error is RuntimeException).isTrue
    assertThat(error.isFaulted).isTrue
    assertThat(error.isCancelled).isFalse
    assertThat(cancelled.isCompleted).isTrue
    assertThat(cancelled.isFaulted).isFalse
    assertThat(cancelled.isCancelled).isTrue
  }

  @Test
  fun testDelay() {
    val delayed = Task.delay(200)
    Thread.sleep(50)
    assertThat(delayed.isCompleted).isFalse
    Thread.sleep(200)
    assertThat(delayed.isCompleted).isTrue
    assertThat(delayed.isFaulted).isFalse
    assertThat(delayed.isCancelled).isFalse
  }

  @Test
  fun testDelayWithCancelledToken() {
    val cts = CancellationTokenSource()
    cts.cancel()
    val delayed = Task.delay(200, cts.token)
    assertThat(delayed.isCancelled).isTrue
  }

  @Test
  fun testDelayWithToken() {
    val cts = CancellationTokenSource()
    val delayed = Task.delay(200, cts.token)
    assertThat(delayed.isCancelled).isFalse
    cts.cancel()
    assertThat(delayed.isCancelled).isTrue
  }

  @Test
  fun testSynchronousContinuation() {
    val complete = Task.forResult(5)
    val error = Task.forError<Int>(RuntimeException())
    val cancelled = Task.cancelled<Int>()
    complete.continueWith { task ->
      assertThat(task).isEqualTo(complete)
      assertThat(task.isCompleted).isTrue
      assertThat(task.result).isEqualTo(5)
      assertThat(task.isFaulted).isFalse
      assertThat(task.isCancelled).isFalse
      null
    }
    error.continueWith { task ->
      assertThat(task).isEqualTo(error)
      assertThat(task.isCompleted).isTrue
      assertThat(task.error is RuntimeException).isTrue
      assertThat(task.isFaulted).isTrue
      assertThat(task.isCancelled).isFalse
      null
    }
    cancelled.continueWith { task ->
      assertThat(task).isEqualTo(cancelled)
      assertThat(cancelled.isCompleted).isTrue
      assertThat(cancelled.isFaulted).isFalse
      assertThat(cancelled.isCancelled).isTrue
      null
    }
  }

  @Test
  fun testSynchronousChaining() {
    val first = Task.forResult(1)
    val second = first.continueWith { 2 }
    val third = second.continueWithTask { Task.forResult(3) }
    assertThat(first.isCompleted).isTrue
    assertThat(second.isCompleted).isTrue
    assertThat(third.isCompleted).isTrue
    assertThat(first.result).isEqualTo(1)
    assertThat(second.result).isEqualTo(2)
    assertThat(third.result).isEqualTo(3)
  }

  @Test
  fun testSynchronousCancellation() {
    val first = Task.forResult(1)
    val second = first.continueWith { throw CancellationException() }
    assertThat(first.isCompleted).isTrue
    assertThat(second.isCancelled).isTrue
  }

  @Test
  fun testSynchronousContinuationTokenAlreadyCancelled() {
    val cts = CancellationTokenSource()
    val continuationRun = Capture(false)
    cts.cancel()
    val first = Task.forResult(1)
    val second =
        first.continueWith(
            {
              continuationRun.set(true)
              2
            },
            cts.token)
    assertThat(first.isCompleted).isTrue
    assertThat(second.isCancelled).isTrue
    assertThat(continuationRun.get()).isFalse
  }

  @Test
  fun testSynchronousTaskCancellation() {
    val first = Task.forResult(1)
    val second =
        first.continueWithTask(Continuation<Int, Task<Int>> { throw CancellationException() })
    assertThat(first.isCompleted).isTrue
    assertThat(second.isCancelled).isTrue
  }

  @Test
  fun testBackgroundCall() {
    runTaskTest {
      Task.callInBackground {
        Thread.sleep(100)
        5
      }
          .continueWith { task ->
            assertThat(task.result).isEqualTo(5)
            null
          }
    }
  }

  @Test
  fun testBackgroundCallTokenCancellation() {
    val cts = CancellationTokenSource()
    val ct = cts.token
    val waitingToBeCancelled = Capture(false)
    val cancelLock = ReentrantLock()
    val cancelCondition = cancelLock.newCondition()
    val task =
        Task.callInBackground {
          cancelLock.lock()
          try {
            waitingToBeCancelled.set(true)
            cancelCondition.await()
          } finally {
            cancelLock.unlock()
          }
          ct.throwIfCancellationRequested()
          5
        }
    while (true) {
      cancelLock.lock()
      try {
        if (waitingToBeCancelled.get()) {
          cts.cancel()
          cancelCondition.signal()
          break
        }
      } finally {
        cancelLock.unlock()
      }
      try {
        Thread.sleep(5)
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      }
    }
    try {
      task.waitForCompletion()
    } catch (e: InterruptedException) {
      throw RuntimeException(e)
    }
    assertThat(task.isCancelled).isTrue
  }

  @Test
  fun testBackgroundCallTokenAlreadyCancelled() {
    val cts = CancellationTokenSource()
    cts.cancel()
    runTaskTest {
      Task.callInBackground(
              {
                Thread.sleep(100)
                5
              },
              cts.token)
          .continueWith { task ->
            assertThat(task.isCancelled).isTrue
            null
          }
    }
  }

  @Test
  fun testBackgroundCallWaiting() {
    val task =
        Task.callInBackground {
          Thread.sleep(100)
          5
        }
    task.waitForCompletion()
    assertThat(task.isCompleted).isTrue
    assertThat(task.result).isEqualTo(5)
  }

  @Test
  fun testBackgroundCallWaitingWithTimeouts() {
    val sync = ReentrantLock()
    val condition = sync.newCondition()
    val task =
        Task.callInBackground {
          sync.lock()
          try {
            condition.await()
            Thread.sleep(100)
          } finally {
            sync.unlock()
          }
          5
        }
    // wait -> timeout
    assertThat(task.waitForCompletion(100, TimeUnit.MILLISECONDS)).isFalse
    sync.lock()
    try {
      condition.signal()
    } finally {
      sync.unlock()
    }
    // wait -> completes
    assertThat(task.waitForCompletion(1000, TimeUnit.MILLISECONDS)).isTrue
    // wait -> already completed
    assertThat(task.waitForCompletion(100, TimeUnit.MILLISECONDS)).isTrue
    assertThat(task.result).isEqualTo(5)
  }

  @Test
  fun testBackgroundCallWaitingOnError() {
    val task =
        Task.callInBackground<Int> {
          Thread.sleep(100)
          throw RuntimeException()
        }
    task.waitForCompletion()
    assertThat(task.isCompleted).isTrue
    assertThat(task.isFaulted).isTrue
  }

  @Test
  fun testBackgroundCallWaitOnCancellation() {
    val task =
        Task.callInBackground {
              Thread.sleep(100)
              5
            }
            .continueWithTask(Continuation<Int, Task<Int>> { Task.cancelled() })
    task.waitForCompletion()
    assertThat(task.isCompleted).isTrue
    assertThat(task.isCancelled).isTrue
  }

  @Test
  fun testBackgroundError() {
    runTaskTest {
      Task.callInBackground<Int> { throw IllegalStateException() }.continueWith { task ->
        assertThat(task.isFaulted).isTrue
        assertThat(task.error is IllegalStateException).isTrue
        null
      }
    }
  }

  @Test
  fun testBackgroundCancellation() {
    runTaskTest {
      Task.callInBackground<Void> { throw CancellationException() }.continueWith { task ->
        assertThat(task.isCancelled).isTrue
        null
      }
    }
  }

  @Suppress("ExplicitGarbageCollectionCall")
  @Test
  fun testUnobservedError() {
    try {
      val lock = ReentrantLock()
      val condition = lock.newCondition()
      Task.setUnobservedExceptionHandler { t, e ->
        lock.lock()
        try {
          condition.signal()
        } finally {
          lock.unlock()
        }
      }

      lock.lock()
      try {
        startFailedTask()
        // Intentionally call GC to invoke unobserved exception handler
        System.gc()
        condition.await()
      } finally {
        lock.unlock()
      }
    } finally {
      Task.setUnobservedExceptionHandler(null)
    }
  }

  @Test
  fun testWhenAllNoTasks() {
    val task = Task.whenAll(ArrayList<Task<Void>>())
    assertThat(task.isCompleted).isTrue
    assertThat(task.isCancelled).isFalse
    assertThat(task.isFaulted).isFalse
  }

  @Test
  fun testWhenAnyResultFirstSuccess() {
    runTaskTest {
      val tasks = ArrayList<Task<Int>>()
      val firstToCompleteSuccess =
          Task.callInBackground {
            Thread.sleep(50)
            10
          }
      tasks.addAll(launchTasksWithRandomCompletions(5))
      tasks.add(firstToCompleteSuccess)
      tasks.addAll(launchTasksWithRandomCompletions(5))
      Task.whenAnyResult(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        assertThat(task.result).isEqualTo(firstToCompleteSuccess)
        assertThat(task.result?.isCompleted).isTrue
        assertThat(task.result?.isCancelled).isFalse
        assertThat(task.result?.isFaulted).isFalse
        assertThat(task.result?.result).isEqualTo(10)
        null
      }
    }
  }

  @Test
  fun testWhenAnyFirstSuccess() {
    runTaskTest {
      val tasks = ArrayList<Task<*>>()
      val firstToCompleteSuccess =
          Task.callInBackground {
            Thread.sleep(50)
            "SUCCESS"
          }
      tasks.addAll(launchTasksWithRandomCompletions(5))
      tasks.add(firstToCompleteSuccess)
      tasks.addAll(launchTasksWithRandomCompletions(5))
      Task.whenAny(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        assertThat(task.result).isEqualTo(firstToCompleteSuccess)
        assertThat(task.result?.isCompleted).isTrue
        assertThat(task.result?.isCancelled).isFalse
        assertThat(task.result?.isFaulted).isFalse
        assertThat(task.result?.result).isEqualTo("SUCCESS")
        null
      }
    }
  }

  @Test
  fun testWhenAnyResultFirstError() {
    val error: Exception = RuntimeException("This task failed.")
    runTaskTest {
      val tasks = ArrayList<Task<Int>>()
      val firstToCompleteError =
          Task.callInBackground<Int> {
            Thread.sleep(50)
            throw error
          }
      tasks.addAll(launchTasksWithRandomCompletions(5))
      tasks.add(firstToCompleteError)
      tasks.addAll(launchTasksWithRandomCompletions(5))
      Task.whenAnyResult(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        assertThat(task.result).isEqualTo(firstToCompleteError)
        assertThat(task.result?.isCompleted).isTrue
        assertThat(task.result?.isCancelled).isFalse
        assertThat(task.result?.isFaulted).isTrue
        assertThat(task.result?.error).isEqualTo(error)
        null
      }
    }
  }

  @Test
  fun testWhenAnyFirstError() {
    val error: Exception = RuntimeException("This task failed.")
    runTaskTest {
      val tasks = ArrayList<Task<*>>()
      val firstToCompleteError =
          Task.callInBackground<String> {
            Thread.sleep(50)
            throw error
          }
      tasks.addAll(launchTasksWithRandomCompletions(5))
      tasks.add(firstToCompleteError)
      tasks.addAll(launchTasksWithRandomCompletions(5))
      Task.whenAny(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        assertThat(task.result).isEqualTo(firstToCompleteError)
        assertThat(task.result?.isCompleted).isTrue
        assertThat(task.result?.isCancelled).isFalse
        assertThat(task.result?.isFaulted).isTrue
        assertThat(task.result?.error).isEqualTo(error)
        null
      }
    }
  }

  @Test
  fun testWhenAnyResultFirstCancelled() {
    runTaskTest {
      val tasks = ArrayList<Task<Int>>()
      val firstToCompleteCancelled =
          Task.callInBackground<Int> {
            Thread.sleep(50)
            throw CancellationException()
          }
      tasks.addAll(launchTasksWithRandomCompletions(5))
      tasks.add(firstToCompleteCancelled)
      tasks.addAll(launchTasksWithRandomCompletions(5))
      Task.whenAnyResult(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        assertThat(task.result).isEqualTo(firstToCompleteCancelled)
        assertThat(task.result?.isCompleted).isTrue
        assertThat(task.result?.isCancelled).isTrue
        assertThat(task.result?.isFaulted).isFalse
        null
      }
    }
  }

  @Test
  fun testWhenAnyFirstCancelled() {
    runTaskTest {
      val tasks = ArrayList<Task<*>>()
      val firstToCompleteCancelled =
          Task.callInBackground<String> {
            Thread.sleep(50)
            throw CancellationException()
          }
      tasks.addAll(launchTasksWithRandomCompletions(5))
      tasks.add(firstToCompleteCancelled)
      tasks.addAll(launchTasksWithRandomCompletions(5))
      Task.whenAny(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        assertThat(task.result).isEqualTo(firstToCompleteCancelled)
        assertThat(task.result?.isCompleted).isTrue
        assertThat(task.result?.isCancelled).isTrue
        assertThat(task.result?.isFaulted).isFalse
        null
      }
    }
  }

  @Test
  fun testWhenAllSuccess() {
    runTaskTest {
      val tasks = ArrayList<Task<Void>>()
      for (i in 0..19) {
        val task =
            Task.callInBackground<Void> {
              Thread.sleep((Math.random() * 100).toLong())
              null
            }
        tasks.add(task)
      }
      Task.whenAll(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        for (t in tasks) {
          assertThat(t.isCompleted).isTrue
        }
        null
      }
    }
  }

  @Test
  fun testWhenAllOneError() {
    val error: Exception = RuntimeException("This task failed.")
    runTaskTest {
      val tasks = ArrayList<Task<Void>>()
      for (i in 0..19) {
        val task =
            Task.callInBackground<Void> {
              Thread.sleep((Math.random() * 100).toLong())
              if (i == 10) {
                throw error
              }
              null
            }
        tasks.add(task)
      }
      Task.whenAll(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isTrue
        assertThat(task.isCancelled).isFalse
        assertThat(task.error is AggregateException).isFalse
        assertThat(task.error).isEqualTo(error)
        for (t in tasks) {
          assertThat(t.isCompleted).isTrue
        }
        null
      }
    }
  }

  @Test
  fun testWhenAllCancel() {
    runTaskTest {
      val tasks = ArrayList<Task<Void?>>()
      for (i in 0..19) {
        val tcs = TaskCompletionSource<Void?>()
        Task.callInBackground<Void> {
          Thread.sleep((Math.random() * 100).toLong())
          if (i == 10) {
            tcs.setCancelled()
          } else {
            tcs.setResult(null)
          }
          null
        }
        tasks.add(tcs.task)
      }
      Task.whenAll(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isTrue
        for (t in tasks) {
          assertThat(t.isCompleted).isTrue
        }
        null
      }
    }
  }

  @Test
  fun testWhenAllResultNoTasks() {
    val task = Task.whenAllResult(ArrayList<Task<Void>>())
    assertThat(task.isCompleted).isTrue
    assertThat(task.isCancelled).isFalse
    assertThat(task.isFaulted).isFalse
    assertThat(task.result?.isEmpty()).isTrue
  }

  @Test
  fun testWhenAllResultSuccess() {
    runTaskTest {
      val tasks: MutableList<Task<Int>> = ArrayList()
      for (i in 0..19) {
        val number = i + 1
        val task =
            Task.callInBackground {
              Thread.sleep((Math.random() * 100).toLong())
              number
            }
        tasks.add(task)
      }
      Task.whenAllResult(tasks).continueWith { task ->
        assertThat(task.isCompleted).isTrue
        assertThat(task.isFaulted).isFalse
        assertThat(task.isCancelled).isFalse
        assertThat(task.result?.size).isEqualTo(tasks.size)
        for (i in tasks.indices) {
          val t = tasks[i]
          assertThat(t.isCompleted).isTrue
          assertThat(t.result)?.isEqualTo(task.result?.get(i))
        }
        null
      }
    }
  }

  @Test
  fun testAsyncChaining() {
    runTaskTest {
      val sequence = ArrayList<Int>()
      var result = Task.forResult<Void?>(null)
      for (i in 0..19) {
        result =
            result.continueWithTask {
              Task.callInBackground {
                sequence.add(i)
                null
              }
            }
      }
      result =
          result.continueWith {
            assertThat(sequence.size).isEqualTo(20)
            for (i in 0..19) {
              assertThat(sequence[i]).isEqualTo(i)
            }
            null
          }
      result
    }
  }

  @Test
  fun testOnSuccess() {
    val continuation: Continuation<Int, Int> =
        Continuation<Int, Int> { task -> checkNotNull(task.result) + 1 }
    val complete = Task.forResult(5).onSuccess(continuation)
    val error = Task.forError<Int>(IllegalStateException()).onSuccess(continuation)
    val cancelled = Task.cancelled<Int>().onSuccess(continuation)
    assertThat(complete.isCompleted).isTrue
    assertThat(complete.result).isEqualTo(6)
    assertThat(complete.isFaulted).isFalse
    assertThat(complete.isCancelled).isFalse
    assertThat(error.isCompleted).isTrue
    assertThat(error.error is RuntimeException).isTrue
    assertThat(error.isFaulted).isTrue
    assertThat(error.isCancelled).isFalse
    assertThat(cancelled.isCompleted).isTrue
    assertThat(cancelled.isFaulted).isFalse
    assertThat(cancelled.isCancelled).isTrue
  }

  @Test
  fun testOnSuccessTask() {
    val continuation: Continuation<Int, Task<Int>> =
        Continuation<Int, Task<Int>> { task -> Task.forResult(checkNotNull(task.result) + 1) }
    val complete = Task.forResult(5).onSuccessTask(continuation)
    val error = Task.forError<Int>(IllegalStateException()).onSuccessTask(continuation)
    val cancelled = Task.cancelled<Int>().onSuccessTask(continuation)
    assertThat(complete.isCompleted).isTrue
    assertThat(complete.result).isEqualTo(6)
    assertThat(complete.isFaulted).isFalse
    assertThat(complete.isCancelled).isFalse
    assertThat(error.isCompleted).isTrue
    assertThat(error.error is RuntimeException).isTrue
    assertThat(error.isFaulted).isTrue
    assertThat(error.isCancelled).isFalse
    assertThat(cancelled.isCompleted).isTrue
    assertThat(cancelled.isFaulted).isFalse
    assertThat(cancelled.isCancelled).isTrue
  }

  @Test
  fun testContinueWhile() {
    val count = AtomicInteger(0)
    runTaskTest {
      Task.forResult<Any>(null)
          .continueWhile({ count.get() < 10 }) {
            count.incrementAndGet()
            null
          }
          .continueWith {
            assertThat(count.get()).isEqualTo(10)
            null
          }
    }
  }

  @Test
  fun testContinueWhileAsync() {
    val count = AtomicInteger(0)
    runTaskTest {
      Task.forResult<Any>(null)
          .continueWhile(
              { count.get() < 10 },
              {
                count.incrementAndGet()
                null
              },
              Executors.newCachedThreadPool())
          .continueWith {
            assertThat(count.get()).isEqualTo(10)
            null
          }
    }
  }

  @Test
  fun testContinueWhileAsyncCancellation() {
    val count = AtomicInteger(0)
    val cts = CancellationTokenSource()
    runTaskTest {
      Task.forResult<Any>(null)
          .continueWhile(
              { count.get() < 10 },
              {
                if (count.incrementAndGet() == 5) {
                  cts.cancel()
                }
                null
              },
              Executors.newCachedThreadPool(),
              cts.token)
          .continueWith { task ->
            assertThat(task.isCancelled).isTrue
            assertThat(count.get()).isEqualTo(5)
            null
          }
    }
  }

  @Test
  fun testCallWithBadExecutor() {
    val exception = RuntimeException("BAD EXECUTORS")
    Task.call({ 1 }) { throw exception }.continueWith { task ->
      assertThat(task.isFaulted).isTrue
      assertThat(task.error is ExecutorException).isTrue
      assertThat(task.error?.cause).isEqualTo(exception)
      null
    }
  }

  @Test
  fun testContinueWithBadExecutor() {
    val exception = RuntimeException("BAD EXECUTORS")
    Task.call { 1 }.continueWith({ task -> task.result }) { throw exception }.continueWith { task ->
      assertThat(task.isFaulted).isTrue
      assertThat(task.error is ExecutorException).isTrue
      assertThat(task.error?.cause).isEqualTo(exception)
      null
    }
  }

  @Test
  fun testContinueWithTaskAndBadExecutor() {
    val exception = RuntimeException("BAD EXECUTORS")
    Task.call { 1 }.continueWithTask({ task -> task }) { throw exception }.continueWith { task ->
      assertThat(task.isFaulted).isTrue
      assertThat(task.error is ExecutorException).isTrue
      assertThat(task.error?.cause).isEqualTo(exception)
      null
    }
  }

  // region TaskCompletionSource
  @Test
  fun testTrySetResult() {
    val tcs = TaskCompletionSource<String>()
    val task = tcs.task
    assertThat(task.isCompleted).isFalse
    val success = tcs.trySetResult("SHOW ME WHAT YOU GOT")
    assertThat(success).isTrue
    assertThat(task.isCompleted).isTrue
    assertThat(task.result).isEqualTo("SHOW ME WHAT YOU GOT")
  }

  @Test
  fun testTrySetError() {
    val tcs = TaskCompletionSource<Void>()
    val task = tcs.task
    assertThat(task.isCompleted).isFalse
    val exception: Exception = RuntimeException("DISQUALIFIED")
    val success = tcs.trySetError(exception)
    assertThat(success).isTrue
    assertThat(task.isCompleted).isTrue
    assertThat(task.error).isEqualTo(exception)
  }

  @Test
  fun testTrySetCanceled() {
    val tcs = TaskCompletionSource<Void>()
    val task = tcs.task
    assertThat(task.isCompleted).isFalse
    val success = tcs.trySetCancelled()
    assertThat(success).isTrue
    assertThat(task.isCompleted).isTrue
    assertThat(task.isCancelled).isTrue
  }

  @Test
  fun testTrySetOnCompletedTask() {
    val tcs = TaskCompletionSource<Void?>()
    tcs.setResult(null)
    assertThat(tcs.trySetResult(null)).isFalse
    assertThat(tcs.trySetError(RuntimeException())).isFalse
    assertThat(tcs.trySetCancelled()).isFalse
  }

  @Test(expected = IllegalStateException::class)
  fun testSetResultOnCompletedTask() {
    val tcs = TaskCompletionSource<Void?>()
    tcs.setResult(null)
    tcs.setResult(null)
  }

  @Test(expected = IllegalStateException::class)
  fun testSetErrorOnCompletedTask() {
    val tcs = TaskCompletionSource<Void?>()
    tcs.setResult(null)
    tcs.setError(RuntimeException())
  }

  @Test(expected = IllegalStateException::class)
  fun testSetCancelledOnCompletedTask() {
    val tcs = TaskCompletionSource<Void?>()
    tcs.setResult(null)
    tcs.setCancelled()
  }
}
