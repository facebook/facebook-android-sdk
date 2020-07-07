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
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.internal.logging.ExternalLog;
import com.facebook.internal.logging.LoggingManager;
import com.facebook.internal.metrics.MetricsUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Monitor is the entry point of the Monitoring System. Once Monitor is enabled, we will fetch the
 * sampling rates from the server and flush the disk (send the logs to the server and clean the
 * disk).
 */
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
   * <p>need to call Monitor.stopMeasurePerfFor() for the same event at the point you want to stop
   * measuring
   *
   * @param eventName indicates the target function(s)
   */
  public static void startMeasurePerfFor(PerformanceEventName eventName) {
    if (isEnabled) {
      metricsUtil.startMeasureFor(eventName);
    }
  }

  /**
   * stop to measure the performance for the specific event, which is the target function(s)
   * calculate the metrics data and create a new log which will be sent to the server later
   *
   * <p>need to call Monitor.startMeasurePerfFor() for the same event at the point you want to start
   * measuring
   *
   * @param eventName indicates the target function(s)
   */
  public static void stopMeasurePerfFor(PerformanceEventName eventName) {
    if (isEnabled) {
      MonitorLog monitorLog = metricsUtil.stopMeasureFor(eventName);
      if (monitorLog.isValid()) {
        addLog(monitorLog);
      }
    }
  }

  /**
   * add the log to the monitoring system if the log is sampled
   *
   * @param log which will be sent back to the server
   */
  public static void addLog(ExternalLog log) {
    if (isEnabled && isSampled(log)) {
      monitorLoggingManager.addLog(log);
    }
  }

  public static boolean isEnabled() {
    return isEnabled;
  }

  /** @return true if this log should be added respecting its sampling rate */
  static boolean isSampled(ExternalLog log) {
    if (log == null) {
      return false;
    }
    String eventName = log.getEventName();
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
