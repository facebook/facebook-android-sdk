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

import android.os.Build;
import androidx.annotation.Nullable;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestBatch;
import com.facebook.internal.Utility;
import com.facebook.internal.logging.ExternalLog;
import com.facebook.internal.logging.LoggingCache;
import com.facebook.internal.logging.LoggingManager;
import com.facebook.internal.logging.LoggingStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_DEVICE_MODEL;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_DEVICE_OS_VERSION;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_UNIQUE_APPLICATION_ID;

/**
 * MonitorLoggingManager deals with all new logs and the logs storing in the memory and the disk.
 * The new log will be added into MonitorLoggingQueue.
 * The MonitorLoggingManger will do the next step depending on if the MonitorLoggingQueue has
 * reached the flush limit after adding new log(s). If yes, MonitorLoggingManager will send the
 * logs back to our server. If not, MonitorLoggingManager will schedule a future task of sending
 * logs at regular intervals.
 *
 * Each GraphRequest can have limited number of logs in the parameter in maximum.
 * We send the GraphRequest(s) using GraphRequestBatch call.
 */
public class MonitorLoggingManager implements LoggingManager {
    private static final int FLUSH_PERIOD = 60;    // in second
    private static final Integer MAX_LOG_NUMBER_PER_REQUEST = 100;
    private static final String ENTRIES_KEY = "entries";
    private static final String MONITORING_ENDPOINT = "monitorings";
    private final ScheduledExecutorService singleThreadExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private static MonitorLoggingManager monitorLoggingManager;
    private LoggingCache logQueue;
    private LoggingStore logStore;
    private ScheduledFuture flushTimer;

    // device information
    private static String deviceOSVersion;
    private static String deviceModel;

    static {
        deviceOSVersion = Build.VERSION.RELEASE;
        deviceModel = Build.MODEL;
    }

    // Only call for the singleThreadExecutor
    private final Runnable flushRunnable = new Runnable() {
        @Override
        public void run() {
            flushAndWait();
        }
    };

    private MonitorLoggingManager(LoggingCache monitorLoggingQueue, LoggingStore monitorLoggingStore) {
        if (logQueue == null) {
            this.logQueue = monitorLoggingQueue;
        }
        if (logStore == null) {
            this.logStore = monitorLoggingStore;
        }
    }

    public synchronized static MonitorLoggingManager getInstance(LoggingCache monitorLoggingQueue, LoggingStore logStore) {
        if (monitorLoggingManager == null) {
            monitorLoggingManager = new MonitorLoggingManager(monitorLoggingQueue, logStore);
        }
        return monitorLoggingManager;
    }

    @Override
    public void addLog(final ExternalLog log) {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (logQueue.addLog(log)) {
                    flushAndWait();
                } else if (flushTimer == null) {
                    flushTimer = singleThreadExecutor.schedule(
                            flushRunnable,
                            FLUSH_PERIOD,
                            TimeUnit.SECONDS);
                }
            }
        });
    }

    @Override
    public void flushAndWait() {
        if (flushTimer != null) {
            flushTimer.cancel(true);
        }

        // build requests
        List<GraphRequest> requests = buildRequests(logQueue);
        try {
            new GraphRequestBatch(requests).executeAsync();
        } catch (Exception e) {
            // swallow Exception to avoid user's app to crash
        }
    }

    // will be called once the Monitor is enabled
    @Override
    public void flushLoggingStore() {
        Collection<ExternalLog> logsReadFromStore = logStore.readAndClearStore();
        logQueue.addLogs(logsReadFromStore);
        flushAndWait();
    }

    static List<GraphRequest> buildRequests(LoggingCache monitorLoggingQueue) {
        List<GraphRequest> requests = new ArrayList<>();
        String appID = FacebookSdk.getApplicationId();

        // Check App ID is not null
        if (Utility.isNullOrEmpty(appID)) {
            return requests;
        }

        while (!monitorLoggingQueue.isEmpty()) {
            final List<ExternalLog> logsReadyToBeSend = new ArrayList<>();

            // each GraphRequest contains MAX_LOG_NUMBER_PER_REQUEST of logs
            for (int i = 0; i < MAX_LOG_NUMBER_PER_REQUEST && !monitorLoggingQueue.isEmpty(); i++) {
                ExternalLog log = monitorLoggingQueue.fetchLog();
                logsReadyToBeSend.add(log);
            }

            GraphRequest postRequest = buildPostRequestFromLogs(logsReadyToBeSend);
            if (postRequest != null) {
                requests.add(postRequest);
            }
        }
        return requests;
    }

    @Nullable
    static GraphRequest buildPostRequestFromLogs(List<? extends ExternalLog> logs) {
        String packageName = FacebookSdk.getApplicationContext().getPackageName();
        JSONArray logsToParams = new JSONArray();

        for (ExternalLog log : logs) {
            logsToParams.put(log.convertToJSONObject());
        }

        if (logsToParams.length() == 0) {
            return null;
        }

        JSONObject params = new JSONObject();
        try {
            params.put(PARAM_DEVICE_OS_VERSION, deviceOSVersion);
            params.put(PARAM_DEVICE_MODEL, deviceModel);
            params.put(PARAM_UNIQUE_APPLICATION_ID, packageName);
            params.put(ENTRIES_KEY, logsToParams);
        } catch (JSONException e) {
            return null;
        }

        return GraphRequest.newPostRequest(
                null,
                String.format("%s/" + MONITORING_ENDPOINT, FacebookSdk.getApplicationId()),
                params,
                null);
    }
}
