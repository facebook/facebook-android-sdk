/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.internal.logging.monitor;

import com.facebook.internal.logging.LogCategory;
import com.facebook.internal.logging.LogEvent;

public class MonitorLoggingTestUtil {
    static final String TEST_APP_ID = "TEST_APP_ID";
    static final LogCategory TEST_CATEGORY = LogCategory.PERFORMANCE;
    static final int TEST_TIME_SPENT = 20000;
    static final int TEST_TIME_START = 100;
    static final int TEST_INVALID_TIME_SPENT = -50;
    static final long TEST_INVALID_TIME_START = -100;
    static final String TEST_EVENT_NAME = "FB_CORE_STARTUP";
    static final String TEST_PACKAGE_NAME = "com.facebook.fbloginsample";
    static final int INVALID_TIME = -1;
    static final long INVALID_TIME_LONG = -1;
    static final LogEvent TEST_LOG_EVENT = new LogEvent(TEST_EVENT_NAME, TEST_CATEGORY);
    static final int TEST_SAMPLING_RATE = 100;
    static final Integer TEST_DEFAULT_SAMPLING_RATE = 20;

    public static MonitorLog getTestMonitorLog(long timeStart, int timeSpent) {
        MonitorLog log = new MonitorLog.LogBuilder(TEST_LOG_EVENT)
                .timeStart(timeStart)
                .timeSpent(timeSpent)
                .build();
        return log;
    }

    public static MonitorLog getTestMonitorLog(long timeStart) {
        return MonitorLoggingTestUtil.getTestMonitorLog(timeStart, TEST_TIME_SPENT);
    }
}
