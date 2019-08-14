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

package com.facebook.internal.instrument;

import android.support.annotation.RestrictTo;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.instrument.crashreport.CrashHandler;
import com.facebook.internal.instrument.crashreport.CrashReportData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InstrumentManager {

    private static final int MAX_CRASH_REPORT_NUM = 5;

    /**
     * Start Instrument functionality.
     *
     * Note that the function should be called after FacebookSdk is initialized. Otherwise,
     * exception FacebookSdkNotInitializedException will be thrown when loading and sending crash
     * reports.
     */
    public static void start() {
        if (FeatureManager.isEnabled(FeatureManager.Feature.CrashReport)) {
            CrashHandler.enable();
            if (FacebookSdk.getAutoLogAppEventsEnabled()) {
                sendCrashReports();
            }
        }
    }

    /**
     * Load cached crash reports from cache directory defined in
     * {@link InstrumentUtility#getInstrumentReportDir()}, create Graph Request and send the
     * request to Facebook along with crash reports.
     */
    private static void sendCrashReports() {
        File[] reports = InstrumentUtility.listCrashReportFiles();
        final ArrayList<CrashReportData> validReports = new ArrayList<>();
        for (File report : reports) {
            CrashReportData crashData = new CrashReportData(report);
            if (crashData.isValid()) {
                validReports.add(crashData);
            }
        }
        Collections.sort(validReports, new Comparator<CrashReportData>() {
            @Override
            public int compare(CrashReportData o1, CrashReportData o2) {
                return o1.compareTo(o2);
            }
        });

        final JSONArray crashLogs = new JSONArray();
        for (int i = 0; i < validReports.size() && i < MAX_CRASH_REPORT_NUM; i++) {
            crashLogs.put(validReports.get(i));
        }

        sendReports("crash_reports", crashLogs, new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                try {
                    if (response.getError() == null
                            && response.getJSONObject().getBoolean("success")) {
                        for (int i = 0; validReports.size() > i; i++) {
                           validReports.get(i).clear();
                        }
                    }
                } catch (JSONException e) {
                    /* no op */
                }
            }
        });
    }

    /**
     * Create Graph Request for Instrument reports and send the reports to Facebook.
     */
    private static void sendReports(String key, JSONArray reports, GraphRequest.Callback callback) {
        if (reports.length() == 0) {
            return;
        }

        final JSONObject params = new JSONObject();
        try {
            params.put(key, reports.toString());
        } catch (JSONException e) {
            return;
        }

        final GraphRequest request = GraphRequest.newPostRequest(null, String.format("%s" +
                "/instruments", FacebookSdk.getApplicationId()), params, callback);
        request.executeAsync();
    }
}
