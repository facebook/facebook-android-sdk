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

package com.facebook.internal.logging;

import java.util.Collection;

/**
 *  LoggingCache will store the logs in the memory temporarily,
 *  the logs will be sent to the server or write into the disk
 */
public interface LoggingCache {

    /**
     *  @return true if the size of LoggingCache has reached the flush limit after adding new log
     */
    boolean addLog(ExternalLog log);

    /**
     *  @return true if the size of LoggingCache has reached the flush limit after adding new logs
     */
    boolean addLogs(Collection<? extends ExternalLog> logs);

    boolean isEmpty();

    /**
     * fetch ExternalLog from LoggingCache
     */
    ExternalLog fetchLog();
}
