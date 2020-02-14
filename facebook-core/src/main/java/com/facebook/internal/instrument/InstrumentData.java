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

import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InstrumentData {

    public enum Type {
        CrashReport,
        CrashShield,
        ThreadCheck;

        @Override
        public String toString() {
            String name = "Unknown";

            switch (this) {
                case CrashReport: name = "CrashReport"; break;
                case CrashShield: name = "CrashShield"; break;
                case ThreadCheck: name = "ThreadCheck"; break;
            }

            return name;
        }
    }

    private static final String PARAM_TIMESTAMP = "timestamp";
    private static final String PARAM_APP_VERSION = "app_version";
    private static final String PARAM_DEVICE_OS = "device_os_version";
    private static final String PARAM_DEVICE_MODEL = "device_model";
    private static final String PARAM_REASON = "reason";
    private static final String PARAM_CALLSTACK = "callstack";
    private static final String PARAM_TYPE = "type";

    private String filename;
    @Nullable private String appVersion;
    @Nullable private String cause;
    @Nullable private String stackTrace;
    @Nullable private Long timestamp;
    @Nullable private String type;

    public InstrumentData(Throwable e, Type t) {
        appVersion = Utility.getAppVersion();
        cause = InstrumentUtility.getCause(e);
        stackTrace = InstrumentUtility.getStackTrace(e);
        timestamp = System.currentTimeMillis() / 1000;
        type = t.toString();
        filename = new StringBuffer()
                .append(InstrumentUtility.CRASH_REPORT_PREFIX)
                .append(timestamp.toString())
                .append(".json")
                .toString();
    }

    public InstrumentData(File file) {
        filename = file.getName();
        final JSONObject object = InstrumentUtility.readFile(filename, true);
        if (object != null) {
            appVersion = object.optString(PARAM_APP_VERSION, null);
            cause = object.optString(PARAM_REASON, null);
            stackTrace = object.optString(PARAM_CALLSTACK, null);
            timestamp = object.optLong(PARAM_TIMESTAMP, 0);
            type = object.optString(PARAM_TYPE, null);
        }
    }

    public int compareTo(InstrumentData data) {
        if (timestamp == null) {
            return -1;
        }
        if (data.timestamp == null) {
            return 1;
        }
        return data.timestamp.compareTo(timestamp);
    }

    public boolean isValid() {
        return stackTrace != null && timestamp != null;
    }

    public void save() {
        if (!this.isValid()) {
            return;
        }
        InstrumentUtility.writeFile(filename, this.toString());
    }

    public void clear() {
        InstrumentUtility.deleteFile(filename);
    }

    @Nullable
    public String toString() {
        JSONObject params = getParameters();
        if (params == null) {
            return null;
        }
        return params.toString();
    }

    @Nullable
    public JSONObject getParameters() {
        JSONObject object = new JSONObject();
        try {
            object.put(PARAM_DEVICE_OS, Build.VERSION.RELEASE);
            object.put(PARAM_DEVICE_MODEL, Build.MODEL);
            if (appVersion != null) {
                object.put(PARAM_APP_VERSION, appVersion);
            }
            if (timestamp != null) {
                object.put(PARAM_TIMESTAMP, timestamp);
            }
            if (cause != null) {
                object.put(PARAM_REASON, cause);
            }
            if (stackTrace != null) {
                object.put(PARAM_CALLSTACK, stackTrace);
            }
            if (type != null) {
                object.put(PARAM_TYPE, type);
            }
            return object;
        } catch (JSONException e) { /* no op */ }
        return null;
    }
}
