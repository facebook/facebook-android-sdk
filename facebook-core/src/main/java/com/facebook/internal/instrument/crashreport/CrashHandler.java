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

package com.facebook.internal.instrument.crashreport;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.instrument.ExceptionAnalyzer;
import com.facebook.internal.instrument.InstrumentData;
import com.facebook.internal.instrument.InstrumentUtility;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = CrashHandler.class.getCanonicalName();
    private static final int MAX_CRASH_REPORT_NUM = 5;

    @Nullable private static CrashHandler instance;

    @Nullable private final Thread.UncaughtExceptionHandler mPreviousHandler;

    private CrashHandler(@Nullable  Thread.UncaughtExceptionHandler oldHandler) {
        mPreviousHandler = oldHandler;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (InstrumentUtility.isSDKRelatedException(e)) {
            ExceptionAnalyzer.execute(e);
            InstrumentData.Builder.build(e, InstrumentData.Type.CrashReport).save();
        }
        if (mPreviousHandler != null) {
            mPreviousHandler.uncaughtException(t, e);
        }
    }

    public static synchronized void enable() {
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            sendExceptionReports();
        }
        if (instance != null) {
            Log.w(TAG, "Already enabled!");
            return;
        }
        Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        instance = new CrashHandler(oldHandler);
        Thread.setDefaultUncaughtExceptionHandler(instance);
    }

    /**
     * Load cached exception reports from cache directory defined in
     * {@link InstrumentUtility#getInstrumentReportDir()}, create Graph Request and send the
     * request to Facebook along with crash reports.
     */
    private static void sendExceptionReports() {
        File[] reports = InstrumentUtility.listExceptionReportFiles();
        final ArrayList<InstrumentData> validReports = new ArrayList<>();
        for (File report : reports) {
            InstrumentData instrumentData = InstrumentData.Builder.load(report);
            if (instrumentData.isValid()) {
                validReports.add(instrumentData);
            }
        }
        Collections.sort(validReports, new Comparator<InstrumentData>() {
            @Override
            public int compare(InstrumentData o1, InstrumentData o2) {
                return o1.compareTo(o2);
            }
        });

        final JSONArray crashLogs = new JSONArray();
        for (int i = 0; i < validReports.size() && i < MAX_CRASH_REPORT_NUM; i++) {
            crashLogs.put(validReports.get(i));
        }

        InstrumentUtility.sendReports("crash_reports", crashLogs, new GraphRequest.Callback() {
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
}
