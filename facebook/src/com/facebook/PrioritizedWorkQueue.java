package com.facebook;

import java.util.concurrent.Executor;

class PrioritizedWorkQueue {
    public static final int PRIORITY_RUNNING = -1;
    public static final int PRIORITY_ACTIVE = 0;
    public static final int PRIORITY_BACKGROUND = 1;
    private static final int PRIORITY_COUNT = 2;
    private static final int DEFAULT_MAX_CONCURRENT = 8;

    private final WorkNode[] queues = new WorkNode[PRIORITY_COUNT];
    private final int maxConcurrent;
    private final Executor executor;

    private WorkNode runningItems = null;
    private int runningCount = 0;

    PrioritizedWorkQueue() {
        this(DEFAULT_MAX_CONCURRENT, Settings.getExecutor());
    }

    PrioritizedWorkQueue(int maxConcurrent, Executor executor) {
        this.maxConcurrent = maxConcurrent;
        this.executor = executor;
    }

    WorkItem addActiveWorkItem(Runnable callback) {
        WorkNode node = new WorkNode(callback);
        synchronized (queues) {
            queues[node.priority] = node.addToList(queues[node.priority]);
        }

        startItem();
        return node;
    }

    void backgroundAll() {
        setPriorityOnAll(PRIORITY_BACKGROUND);
    }

    void validate() {
        synchronized (queues) {
            // Verify that priority on items agrees with the priority of the queue they are in
            for (int priority = 0; priority < PRIORITY_COUNT; priority++) {
                if (queues[priority] != null) {
                    WorkNode walk = queues[priority];
                    do {
                        walk.verify(priority);
                        walk = walk.getNext();
                    } while (walk != queues[priority]);
                }
            }

            // Verify that all running items know they are running, and counts match
            int count = 0;

            if (runningItems != null) {
                WorkNode walk = runningItems;
                do {
                    walk.verify(PRIORITY_RUNNING);
                    count++;
                    walk = walk.getNext();
                } while (walk != runningItems);
            }

            assert runningCount == count;
        }
    }

    private void startItem() {
        finishItemAndStartNew(null);
    }

    private void finishItemAndStartNew(WorkNode finished) {
        WorkNode ready = null;

        synchronized (queues) {
            if (finished != null) {
                runningItems = finished.removeFromList(runningItems);
                runningCount--;
            }

            if (runningCount < maxConcurrent) {
                ready = extractNextReadyItem();

                if (ready != null) {
                    ready.setPriorityRunning();
                    runningItems = ready.addToList(runningItems);
                    runningCount++;
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

    private WorkNode extractNextReadyItem() {
        for (int priority = 0; priority < PRIORITY_COUNT; priority++) {
            WorkNode ready = queues[priority];
            if (ready != null) {
                queues[priority] = ready.removeFromList(queues[priority]);
                return ready;
            }
        }

        return null;
    }

    private void setPriorityOnAll(int priority) {
        synchronized (queues) {
            for (int i = 0; i < PRIORITY_COUNT; i++) {
                if (i != priority) {
                    WorkNode move = queues[i];
                    if (move != null) {
                        do {
                            move.priority = priority;
                            move = move.getNext();
                        } while (move != queues[i]);

                        queues[i] = null;
                        queues[priority] = move.spliceLists(queues[priority]);
                    }
                }
            }
        }
    }

    private class WorkNode implements WorkItem {
        private final Runnable callback;
        private int priority;
        private WorkNode next;
        private WorkNode prev;

        WorkNode(Runnable callback) {
            this.callback = callback;
            this.priority = PRIORITY_ACTIVE;
        }

        @Override
        public boolean cancel() {
            synchronized (queues) {
                if ((priority != PRIORITY_RUNNING) && (next != null)) {
                    queues[priority] = removeFromList(queues[priority]);
                    return true;
                }
            }

            return false;
        }

        @Override
        public void setPriority(int newPriority) {
            assert priority >= 0;
            assert priority < PRIORITY_COUNT;

            synchronized (queues) {
                if (priority != PRIORITY_RUNNING) {
                    if (next != null) {
                        queues[priority] = removeFromList(queues[priority]);
                    }
                    priority = newPriority;
                    queues[priority] = addToList(queues[priority]);
                }
            }
        }

        @Override
        public int getPriority() {
            return priority;
        }

        void setPriorityRunning() {
            synchronized (queues) {
                priority = PRIORITY_RUNNING;
            }
        }

        Runnable getCallback() {
            return callback;
        }

        WorkNode getNext() {
            return next;
        }

        WorkNode addToList(WorkNode list) {
            assert next == null;
            assert prev == null;

            if (list == null) {
                list = next = prev = this;
            } else {
                next = list;
                prev = list.prev;
                next.prev = prev.next = this;
            }

            return list;
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

        WorkNode spliceLists(WorkNode list) {
            if (list == null) {
                list = this;
            } else {
                WorkNode listPrev = list.prev;

                list.prev = prev;
                prev.next = list;

                listPrev.next = this;
                prev = listPrev;
            }

            return list;
        }

        void verify(int expectedPriority) {
            assert priority == expectedPriority;
            assert prev.next == this;
            assert next.prev == this;
        }
    }

    interface WorkItem {
        boolean cancel();
        int getPriority();
        void setPriority(int priority);
    }
}
