/**
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

package com.facebook.internal;

import com.facebook.FacebookTestCase;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import static org.junit.Assert.*;

public class WorkQueueTest extends FacebookTestCase {

    @Test
    public void testEmptyValidate() {
        WorkQueue manager = new WorkQueue();
        manager.validate();
    }

    @Test
    public void testRunSomething() {
        CountingRunnable run = new CountingRunnable();
        assertEquals(0, run.getRunCount());

        ScriptableExecutor executor = new ScriptableExecutor();
        assertEquals(0, executor.getPendingCount());

        WorkQueue manager = new WorkQueue(1, executor);

        addActiveWorkItem(manager, run);
        assertEquals(1, executor.getPendingCount());
        assertEquals(0, run.getRunCount());

        executeNext(manager, executor);
        assertEquals(0, executor.getPendingCount());
        assertEquals(1, run.getRunCount());
    }

    @Test
    public void testRunSequence() {
        final int workTotal = 100;

        CountingRunnable run = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        WorkQueue manager = new WorkQueue(1, executor);

        for (int i = 0; i < workTotal; i++) {
            addActiveWorkItem(manager, run);
            assertEquals(1, executor.getPendingCount());
        }

        for (int i = 0; i < workTotal; i++) {
            assertEquals(1, executor.getPendingCount());
            assertEquals(i, run.getRunCount());
            executeNext(manager, executor);
        }
        assertEquals(0, executor.getPendingCount());
        assertEquals(workTotal, run.getRunCount());
    }

    @Test
    public void testRunParallel() {
        final int workTotal = 100;

        CountingRunnable run = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        WorkQueue manager = new WorkQueue(workTotal, executor);

        for (int i = 0; i < workTotal; i++) {
            assertEquals(i, executor.getPendingCount());
            addActiveWorkItem(manager, run);
        }

        for (int i = 0; i < workTotal; i++) {
            assertEquals(workTotal - i, executor.getPendingCount());
            assertEquals(i, run.getRunCount());
            executeNext(manager, executor);
        }
        assertEquals(0, executor.getPendingCount());
        assertEquals(workTotal, run.getRunCount());
    }

    @Test
    public void testSimpleCancel() {
        CountingRunnable run = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        WorkQueue manager = new WorkQueue(1, executor);

        addActiveWorkItem(manager, run);
        WorkQueue.WorkItem work1 = addActiveWorkItem(manager, run);
        cancelWork(manager, work1);

        assertEquals(1, executor.getPendingCount());
        executeNext(manager, executor);
        assertEquals(0, executor.getPendingCount());
    }

    @Test
    public void testMoveToFront() {
        final int firstCount = 8;
        final int highCount = 17;

        ArrayList<WorkQueue.WorkItem> highWorkItems = new ArrayList<WorkQueue.WorkItem>();
        CountingRunnable highRun = new CountingRunnable();
        CountingRunnable firstRun = new CountingRunnable();
        CountingRunnable lowRun = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        WorkQueue manager = new WorkQueue(firstCount, executor);

        for (int i = 0; i < firstCount; i++) {
            addActiveWorkItem(manager, firstRun);
        }

        int lowCount = 0;
        for (int h = 0; h < highCount; h++) {
            highWorkItems.add(addActiveWorkItem(manager, highRun));
            for (int l = 0; l < h; l++) {
                addActiveWorkItem(manager, lowRun);
                lowCount++;
            }
        }

        assertEquals(firstCount, executor.getPendingCount());
        for (WorkQueue.WorkItem highItem : highWorkItems) {
            prioritizeWork(manager, highItem);
        }

        for (int i = 0; i < firstCount; i++) {
            assertEquals(i, firstRun.getRunCount());
            executeNext(manager, executor);
        }

        for (int i = 0; i < highCount; i++) {
            assertEquals(i, highRun.getRunCount());
            executeNext(manager, executor);
        }

        for (int i = 0; i < lowCount; i++) {
            assertEquals(i, lowRun.getRunCount());
            executeNext(manager, executor);
        }

        assertEquals(firstCount, firstRun.getRunCount());
        assertEquals(highCount, highRun.getRunCount());
        assertEquals(lowCount, lowRun.getRunCount());
    }

    // Test cancelling running work item, completed work item
    @Test
    public void testThreadStress() {
        WorkQueue manager = new WorkQueue();
        ArrayList<StressRunnable> runnables = new ArrayList<StressRunnable>();
        final int threadCount = 20;

        for (int i = 0; i < threadCount; i++) {
            runnables.add(new StressRunnable(manager, 20));
        }

        for (int i = 0; i < threadCount; i++) {
            manager.addActiveWorkItem(runnables.get(i));
        }

        for (int i = 0; i < threadCount; i++) {
            runnables.get(i).waitForDone();
        }
    }

    private WorkQueue.WorkItem addActiveWorkItem(WorkQueue manager, Runnable runnable) {
        manager.validate();
        WorkQueue.WorkItem workItem = manager.addActiveWorkItem(runnable);
        manager.validate();
        return workItem;
    }

    private void executeNext(WorkQueue manager, ScriptableExecutor executor) {
        manager.validate();
        executor.runNext();
        manager.validate();
    }

    private void cancelWork(WorkQueue manager, WorkQueue.WorkItem workItem) {
        manager.validate();
        workItem.cancel();
        manager.validate();
    }

    private void prioritizeWork(WorkQueue manager, WorkQueue.WorkItem workItem) {
        manager.validate();
        workItem.moveToFront();
        manager.validate();
    }

    static class StressRunnable implements Runnable {
        static ArrayList<WorkQueue.WorkItem> tracked = new ArrayList<WorkQueue.WorkItem>();

        final WorkQueue manager;
        final SecureRandom random = new SecureRandom();
        final int iterationCount;
        int iterationIndex = 0;
        boolean isDone = false;

        StressRunnable(WorkQueue manager, int iterationCount) {
            this.manager = manager;
            this.iterationCount = iterationCount;
        }

        @Override
        public void run() {
            // Each iteration runs a random action against the WorkQueue.
            if (iterationIndex++ < iterationCount) {
                final int sleepWeight = 80;
                final int trackThisWeight = 10;
                final int prioritizeTrackedWeight = 6;
                final int validateWeight = 2;
                int weight = 0;
                final int n = random.nextInt(sleepWeight + trackThisWeight + prioritizeTrackedWeight + validateWeight);
                WorkQueue.WorkItem workItem = manager.addActiveWorkItem(this);

                if (n < (weight += sleepWeight)) {
                    // Sleep
                    try {
                        Thread.sleep(n/4);
                    } catch (InterruptedException e) {
                    }
                } else if (n < (weight += trackThisWeight)) {
                    // Track this work item to activate later
                    synchronized (tracked) {
                        tracked.add(workItem);
                    }
                } else if (n < (weight += prioritizeTrackedWeight)) {
                    // Background all pending items, prioritize tracked items, and clear tracked list
                    ArrayList<WorkQueue.WorkItem> items = new ArrayList<WorkQueue.WorkItem>();

                    synchronized (tracked) {
                        items.addAll(tracked);
                        tracked.clear();
                    }

                    for (WorkQueue.WorkItem item : items) {
                        item.moveToFront();
                    }
                } else {
                    // Validate
                    manager.validate();
                }
            } else {
                // Also have all threads validate once they are done.
                manager.validate();
                synchronized (this) {
                    isDone = true;
                    this.notifyAll();
                }
            }
        }

        void waitForDone() {
            synchronized (this) {
                while (!isDone) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    static class ScriptableExecutor implements Executor {
        private final ArrayList<Runnable> runnables = new ArrayList<Runnable>();

        int getPendingCount() {
            return runnables.size();
        }

        void runNext() {
            assertTrue(runnables.size() > 0);
            runnables.get(0).run();
            runnables.remove(0);
        }

        void runLast() {
            assertTrue(runnables.size() > 0);
            int index = runnables.size() - 1;
            runnables.get(index).run();
            runnables.remove(index);
        }

        @Override
        public void execute(Runnable runnable) {
            synchronized (this) {
                runnables.add(runnable);
            }
        }
    }

    static class CountingRunnable implements Runnable {
        private int runCount = 0;

        synchronized int getRunCount() {
            return runCount;
        }

        @Override
        public void run() {
            synchronized (this) {
                runCount++;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }
}
