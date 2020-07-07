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

import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_CATEGORY;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_EVENT_NAME;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_TIME_SPENT;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_TIME_START;

import androidx.annotation.Nullable;
import com.facebook.internal.logging.ExternalLog;
import com.facebook.internal.logging.LogCategory;
import com.facebook.internal.logging.LogEvent;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * MonitorLog will be sent to the server via our Monitor. MonitorLog must have a Log Event including
 * a logCategory and an event name which indicates the specific tracked feature/function.
 */
public class MonitorLog implements ExternalLog {

  private static final long serialVersionUID = 1L;
  private LogEvent logEvent;
  private long timeStart;
  private int timeSpent;

  // Lazily initialized hashcode.
  private int hashCode;

  private static final int INVALID_TIME = -1;
  private static Set<String> validPerformanceEventNames;

  static {
    validPerformanceEventNames = new HashSet<>();
    for (PerformanceEventName eventName : PerformanceEventName.values()) {
      validPerformanceEventNames.add(eventName.toString());
    }
  }

  public MonitorLog(LogBuilder logBuilder) {
    this.logEvent = logBuilder.logEvent;
    this.timeStart = logBuilder.timeStart;
    this.timeSpent = logBuilder.timeSpent;
  }

  @Override
  public String getEventName() {
    return this.logEvent.getEventName();
  }

  @Override
  public LogCategory getLogCategory() {
    return this.logEvent.getLogCategory();
  }

  public long getTimeStart() {
    return this.timeStart;
  }

  public int getTimeSpent() {
    return this.timeSpent;
  }

  public static class LogBuilder {
    private LogEvent logEvent;
    private long timeStart;
    private int timeSpent;

    public LogBuilder(LogEvent logEvent) {
      this.logEvent = logEvent;
      if (logEvent.getLogCategory() == LogCategory.PERFORMANCE) {
        logEvent.upperCaseEventName();
      }
    }

    public LogBuilder timeStart(long timeStart) {
      this.timeStart = timeStart;
      return this;
    }

    public LogBuilder timeSpent(int timeSpent) {
      this.timeSpent = timeSpent;
      return this;
    }

    public MonitorLog build() {
      MonitorLog monitorLog = new MonitorLog(this);
      validateMonitorLog(monitorLog);
      return monitorLog;
    }

    private void validateMonitorLog(MonitorLog monitorLog) {
      // if the time is a negative value, we will set it to -1 to indicate it's an invalid value
      if (timeSpent < 0) {
        monitorLog.timeSpent = INVALID_TIME;
      }

      if (timeStart < 0) {
        monitorLog.timeStart = INVALID_TIME;
      }

      // if the category is PERFORMANCE, the event name should be
      // one of the value in validPerformanceEventNames set
      if (logEvent.getLogCategory() == LogCategory.PERFORMANCE
          && !validPerformanceEventNames.contains(logEvent.getEventName())) {
        throw new IllegalArgumentException(
            "Invalid event name: "
                + logEvent.getEventName()
                + "\n"
                + "It should be one of "
                + validPerformanceEventNames
                + ".");
      }
    }
  }

  public boolean isValid() {
    return timeStart >= 0 && timeSpent >= 0;
  }

  @Override
  public String toString() {
    String format = ": %s";
    return String.format(
        PARAM_EVENT_NAME
            + format
            + ", "
            + PARAM_CATEGORY
            + format
            + ", "
            + PARAM_TIME_START
            + format
            + ", "
            + PARAM_TIME_SPENT
            + format,
        logEvent.getEventName(),
        logEvent.getLogCategory(),
        timeStart,
        timeSpent);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 31 * result + logEvent.hashCode();
      result = 31 * result + (int) (timeStart ^ (timeStart >>> 32));
      result = 31 * result + (timeSpent ^ (timeSpent >>> 32));
      hashCode = result;
    }
    return hashCode;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MonitorLog other = (MonitorLog) obj;

    return logEvent.getEventName().equals(other.logEvent.getEventName())
        && logEvent.getLogCategory().equals(other.logEvent.getLogCategory())
        && timeStart == other.timeStart
        && timeSpent == other.timeSpent;
  }

  @Override
  public JSONObject convertToJSONObject() {
    JSONObject object = new JSONObject();
    try {
      object.put(PARAM_EVENT_NAME, logEvent.getEventName());
      object.put(PARAM_CATEGORY, logEvent.getLogCategory());

      if (timeStart != 0) {
        object.put(PARAM_TIME_START, timeStart);
      }
      if (timeSpent != 0) {
        object.put(PARAM_TIME_SPENT, timeSpent);
      }

      return object;
    } catch (JSONException e) {
      /* no op */
    }
    return null;
  }
}
