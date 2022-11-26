/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument

import android.os.Build
import androidx.annotation.RestrictTo
import com.facebook.internal.Utility.getAppVersion
import com.facebook.internal.instrument.InstrumentUtility.deleteFile
import com.facebook.internal.instrument.InstrumentUtility.getCause
import com.facebook.internal.instrument.InstrumentUtility.getStackTrace
import com.facebook.internal.instrument.InstrumentUtility.readFile
import com.facebook.internal.instrument.InstrumentUtility.writeFile
import java.io.File
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InstrumentData {
  enum class Type {
    Unknown,
    Analysis,
    AnrReport,
    CrashReport,
    CrashShield,
    ThreadCheck;

    override fun toString(): String {
      return when (this) {
        Analysis -> "Analysis"
        AnrReport -> "AnrReport"
        CrashReport -> "CrashReport"
        CrashShield -> "CrashShield"
        ThreadCheck -> "ThreadCheck"
        else -> UNKNOWN
      }
    }

    val logPrefix: String
      get() {
        return when (this) {
          Analysis -> InstrumentUtility.ANALYSIS_REPORT_PREFIX
          AnrReport -> InstrumentUtility.ANR_REPORT_PREFIX
          CrashReport -> InstrumentUtility.CRASH_REPORT_PREFIX
          CrashShield -> InstrumentUtility.CRASH_SHIELD_PREFIX
          ThreadCheck -> InstrumentUtility.THREAD_CHECK_PREFIX
          else -> UNKNOWN
        }
      }
  }

  private var filename: String
  private var type: Type?
  private var featureNames: JSONArray? = null
  private var appVersion: String? = null
  private var cause: String? = null
  private var stackTrace: String? = null
  private var timestamp: Long? = null

  private constructor(features: JSONArray) {
    type = Type.Analysis
    timestamp = System.currentTimeMillis() / 1000
    featureNames = features
    filename =
        StringBuffer()
            .append(InstrumentUtility.ANALYSIS_REPORT_PREFIX)
            .append(timestamp.toString())
            .append(".json")
            .toString()
  }

  private constructor(e: Throwable?, t: Type) {
    type = t
    appVersion = getAppVersion()
    cause = getCause(e)
    stackTrace = getStackTrace(e)
    timestamp = System.currentTimeMillis() / 1000
    filename =
        StringBuffer().append(t.logPrefix).append(timestamp.toString()).append(".json").toString()
  }

  private constructor(anrCause: String?, st: String?) {
    type = Type.AnrReport
    appVersion = getAppVersion()
    cause = anrCause
    stackTrace = st
    timestamp = System.currentTimeMillis() / 1000
    filename =
        StringBuffer()
            .append(InstrumentUtility.ANR_REPORT_PREFIX)
            .append(timestamp.toString())
            .append(".json")
            .toString()
  }

  private constructor(file: File) {
    filename = file.name
    type = getType(filename)
    val obj = readFile(filename, true)
    if (obj != null) {
      timestamp = obj.optLong(PARAM_TIMESTAMP, 0)
      appVersion = obj.optString(PARAM_APP_VERSION, null)
      cause = obj.optString(PARAM_REASON, null)
      stackTrace = obj.optString(PARAM_CALLSTACK, null)
      featureNames = obj.optJSONArray(PARAM_FEATURE_NAMES)
    }
  }

  operator fun compareTo(data: InstrumentData): Int {
    val ts = timestamp ?: return -1
    val dts = data.timestamp ?: return 1
    return dts.compareTo(ts)
  }

  val isValid: Boolean
    get() {
      return when (type) {
        Type.Analysis -> featureNames != null && timestamp != null
        Type.AnrReport -> stackTrace != null && cause != null && timestamp != null
        Type.CrashReport,
        Type.CrashShield,
        Type.ThreadCheck -> stackTrace != null && timestamp != null
        else -> false
      }
    }

  fun save() {
    if (!isValid) {
      return
    }
    writeFile(filename, this.toString())
  }

  fun clear() {
    deleteFile(filename)
  }

  override fun toString(): String {
    val params = parameters ?: return JSONObject().toString()
    return params.toString()
  }

  private val parameters: JSONObject?
    private get() {
      return when (type) {
        Type.Analysis -> analysisReportParameters
        Type.AnrReport,
        Type.CrashReport,
        Type.CrashShield,
        Type.ThreadCheck -> exceptionReportParameters
        else -> null
      }
    }

  private val analysisReportParameters: JSONObject?
    private get() {
      val obj = JSONObject()
      try {
        if (featureNames != null) {
          obj.put(PARAM_FEATURE_NAMES, featureNames)
        }
        if (timestamp != null) {
          obj.put(PARAM_TIMESTAMP, timestamp)
        }
        return obj
      } catch (e: JSONException) {
        /* no op */
      }
      return null
    }

  private val exceptionReportParameters: JSONObject?
    private get() {
      val obj = JSONObject()
      try {
        obj.put(PARAM_DEVICE_OS, Build.VERSION.RELEASE)
        obj.put(PARAM_DEVICE_MODEL, Build.MODEL)
        if (appVersion != null) {
          obj.put(PARAM_APP_VERSION, appVersion)
        }
        if (timestamp != null) {
          obj.put(PARAM_TIMESTAMP, timestamp)
        }
        if (cause != null) {
          obj.put(PARAM_REASON, cause)
        }
        if (stackTrace != null) {
          obj.put(PARAM_CALLSTACK, stackTrace)
        }
        if (type != null) {
          obj.put(PARAM_TYPE, type)
        }
        return obj
      } catch (e: JSONException) {
        /* no op */
      }
      return null
    }

  object Builder {
    @JvmStatic
    fun load(file: File): InstrumentData {
      return InstrumentData(file)
    }

    @JvmStatic
    fun build(e: Throwable?, t: Type): InstrumentData {
      return InstrumentData(e, t)
    }

    @JvmStatic
    fun build(features: JSONArray): InstrumentData {
      return InstrumentData(features)
    }

    @JvmStatic
    fun build(anrCause: String?, st: String?): InstrumentData {
      return InstrumentData(anrCause, st)
    }
  }

  companion object {
    private const val UNKNOWN = "Unknown"
    private const val PARAM_TIMESTAMP = "timestamp"
    private const val PARAM_APP_VERSION = "app_version"
    private const val PARAM_DEVICE_OS = "device_os_version"
    private const val PARAM_DEVICE_MODEL = "device_model"
    private const val PARAM_REASON = "reason"
    private const val PARAM_CALLSTACK = "callstack"
    private const val PARAM_TYPE = "type"
    private const val PARAM_FEATURE_NAMES = "feature_names"
    private fun getType(filename: String): Type {
      if (filename.startsWith(InstrumentUtility.CRASH_REPORT_PREFIX)) {
        return Type.CrashReport
      } else if (filename.startsWith(InstrumentUtility.CRASH_SHIELD_PREFIX)) {
        return Type.CrashShield
      } else if (filename.startsWith(InstrumentUtility.THREAD_CHECK_PREFIX)) {
        return Type.ThreadCheck
      } else if (filename.startsWith(InstrumentUtility.ANALYSIS_REPORT_PREFIX)) {
        return Type.Analysis
      } else if (filename.startsWith(InstrumentUtility.ANR_REPORT_PREFIX)) {
        return Type.AnrReport
      }
      return Type.Unknown
    }
  }
}
