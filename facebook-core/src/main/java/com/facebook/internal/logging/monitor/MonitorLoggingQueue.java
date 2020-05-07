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

package com.facebook.internal.logging.monitor;

import com.facebook.internal.logging.ExternalLog;
import com.facebook.internal.logging.LoggingCache;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class MonitorLoggingQueue implements LoggingCache {
    private static MonitorLoggingQueue monitorLoggingQueue;
    private Queue<ExternalLog> logQueue = new LinkedList<>();

    private static final Integer FLUSH_LIMIT = 100;

    private MonitorLoggingQueue() {
    }

    public synchronized static MonitorLoggingQueue getInstance() {
        if (monitorLoggingQueue == null) {
            monitorLoggingQueue = new MonitorLoggingQueue();
        }
        return monitorLoggingQueue;
    }

    /**
     * Add single log in log queue
     * @return true if the log queue's size has reached the flush limit after adding the new log
     */
    @Override
    public boolean addLog(ExternalLog log) {
        return addLogs(Arrays.asList(log));
    }

    /**
     * Add multiple logs in log queue
     * @return true if the log queue's size has reached the flush limit after adding the new log
     */
    @Override
    public boolean addLogs(Collection<? extends ExternalLog> logs) {
        if (logs != null) {
            logQueue.addAll(logs);
        }
        return hasReachedFlushLimit();
    }

    /**
     * Log queue has a FLUSH_LIMIT
     * @return true if the log queue's size has reached the flush limit
     */
    private boolean hasReachedFlushLimit() {
        return logQueue.size() >= FLUSH_LIMIT;
    }

    @Override
    public boolean isEmpty() {
        return logQueue.isEmpty();
    }

    @Override
    public ExternalLog fetchLog() {
        return logQueue.poll();
    }

    @Override
    public Collection<ExternalLog> fetchAllLogs() {
        Collection<ExternalLog> logs = new LinkedList<>(this.logQueue);
        this.logQueue.clear();
        return logs;
    }
}
