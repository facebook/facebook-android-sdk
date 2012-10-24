package com.facebook;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.security.SecureRandom;

public class PrioritizedWorkQueueTests extends FacebookTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testEmptyValidate() {
        PrioritizedWorkQueue manager = new PrioritizedWorkQueue();
        manager.validate();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testRunSomething() {
        CountingRunnable run = new CountingRunnable();
        assertEquals(0, run.getRunCount());

        ScriptableExecutor executor = new ScriptableExecutor();
        assertEquals(0, executor.getPendingCount());

        PrioritizedWorkQueue manager = new PrioritizedWorkQueue(1, executor);

        addActiveWorkItem(manager, run);
        assertEquals(1, executor.getPendingCount());
        assertEquals(0, run.getRunCount());

        executeNext(manager, executor);
        assertEquals(0, executor.getPendingCount());
        assertEquals(1, run.getRunCount());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testRunSequence() {
        final int workTotal = 100;

        CountingRunnable run = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        PrioritizedWorkQueue manager = new PrioritizedWorkQueue(1, executor);

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

    @SmallTest
    @MediumTest
    @LargeTest
    public void testRunParallel() {
        final int workTotal = 100;

        CountingRunnable run = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        PrioritizedWorkQueue manager = new PrioritizedWorkQueue(workTotal, executor);

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

    @SmallTest
    @MediumTest
    @LargeTest
    public void testSimpleCancel() {
        CountingRunnable run = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        PrioritizedWorkQueue manager = new PrioritizedWorkQueue(1, executor);

        addActiveWorkItem(manager, run);
        PrioritizedWorkQueue.WorkItem work1 = addActiveWorkItem(manager, run);
        cancelWork(manager, work1);

        assertEquals(1, executor.getPendingCount());
        executeNext(manager, executor);
        assertEquals(0, executor.getPendingCount());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testMoveToFront() {
        final int firstCount = 8;
        final int highCount = 17;

        ArrayList<PrioritizedWorkQueue.WorkItem> highWorkItems = new ArrayList<PrioritizedWorkQueue.WorkItem>();
        CountingRunnable highRun = new CountingRunnable();
        CountingRunnable firstRun = new CountingRunnable();
        CountingRunnable lowRun = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();
        PrioritizedWorkQueue manager = new PrioritizedWorkQueue(firstCount, executor);

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
        for (PrioritizedWorkQueue.WorkItem highItem : highWorkItems) {
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

    @LargeTest
    public void testThreadStress() {
        PrioritizedWorkQueue manager = new PrioritizedWorkQueue();
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

    private PrioritizedWorkQueue.WorkItem addActiveWorkItem(PrioritizedWorkQueue manager, Runnable runnable) {
        manager.validate();
        PrioritizedWorkQueue.WorkItem workItem = manager.addActiveWorkItem(runnable);
        manager.validate();
        return workItem;
    }

    private void executeNext(PrioritizedWorkQueue manager, ScriptableExecutor executor) {
        manager.validate();
        executor.runNext();
        manager.validate();
    }

    private void cancelWork(PrioritizedWorkQueue manager, PrioritizedWorkQueue.WorkItem workItem) {
        manager.validate();
        workItem.cancel();
        manager.validate();
    }

    private void prioritizeWork(PrioritizedWorkQueue manager, PrioritizedWorkQueue.WorkItem workItem) {
        manager.validate();
        workItem.moveToFront();
        manager.validate();
    }

    static class StressRunnable implements Runnable {
        static ArrayList<PrioritizedWorkQueue.WorkItem> tracked = new ArrayList<PrioritizedWorkQueue.WorkItem>();

        final PrioritizedWorkQueue manager;
        final SecureRandom random = new SecureRandom();
        final int iterationCount;
        int iterationIndex = 0;
        boolean isDone = false;

        StressRunnable(PrioritizedWorkQueue manager, int iterationCount) {
            this.manager = manager;
            this.iterationCount = iterationCount;
        }

        @Override
        public void run() {
            // Each iteration runs a random action against the PrioritizedWorkQueue.
            if (iterationIndex++ < iterationCount) {
                final int sleepWeight = 80;
                final int trackThisWeight = 10;
                final int prioritizeTrackedWeight = 6;
                final int validateWeight = 2;
                int weight = 0;
                final int n = random.nextInt(sleepWeight + trackThisWeight + prioritizeTrackedWeight + validateWeight);
                PrioritizedWorkQueue.WorkItem workItem = manager.addActiveWorkItem(this);

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
                    ArrayList<PrioritizedWorkQueue.WorkItem> items = new ArrayList<PrioritizedWorkQueue.WorkItem>();

                    synchronized (tracked) {
                        items.addAll(tracked);
                        tracked.clear();
                    }

                    for (PrioritizedWorkQueue.WorkItem item : items) {
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

    class ScriptableExecutor implements Executor {
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

    class CountingRunnable implements Runnable {
        volatile int runCount = 0;

        int getRunCount() {
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
