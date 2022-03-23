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

package com.facebook.internal

import com.facebook.FacebookSdk
import com.facebook.FacebookTestCase
import java.security.SecureRandom
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class WorkQueueTest : FacebookTestCase() {
  @Before
  fun init() {
    FacebookSdk.setExecutor(Executors.newFixedThreadPool(5))
  }

  @Test
  fun `test empty validate`() {
    val manager = WorkQueue()
    manager.validate()
  }

  @Test
  fun `test run something`() {
    val run = CountingRunnable()
    Assert.assertEquals(0, run.runCount)
    val executor = ScriptableExecutor()
    Assert.assertEquals(0, executor.pendingCount)
    val manager = WorkQueue(1, executor)
    addActiveWorkItem(manager, run)
    Assert.assertEquals(1, executor.pendingCount)
    Assert.assertEquals(0, run.runCount)
    executeNext(manager, executor)
    Assert.assertEquals(0, executor.pendingCount)
    Assert.assertEquals(1, run.runCount)
  }

  @Test
  fun `test run sequence`() {
    val workTotal = 100
    val run = CountingRunnable()
    val executor = ScriptableExecutor()
    val manager = WorkQueue(1, executor)
    for (i in 0 until workTotal) {
      addActiveWorkItem(manager, run)
      Assert.assertEquals(1, executor.pendingCount)
    }
    for (i in 0 until workTotal) {
      Assert.assertEquals(1, executor.pendingCount)
      Assert.assertEquals(i, run.runCount)
      executeNext(manager, executor)
    }
    Assert.assertEquals(0, executor.pendingCount)
    Assert.assertEquals(workTotal, run.runCount)
  }

  @Test
  fun `test run parallel`() {
    val workTotal = 100
    val run = CountingRunnable()
    val executor = ScriptableExecutor()
    val manager = WorkQueue(workTotal, executor)
    for (i in 0 until workTotal) {
      Assert.assertEquals(i, executor.pendingCount)
      addActiveWorkItem(manager, run)
    }
    for (i in 0 until workTotal) {
      Assert.assertEquals(workTotal - i, executor.pendingCount)
      Assert.assertEquals(i, run.runCount)
      executeNext(manager, executor)
    }
    Assert.assertEquals(0, executor.pendingCount)
    Assert.assertEquals(workTotal, run.runCount)
  }

  @Test
  fun `test simple cancel`() {
    val run = CountingRunnable()
    val executor = ScriptableExecutor()
    val manager = WorkQueue(1, executor)
    addActiveWorkItem(manager, run)
    val work1 = addActiveWorkItem(manager, run)
    cancelWork(manager, work1)
    Assert.assertEquals(1, executor.pendingCount)
    executeNext(manager, executor)
    Assert.assertEquals(0, executor.pendingCount)
  }

  @Test
  fun `test move to front`() {
    val firstCount = 8
    val highCount = 17
    val highWorkItems = ArrayList<WorkQueue.WorkItem>()
    val highRun = CountingRunnable()
    val firstRun = CountingRunnable()
    val lowRun = CountingRunnable()
    val executor = ScriptableExecutor()
    val manager = WorkQueue(firstCount, executor)
    for (i in 0 until firstCount) {
      addActiveWorkItem(manager, firstRun)
    }
    var lowCount = 0
    for (h in 0 until highCount) {
      highWorkItems.add(addActiveWorkItem(manager, highRun))
      repeat(h) {
        addActiveWorkItem(manager, lowRun)
        lowCount++
      }
    }
    Assert.assertEquals(firstCount, executor.pendingCount)
    for (highItem in highWorkItems) {
      prioritizeWork(manager, highItem)
    }
    for (i in 0 until firstCount) {
      Assert.assertEquals(i, firstRun.runCount)
      executeNext(manager, executor)
    }
    for (i in 0 until highCount) {
      Assert.assertEquals(i, highRun.runCount)
      executeNext(manager, executor)
    }
    for (i in 0 until lowCount) {
      Assert.assertEquals(i, lowRun.runCount)
      executeNext(manager, executor)
    }
    Assert.assertEquals(firstCount, firstRun.runCount)
    Assert.assertEquals(highCount, highRun.runCount)
    Assert.assertEquals(lowCount, lowRun.runCount)
  }

  // Test cancelling running work item, completed work item
  @Test
  fun `test thread stress`() {
    val manager = WorkQueue()
    val runnables = ArrayList<StressRunnable>()
    val threadCount = 20
    for (i in 0 until threadCount) {
      runnables.add(StressRunnable(manager, 20))
    }
    for (i in 0 until threadCount) {
      manager.addActiveWorkItem(runnables[i])
    }
    for (i in 0 until threadCount) {
      runnables[i].waitForDone()
    }
  }

  private fun addActiveWorkItem(manager: WorkQueue, runnable: Runnable): WorkQueue.WorkItem {
    manager.validate()
    val workItem = manager.addActiveWorkItem(runnable)
    manager.validate()
    return workItem
  }

  private fun executeNext(manager: WorkQueue, executor: ScriptableExecutor) {
    manager.validate()
    executor.runNext()
    manager.validate()
  }

  private fun cancelWork(manager: WorkQueue, workItem: WorkQueue.WorkItem) {
    manager.validate()
    workItem.cancel()
    manager.validate()
  }

  private fun prioritizeWork(manager: WorkQueue, workItem: WorkQueue.WorkItem) {
    manager.validate()
    workItem.moveToFront()
    manager.validate()
  }

  internal class StressRunnable(val manager: WorkQueue, val iterationCount: Int) : Runnable {
    private val random = SecureRandom()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    var iterationIndex = 0
    var isDone = false
    override fun run() {
      // Each iteration runs a random action against the WorkQueue.
      if (iterationIndex++ < iterationCount) {
        val sleepWeight = 80
        val trackThisWeight = 10
        val prioritizeTrackedWeight = 6
        val validateWeight = 2
        var weight = 0
        val n =
            random.nextInt(sleepWeight + trackThisWeight + prioritizeTrackedWeight + validateWeight)
        val workItem = manager.addActiveWorkItem(this)
        if (n <
            sleepWeight.let {
              weight += it
              weight
            }) {
          // Sleep
          try {
            Thread.sleep(n / 4L)
          } catch (e: InterruptedException) {}
        } else if (n <
            trackThisWeight.let {
              weight += it
              weight
            }) {
          // Track this work item to activate later
          synchronized(tracked) { tracked.add(workItem) }
        } else if (n <
            prioritizeTrackedWeight.let {
              weight += it
              weight
            }) {
          // Background all pending items, prioritize tracked items, and clear tracked list
          val items = ArrayList<WorkQueue.WorkItem>()
          synchronized(tracked) {
            items.addAll(tracked)
            tracked.clear()
          }
          for (item in items) {
            item.moveToFront()
          }
        } else {
          // Validate
          manager.validate()
        }
      } else {
        // Also have all threads validate once they are done.
        manager.validate()
        lock.withLock {
          isDone = true
          condition.signalAll()
        }
      }
    }

    fun waitForDone() {
      lock.withLock {
        while (!isDone) {
          try {
            condition.await()
          } catch (e: InterruptedException) {}
        }
      }
    }

    companion object {
      var tracked = ArrayList<WorkQueue.WorkItem>()
    }
  }

  internal class ScriptableExecutor : Executor {
    private val runnables = ArrayList<Runnable>()
    val pendingCount: Int
      get() = runnables.size

    fun runNext() {
      assertThat(runnables.size).isGreaterThan(0)
      runnables[0].run()
      runnables.removeAt(0)
    }

    fun runLast() {
      assertThat(runnables.size).isGreaterThan(0)
      val index = runnables.size - 1
      runnables[index].run()
      runnables.removeAt(index)
    }

    override fun execute(runnable: Runnable) {
      synchronized(this) { runnables.add(runnable) }
    }
  }

  internal class CountingRunnable : Runnable {
    @get:Synchronized
    var runCount = 0
      private set

    override fun run() {
      synchronized(this) { runCount++ }
      try {
        Thread.sleep(1)
      } catch (e: InterruptedException) {}
    }
  }
}
