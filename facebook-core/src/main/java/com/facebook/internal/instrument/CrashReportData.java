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
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

final class CrashReportData {

    private String cause;
    private String stackTrace;
    private Long timestamp;

    public CrashReportData(Throwable e) {
        cause = InstrumentUtility.getCause(e);
        stackTrace = InstrumentUtility.getStackTrace(e);
        timestamp = System.currentTimeMillis() / 1000;
    }

    public void save() {
        // TODO: T47674704
    }

    public void clear() {
        // TODO: T47674704
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
            object.put("device_os_version", Build.VERSION.RELEASE);
            object.put("device_model", Build.MODEL);
            if (timestamp != null) {
                object.put("timestamp", timestamp);
            }
            if (cause != null) {
                object.put("reason", cause);
            }
            if (stackTrace != null) {
                object.put("callstack", stackTrace);
            }
            return object;
        } catch (JSONException e) { /* no op */ }
        return null;
    }
}
