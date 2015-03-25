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

import com.facebook.FacebookSdk;

import java.util.concurrent.Executor;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class WorkQueue {
    public static final int DEFAULT_MAX_CONCURRENT = 8;

    private final Object workLock = new Object();
    private WorkNode pendingJobs;

    private final int maxConcurrent;
    private final Executor executor;

    private WorkNode runningJobs = null;
    private int runningCount = 0;

    public WorkQueue() {
        this(DEFAULT_MAX_CONCURRENT);
    }

    public WorkQueue(int maxConcurrent) {
        this(maxConcurrent, FacebookSdk.getExecutor());
    }

    public WorkQueue(int maxConcurrent, Executor executor) {
        this.maxConcurrent = maxConcurrent;
        this.executor = executor;
    }

    public WorkItem addActiveWorkItem(Runnable callback) {
        return addActiveWorkItem(callback, true);
    }

    public WorkItem addActiveWorkItem(Runnable callback, boolean addToFront) {
        WorkNode node = new WorkNode(callback);
        synchronized (workLock) {
            pendingJobs = node.addToList(pendingJobs, addToFront);
        }

        startItem();
        return node;
    }

    public void validate() {
        synchronized (workLock) {
            // Verify that all running items know they are running, and counts match
            int count = 0;

            if (runningJobs != null) {
                WorkNode walk = runningJobs;
                do {
                    walk.verify(true);
                    count++;
                    walk = walk.getNext();
                } while (walk != runningJobs);
            }

            assert runningCount == count;
        }
    }

    private void startItem() {
        finishItemAndStartNew(null);
    }

    private void finishItemAndStartNew(WorkNode finished) {
        WorkNode ready = null;

        synchronized (workLock) {
            if (finished != null) {
                runningJobs = finished.removeFromList(runningJobs);
                runningCount--;
            }

            if (runningCount < maxConcurrent) {
                ready = pendingJobs; // Head of the pendingJobs queue
                if (ready != null) {
                    // The Queue reassignments are necessary since 'ready' might have been
                    // added / removed from the front of either queue, which changes its
                    // respective head.
                    pendingJobs = ready.removeFromList(pendingJobs);
                    runningJobs = ready.addToList(runningJobs, false);
                    runningCount++;

                    ready.setIsRunning(true);
                }
            }
        }

        if (ready != null) {
            execute(ready);
        }
    }

    private void execute(final WorkNode node) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    node.getCallback().run();
                } finally {
                    finishItemAndStartNew(node);
                }
            }
        });
    }

    private class WorkNode implements WorkItem {
        private final Runnable callback;
        private WorkNode next;
        private WorkNode prev;
        private boolean isRunning;

        WorkNode(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public boolean cancel() {
            synchronized (workLock) {
                if (!isRunning()) {
                    pendingJobs = removeFromList(pendingJobs);
                    return true;
                }
            }

            return false;
        }

        @Override
        public void moveToFront() {
            synchronized (workLock) {
                if (!isRunning()) {
                    pendingJobs = removeFromList(pendingJobs);
                    pendingJobs = addToList(pendingJobs, true);
                }
            }
        }

        @Override
        public boolean isRunning() {
            return isRunning;
        }

        Runnable getCallback() {
            return callback;
        }

        WorkNode getNext() {
            return next;
        }

        void setIsRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        WorkNode addToList(WorkNode list, boolean addToFront) {
            assert next == null;
            assert prev == null;

            if (list == null) {
                list = next = prev = this;
            } else {
                next = list;
                prev = list.prev;
                next.prev = prev.next = this;
            }

            return addToFront ? this : list;
        }

        WorkNode removeFromList(WorkNode list) {
            assert next != null;
            assert prev != null;

            if (list == this) {
                if (next == this) {
                    list = null;
                } else {
                    list = next;
                }
            }

            next.prev = prev;
            prev.next = next;
            next = prev = null;

            return list;
        }

        void verify(boolean shouldBeRunning) {
            assert prev.next == this;
            assert next.prev == this;
            assert isRunning() == shouldBeRunning;
        }
    }

    interface WorkItem {
        boolean cancel();
        boolean isRunning();
        void moveToFront();
    }
}
