/*
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
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.io.File;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InstrumentData {

  public enum Type {
    Unknown,
    Analysis,
    CrashReport,
    CrashShield,
    ThreadCheck;

    @Override
    public String toString() {
      switch (this) {
        case Analysis:
          return "Analysis";
        case CrashReport:
          return "CrashReport";
        case CrashShield:
          return "CrashShield";
        case ThreadCheck:
          return "ThreadCheck";
      }
      return UNKNOWN;
    }

    public String getLogPrefix() {
      switch (this) {
        case Analysis:
          return InstrumentUtility.ANALYSIS_REPORT_PREFIX;
        case CrashReport:
          return InstrumentUtility.CRASH_REPORT_PREFIX;
        case CrashShield:
          return InstrumentUtility.CRASH_SHIELD_PREFIX;
        case ThreadCheck:
          return InstrumentUtility.THREAD_CHECK_PREFIX;
      }
      return UNKNOWN;
    }
  }

  private static final String UNKNOWN = "Unknown";
  private static final String PARAM_TIMESTAMP = "timestamp";
  private static final String PARAM_APP_VERSION = "app_version";
  private static final String PARAM_DEVICE_OS = "device_os_version";
  private static final String PARAM_DEVICE_MODEL = "device_model";
  private static final String PARAM_REASON = "reason";
  private static final String PARAM_CALLSTACK = "callstack";
  private static final String PARAM_TYPE = "type";
  private static final String PARAM_FEATURE_NAMES = "feature_names";

  private String filename;
  private Type type;
  @Nullable private JSONArray featureNames;
  @Nullable private String appVersion;
  @Nullable private String cause;
  @Nullable private String stackTrace;
  @Nullable private Long timestamp;

  private InstrumentData(JSONArray features) {
    type = Type.Analysis;
    timestamp = System.currentTimeMillis() / 1000;
    featureNames = features;
    filename =
        new StringBuffer()
            .append(InstrumentUtility.ANALYSIS_REPORT_PREFIX)
            .append(timestamp.toString())
            .append(".json")
            .toString();
  }

  private InstrumentData(Throwable e, Type t) {
    type = t;
    appVersion = Utility.getAppVersion();
    cause = InstrumentUtility.getCause(e);
    stackTrace = InstrumentUtility.getStackTrace(e);
    timestamp = System.currentTimeMillis() / 1000;
    filename =
        new StringBuffer()
            .append(t.getLogPrefix())
            .append(timestamp.toString())
            .append(".json")
            .toString();
  }

  private InstrumentData(File file) {
    filename = file.getName();
    type = getType(filename);
    final JSONObject object = InstrumentUtility.readFile(filename, true);
    if (object != null) {
      timestamp = object.optLong(PARAM_TIMESTAMP, 0);
      appVersion = object.optString(PARAM_APP_VERSION, null);
      cause = object.optString(PARAM_REASON, null);
      stackTrace = object.optString(PARAM_CALLSTACK, null);
      featureNames = object.optJSONArray(PARAM_FEATURE_NAMES);
    }
  }

  private static Type getType(String filename) {
    if (filename.startsWith(InstrumentUtility.CRASH_REPORT_PREFIX)) {
      return Type.CrashReport;
    } else if (filename.startsWith(InstrumentUtility.CRASH_SHIELD_PREFIX)) {
      return Type.CrashShield;
    } else if (filename.startsWith(InstrumentUtility.THREAD_CHECK_PREFIX)) {
      return Type.ThreadCheck;
    } else if (filename.startsWith(InstrumentUtility.ANALYSIS_REPORT_PREFIX)) {
      return Type.Analysis;
    }
    return Type.Unknown;
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
    switch (type) {
      case Analysis:
        return featureNames != null && timestamp != null;
      case CrashReport:
      case CrashShield:
      case ThreadCheck:
        return stackTrace != null && timestamp != null;
    }
    return false;
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
  private JSONObject getParameters() {
    switch (type) {
      case Analysis:
        return getAnalysisReportParameters();
      case CrashReport:
      case CrashShield:
      case ThreadCheck:
        return getExceptionReportParameters();
    }
    return null;
  }

  @Nullable
  private JSONObject getAnalysisReportParameters() {
    JSONObject object = new JSONObject();
    try {
      if (featureNames != null) {
        object.put(PARAM_FEATURE_NAMES, featureNames);
      }
      if (timestamp != null) {
        object.put(PARAM_TIMESTAMP, timestamp);
      }
      return object;
    } catch (JSONException e) {
      /* no op */
    }
    return null;
  }

  @Nullable
  private JSONObject getExceptionReportParameters() {
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
    } catch (JSONException e) {
      /* no op */
    }
    return null;
  }

  public static class Builder {
    public static InstrumentData load(File file) {
      return new InstrumentData(file);
    }

    public static InstrumentData build(Throwable e, Type t) {
      return new InstrumentData(e, t);
    }

    public static InstrumentData build(JSONArray features) {
      return new InstrumentData(features);
    }
  }
}
