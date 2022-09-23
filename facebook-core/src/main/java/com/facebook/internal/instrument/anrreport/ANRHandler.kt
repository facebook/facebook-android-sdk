/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
