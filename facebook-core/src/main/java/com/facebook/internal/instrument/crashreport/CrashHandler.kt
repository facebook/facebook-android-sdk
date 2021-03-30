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
          if (response.error == null && response.jsonObject.getBoolean("success")) {
            validReports.forEach { it.clear() }
          }
        } catch (e: JSONException) {
          /* no op */
        }
      }
    }
  }
}
