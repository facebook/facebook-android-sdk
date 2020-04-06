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
