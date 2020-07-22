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

import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.logging.LogCategory;
import com.facebook.internal.logging.LogEvent;
import java.util.HashMap;
import java.util.Map;

/*
 * startMeasureFor and stopMeasureFor should always be called in pairs.
 * Possible issue is that the scheduler might switch threads in between the calls, i.e
 * 1) call to startMeasureFor is made
 * 2) expectation is that the code you want to measure is ran here
 * 3) thread switches and unrelated code runs
 * 4) thread switches back to your code
 * 5) stopMeasureFor is called, now it has logged more time than actually was spent in your context
 *
 * We track the performance event by the performance event name and the extra id.
 *
 * If startMeasureFor and stopMeasureFor are called from the same thread, use current thread id as
 * the extra id. It will help to distinguish each measurement for the concurrency performance event.
 *
 * If startMeasureFor and stopMeasureFor are called from different threads, use Monitor.generateExtraId to
 * get an unique long as extra id.
 * An example of startMeasureFor and stopMeasureFor being called from the different threads for the
 * measurement:
 * 1) call to startMeasureFor is made in current thread
 * 2) call to stopMeasureFor is passed via a callback function which will be executed in another thread
 *
 * Measure as little code as possible and make sure you are measuring the right thing in the right
 * places.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoHandleExceptions
public class MetricsUtil {
  private static MetricsUtil metricsUtil;
  private static final String CLASS_TAG = MetricsUtil.class.getCanonicalName();
  private final Map<MetricsKey, TempMetrics> metricsDataMap = new HashMap<>();
  protected static final int INVALID_TIME = -1;

  private MetricsUtil() {}

  public static synchronized MetricsUtil getInstance() {
    if (metricsUtil == null) {
      metricsUtil = new MetricsUtil();
    }
    return metricsUtil;
  }

  /**
   * This method will start the measurement for the target function(s) which is called a performance
   * event as well.
   *
   * <p>The method will override previous metrics data for the same performance event name with the
   * same extra id.
   *
   * @param eventName the target function(s)
   * @param extraId is an extra id that will be used with the performance event name, each
   *     measurement tracks the same event name and extra id.
   */
  void startMeasureFor(PerformanceEventName eventName, long extraId) {
    MetricsKey keyOfMetricsData = new MetricsKey(eventName, extraId);
    long timeStart = SystemClock.elapsedRealtime();
    TempMetrics tempMetrics = new TempMetrics(timeStart);
    metricsDataMap.put(keyOfMetricsData, tempMetrics);
  }

  /**
   * This method will stop the measurement for the target function(s) which is called a performance
   * event as well.
   *
   * <p>This method will return MonitorLog with default invalid value if startMeasureFor has not
   * been called before.
   *
   * @param eventName the target function(s)
   * @param extraId is an extra id that will be used with the performance event name, each
   *     measurement tracks the same event name and extra id.
   */
  MonitorLog stopMeasureFor(PerformanceEventName eventName, long extraId) {
    long timeEnd = SystemClock.elapsedRealtime();
    MetricsKey keyOfMetricsData = new MetricsKey(eventName, extraId);
    LogEvent logEvent = new LogEvent(eventName.toString(), LogCategory.PERFORMANCE);
    MonitorLog monitorLog = new MonitorLog.LogBuilder(logEvent).timeSpent(INVALID_TIME).build();
    if (!metricsDataMap.containsKey(keyOfMetricsData)) {
      StringBuilder warningMessage = new StringBuilder();
      warningMessage.append("Can't measure for ");
      warningMessage.append(eventName);
      warningMessage.append(", startMeasureFor hasn't been called before.");

      Utility.logd(CLASS_TAG, warningMessage.toString());
      return monitorLog;
    }

    TempMetrics tempMetrics = metricsDataMap.get(keyOfMetricsData);
    if (tempMetrics != null) {
      long timeStart = tempMetrics.timeStart;
      int deltaTime = (int) (timeEnd - timeStart);
      monitorLog = new MonitorLog.LogBuilder(logEvent).timeSpent(deltaTime).build();
    }

    metricsDataMap.remove(keyOfMetricsData);
    return monitorLog;
  }

  /**
   * This method will remove the temporary metrics data for the specific performance event in
   * MetricsUtil. Considering the stopMeasureFor may not be guarantee to be executed, you can remove
   * the temporary metrics data if you know where to handle the exception.
   *
   * @param eventName the target function(s)
   * @param extraId is an extra id that will be used with the performance event name, each
   *     measurement tracks the same event name and extra id.
   */
  void removeTempMetricsDataFor(PerformanceEventName eventName, long extraId) {
    MetricsKey keyOfMetricsData = new MetricsKey(eventName, extraId);
    metricsDataMap.remove(keyOfMetricsData);
  }

  private static class MetricsKey {
    private PerformanceEventName performanceEventName;
    private long extraId;

    MetricsKey(PerformanceEventName performanceEventName, long extraId) {
      this.performanceEventName = performanceEventName;
      this.extraId = extraId;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MetricsKey that = (MetricsKey) o;
      return extraId == that.extraId && performanceEventName == that.performanceEventName;
    }

    @Override
    public int hashCode() {
      int hashCode = 17;
      hashCode = 31 * hashCode + performanceEventName.hashCode();
      hashCode = 31 * hashCode + (int) (extraId ^ (extraId >>> 32));
      return hashCode;
    }
  }

  // TempMetrics stores the start status at the beginning of the measurement
  private static class TempMetrics {
    private long timeStart;

    TempMetrics(long timeStart) {
      this.timeStart = timeStart;
    }
  }
}
