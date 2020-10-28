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

import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.APPLICATION_FIELDS;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.DEFAULT_SAMPLE_RATES_KEY;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.MONITOR_CONFIG;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.SAMPLE_RATES;

import android.os.Bundle;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.logging.ExternalLog;
import com.facebook.internal.logging.LoggingManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Monitor is the entry point of the Monitoring System. Once Monitor is enabled, we will fetch the
 * sampling rates from the server and flush the disk (send the logs to the server and clean the
 * disk).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoHandleExceptions
public class Monitor {
  private static final Random random = new Random();

  // in case we can't fetch the sampling rate from the server
  private static Integer defaultSamplingRate = 1000;

  private static boolean isEnabled;
  private static final LoggingManager monitorLoggingManager =
      MonitorLoggingManager.getInstance(
          MonitorLoggingQueue.getInstance(), MonitorLoggingStore.getInstance());
  private static final MetricsUtil metricsUtil = MetricsUtil.getInstance();
  private static final Map<String, Integer> samplingRatesMap = new HashMap<>();
  private static final AtomicLong UNIQUE_EXTRA_ID = new AtomicLong(0);

  private Monitor() {}

  /**
   * Enable Monitor will fetch the sampling rates from the server and send the logs from
   * MonitorLoggingStore
   */
  protected static void enable() {
    if (isEnabled) {
      return;
    }
    isEnabled = true;

    // Pull down the sampling rates, and update samplingRatesMap
    loadSamplingRatesMapAsync();

    // flush disk
    monitorLoggingManager.flushLoggingStore();
  }

