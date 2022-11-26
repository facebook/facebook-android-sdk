/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.crashreport

import android.util.Log
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.internal.Utility.isDataProcessingRestricted
import com.facebook.internal.instrument.ExceptionAnalyzer
import com.facebook.internal.instrument.InstrumentData
import com.facebook.internal.instrument.InstrumentUtility
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CrashHandler
private constructor(private val previousHandler: Thread.UncaughtExceptionHandler?) :
    Thread.UncaughtExceptionHandler {
  override fun uncaughtException(t: Thread, e: Throwable) {
    if (InstrumentUtility.isSDKRelatedException(e)) {
      ExceptionAnalyzer.execute(e)
      InstrumentData.Builder.build(e, InstrumentData.Type.CrashReport).save()
    }
    previousHandler?.uncaughtException(t, e)
  }

  companion object {
    private val TAG = CrashHandler::class.java.canonicalName
    private const val MAX_CRASH_REPORT_NUM = 5
    private var instance: CrashHandler? = null

    @JvmStatic
    @Synchronized
    fun enable() {
      if (FacebookSdk.getAutoLogAppEventsEnabled()) {
        sendExceptionReports()
      }
      if (instance != null) {
        Log.w(TAG, "Already enabled!")
        return
      }
      val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
      instance = CrashHandler(oldHandler)
      Thread.setDefaultUncaughtExceptionHandler(instance)
    }

    /**
     * Load cached exception reports from cache directory defined in
     * [InstrumentUtility.getInstrumentReportDir], create Graph Request and send the request to
     * Facebook along with crash reports.
     */
    private fun sendExceptionReports() {
      if (isDataProcessingRestricted) {
        return
      }
      val reports = InstrumentUtility.listExceptionReportFiles()
      val validReports =
          reports
              .map { InstrumentData.Builder.load(it) }
              .filter { it.isValid }
              .sortedWith(Comparator { o1, o2 -> o1.compareTo(o2) })
      val crashLogs = JSONArray()

      // keep at most MAX_CRASH_REPORT_NUM reports
      (0 until (min(validReports.size, MAX_CRASH_REPORT_NUM))).forEach {
        crashLogs.put(validReports[it])
      }

      InstrumentUtility.sendReports("crash_reports", crashLogs) { response ->
        try {
          if (response.error == null && response.jsonObject?.getBoolean("success") == true) {
            validReports.forEach { it.clear() }
          }
        } catch (e: JSONException) {
          /* no op */
        }
      }
    }
  }
}
