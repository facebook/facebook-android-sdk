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
package com.facebook.internal.metrics;

import android.os.SystemClock;
import androidx.annotation.RestrictTo;
import com.facebook.internal.Utility;
import com.facebook.internal.logging.LogCategory;
import com.facebook.internal.logging.LogEvent;
import com.facebook.internal.logging.monitor.MonitorLog;
import com.facebook.internal.logging.monitor.PerformanceEventName;
import java.util.HashMap;
import java.util.Map;

/*
   startMeasureFor and stopMeasureFor should always be called in pairs.
   Possible issue is that the scheduler might switch threads in between the calls, i.e
    1) call to startMeasureFor is made
    2) expectation is that the code you want to measure is ran here
    3) thread switches and unrelated code runs
    4) thread switches back to your code
    5) stopMeasureFor is called, now it has logged more time than actually was spent in your context

    Measure as little code as possible and make sure you are measuring the right thing in the right places.
*/

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MetricsUtil {
  private static MetricsUtil metricsUtil;
  private static final String CLASS_TAG = MetricsUtil.class.getCanonicalName();
  private final Map<PerformanceEventName, TempMetrics> metricsDataMap = new HashMap<>();
  protected static final int INVALID_TIME = -1;

  private MetricsUtil() {}

  public static synchronized MetricsUtil getInstance() {
    if (metricsUtil == null) {
      metricsUtil = new MetricsUtil();
    }
    return metricsUtil;
  }

  /*
     This method will override previous metrics data.
  */
  public void startMeasureFor(PerformanceEventName eventName) {
    long timeStart = SystemClock.elapsedRealtime();
    TempMetrics tempMetrics = new TempMetrics(timeStart);
    metricsDataMap.put(eventName, tempMetrics);
  }

  /*
     Return MonitorLog with default invalid value if startMeasureFor has not been called before
  */
  public MonitorLog stopMeasureFor(PerformanceEventName eventName) {
    long timeEnd = SystemClock.elapsedRealtime();
    LogEvent logEvent = new LogEvent(eventName.toString(), LogCategory.PERFORMANCE);
    MonitorLog monitorLog = new MonitorLog.LogBuilder(logEvent).timeSpent(INVALID_TIME).build();
    if (!metricsDataMap.containsKey(eventName)) {
      StringBuilder warningMessage = new StringBuilder();
      warningMessage.append("Can't measure for ");
      warningMessage.append(eventName);
      warningMessage.append(", startMeasureFor hasn't been called before.");

      Utility.logd(CLASS_TAG, warningMessage.toString());
      return monitorLog;
    }
    TempMetrics tempMetrics = metricsDataMap.get(eventName);
    if (tempMetrics != null) {
      long timeStart = tempMetrics.timeStart;
      int deltaTime = (int) (timeEnd - timeStart);
      monitorLog = new MonitorLog.LogBuilder(logEvent).timeSpent(deltaTime).build();
    }

    metricsDataMap.remove(eventName);
    return monitorLog;
  }

  // TempMetrics stores the start status at the beginning of the measurement
  private static class TempMetrics {
    private long timeStart;

    TempMetrics(long timeStart) {
      this.timeStart = timeStart;
    }
  }
}
