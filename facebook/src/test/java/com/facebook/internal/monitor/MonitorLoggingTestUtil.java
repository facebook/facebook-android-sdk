package com.facebook.internal.monitor;

import com.facebook.internal.logging.monitor.MonitorLog;

import static com.facebook.internal.logging.monitor.MonitorEvent.FB_CORE_STARTUP;

public class MonitorLoggingTestUtil {
    public static final int TEST_TIME_SPENT = 20000;
    public static final int TEST_INVALID_TIME_SPENT = -50;
    public static final long TEST_INVALID_TIME_START = -100;

    public static MonitorLog getTestMonitorLog(long timeStart, int timeSpent) {
        MonitorLog log = new MonitorLog.LogBuilder(FB_CORE_STARTUP)
                .timeStart(timeStart)
                .timeSpent(timeSpent)
                .build();
        return log;
    }

    public static MonitorLog getTestMonitorLog(long timeStart) {
        return MonitorLoggingTestUtil.getTestMonitorLog(timeStart, TEST_TIME_SPENT);
    }
}