  /** fetch the sampling rates from the server and update SamplingRatesMap */
  static void loadSamplingRatesMapAsync() {
    FacebookSdk.getExecutor()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                JSONObject samplingRates = fetchSamplingRate();
                if (samplingRates != null) {
                  updateSamplingRateMap(samplingRates);
                }
              }
            });
  }

  /**
   * send the request of fetching sampling rates to the server
   *
   * @return JSONObject of sampling rates from server
   */
  static JSONObject fetchSamplingRate() {
    Bundle monitorConfigParams = new Bundle();
    monitorConfigParams.putString(APPLICATION_FIELDS, MONITOR_CONFIG);
    GraphRequest request =
        GraphRequest.newGraphPathRequest(null, FacebookSdk.getApplicationId(), null);
    request.setSkipClientToken(true);
    request.setParameters(monitorConfigParams);
    return request.executeAndWait().getJSONObject();
  }

  /**
   * update samplingRatesMap using fetched sampling rates
   *
   * @param fetchedSamplingRates
   */
  static void updateSamplingRateMap(JSONObject fetchedSamplingRates) {
    try {
      JSONArray samplingRates =
          fetchedSamplingRates.getJSONObject(MONITOR_CONFIG).getJSONArray(SAMPLE_RATES);
      for (int i = 0; i < samplingRates.length(); i++) {
        JSONObject keyValuePair = samplingRates.getJSONObject(i);
        String eventName = keyValuePair.getString("key");
        int samplingRate = keyValuePair.getInt("value");
        if (DEFAULT_SAMPLE_RATES_KEY.equals(eventName)) {
          defaultSamplingRate = samplingRate;
        } else {
          samplingRatesMap.put(eventName, samplingRate);
        }
      }
    } catch (JSONException e) {
      // swallow Exception to avoid user's app to crash
    }
  }

  /**
   * start to measure the performance for the specific event, which is the target function(s)
   *
   * <p>need to call stopMeasurePerfFor for the same event at the point you want to stop measuring
   *
   * @param eventName indicates the target function(s)
   * @param extraId is an extra id that will be used with the performance event name, each
   *     measurement tracks the same event name and extra id. Especially For the case that
   *     startMeasurePerfFor and stopMeasurePerfFor are called from the different threads
   */
  public static void startMeasurePerfFor(PerformanceEventName eventName, long extraId) {
    if (isEnabled && isSampled(eventName.toString())) {
      metricsUtil.startMeasureFor(eventName, extraId);
    }
  }

  /**
   * start to measure the performance for the specific event, which is the target function(s)
   *
   * <p>need to call stopMeasurePerfFor for the same event at the point you want to stop measuring
   *
   * <p>This method will use the current thread id as the default extra id. If startMeasurePerfFor
   * and stopMeasurePerfFor are called from the different threads, don't use this method. Please
   * pass performance event name and an extra id generated by generateExtraId for both
   * startMeasurePerfFor and stopMeasurePerfFor.
   *
   * @param eventName indicates the target function(s)
   */
  public static void startMeasurePerfFor(PerformanceEventName eventName) {
    long metricsCurrentThreadId = getCurrentThreadID();
    startMeasurePerfFor(eventName, metricsCurrentThreadId);
  }

  /**
   * stop to measure the performance for the specific event, which is the target function(s)
   * calculate the metrics data and create a new log which will be sent to the server later
   *
   * <p>need to call startMeasurePerfFor for the same event at the point you want to start measuring
   *
   * @param eventName indicates the target function(s)
   * @param extraId is an extra id that will be used with the performance event name, each
   *     measurement tracks the same event name and extra id. Especially For the case that
   *     startMeasurePerfFor and stopMeasurePerfFor are called from the different threads
   */
  public static void stopMeasurePerfFor(PerformanceEventName eventName, long extraId) {
    MonitorLog monitorLog = metricsUtil.stopMeasureFor(eventName, extraId);
    if (monitorLog.isValid()) {
      addLog(monitorLog);
    }
  }

  /**
   * stop to measure the performance for the specific event, which is the target function(s)
   * calculate the metrics data and create a new log which will be sent to the server later
   *
   * <p>need to call startMeasurePerfFor for the same event at the point you want to start measuring
   *
   * <p>This method will use the current thread id as the default extra id. If startMeasurePerfFor
   * and stopMeasurePerfFor are called from the different threads, don't use this method. Please
   * pass performance event name and an extra id generated by generateExtraId for both
   * startMeasurePerfFor and stopMeasurePerfFor.
   *
   * @param eventName indicates the target function(s)
   */
  public static void stopMeasurePerfFor(PerformanceEventName eventName) {
    long metricsCurrentThreadId = getCurrentThreadID();
    stopMeasurePerfFor(eventName, metricsCurrentThreadId);
  }

  /**
   * This method will remove the temporary metrics data for the specific performance event.
   * Considering the stopMeasurePerfFor may not be guarantee to be executed, you can remove the
   * temporary metrics data if you know where to handle the exception.
   *
   * <p>If startMeasurePerfFor and stopMeasurePerfFor has been passed the extra id generated by
   * generateExtraId, please pass the same extra id into the param.
   *
   * @param eventName the target function(s)
   */
  public static void cancelMeasurePerfFor(PerformanceEventName eventName) {
    long metricsCurrentThreadId = getCurrentThreadID();
    metricsUtil.removeTempMetricsDataFor(eventName, metricsCurrentThreadId);
  }

  /**
   * This method will remove the temporary metrics data for the specific performance event.
   * Considering the stopMeasurePerfFor may not be guarantee to be executed, you can remove the
   * temporary metrics data if you know where to handle the exception.
   *
   * @param eventName the target function(s)
   * @param extraId is the one you used in the call startMeasurePerfFor the target performance event
   */
  public static void cancelMeasurePerfFor(PerformanceEventName eventName, long extraId) {
    metricsUtil.removeTempMetricsDataFor(eventName, extraId);
  }

  /**
   * This method will generate an unique number each time when it has been called.
   *
   * @return extra id which is a positive long number, will be used with performance event name
   */
  public static long generateExtraId() {
    return UNIQUE_EXTRA_ID.incrementAndGet();
  }

  private static long getCurrentThreadID() {
    return Thread.currentThread().getId();
  }

  /**
   * add the log to the monitoring system if Monitor is enabled
   *
   * @param log which will be sent back to the server
   */
  @VisibleForTesting
  static void addLog(ExternalLog log) {
    if (isEnabled) {
      monitorLoggingManager.addLog(log);
    }
  }

  public static boolean isEnabled() {
    return isEnabled;
  }

  /**
   * @return true if the log with the specific event name should be generated and added respecting
   *     its sampling rate
   */
  static boolean isSampled(String eventName) {
    if (Utility.isNullOrEmpty(eventName)) {
      return false;
    }
    int samplingRate = defaultSamplingRate;
    if (samplingRatesMap.containsKey(eventName)) {
      samplingRate = samplingRatesMap.get(eventName);
    }
    return samplingRate > 0 && random.nextInt(samplingRate) == 0;
  }

  // for cleaner test
  static Integer getDefaultSamplingRate() {
    return defaultSamplingRate;
  }
}
