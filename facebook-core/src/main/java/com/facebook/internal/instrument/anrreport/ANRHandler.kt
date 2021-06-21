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
package com.facebook.internal.instrument.anrreport

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.internal.Utility.isDataProcessingRestricted
import com.facebook.internal.instrument.InstrumentData
import com.facebook.internal.instrument.InstrumentUtility
import com.facebook.internal.instrument.InstrumentUtility.listAnrReportFiles
import com.facebook.internal.instrument.InstrumentUtility.sendReports
import com.facebook.internal.instrument.anrreport.ANRDetector.start
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONException

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ANRHandler {
  private const val MAX_ANR_REPORT_NUM = 5
  private val enabled = AtomicBoolean(false)

  @JvmStatic
  @Synchronized
  fun enable() {
    if (enabled.getAndSet(true)) {
      return
    }
    if (FacebookSdk.getAutoLogAppEventsEnabled()) {
      sendANRReports()
    }
    start()
  }

  /**
   * Load cached ANR reports from cache directory defined in
   * [InstrumentUtility.getInstrumentReportDir], create Graph Request and send the request to
   * Facebook along with crash reports.
   */
  @JvmStatic
  @VisibleForTesting
  fun sendANRReports() {
    if (isDataProcessingRestricted) {
      return
    }
    val reports = listAnrReportFiles()
    val validReports =
        reports
            .map { InstrumentData.Builder.load(it) }
            .filter { it.isValid }
            .sortedWith(Comparator { o1, o2 -> o1.compareTo(o2) })
    val anrLogs = JSONArray()
    // keep at most MAX_CRASH_REPORT_NUM reports
    (0 until min(validReports.size, ANRHandler.MAX_ANR_REPORT_NUM)).forEach {
      anrLogs.put(validReports[it])
    }
    sendReports("anr_reports", anrLogs) { response ->
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
