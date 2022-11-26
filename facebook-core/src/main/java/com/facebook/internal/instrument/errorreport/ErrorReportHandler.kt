/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.errorreport

import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.internal.Utility.isDataProcessingRestricted
import com.facebook.internal.instrument.InstrumentUtility
import java.io.File
import org.json.JSONArray
import org.json.JSONException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ErrorReportHandler {
  private const val MAX_ERROR_REPORT_NUM = 1000

  @JvmStatic
  fun save(msg: String?) {
    try {
      val errorReport = ErrorReportData(msg)
      errorReport.save()
    } catch (ex: Exception) {
      /*no op*/
    }
  }

  @JvmStatic
  fun enable() {
    if (FacebookSdk.getAutoLogAppEventsEnabled()) {
      sendErrorReports()
    }
  }

  /**
   * Load cached error reports from cache directory defined in [ ]
   * [InstrumentUtility.getInstrumentReportDir], create Graph Request and send the request to
   * Facebook along with crash reports.
   */
  @JvmStatic
  fun sendErrorReports() {
    if (isDataProcessingRestricted) {
      return
    }
    val reports = listErrorReportFiles()
    val validReports = ArrayList<ErrorReportData>()
    for (report in reports) {
      val errorData = ErrorReportData(report)
      if (errorData.isValid) {
        validReports.add(errorData)
      }
    }
    validReports.sortWith(Comparator { o1, o2 -> o1.compareTo(o2) })
    val errorLogs = JSONArray()
    var i = 0
    while (i < validReports.size && i < MAX_ERROR_REPORT_NUM) {
      errorLogs.put(validReports[i])
      i++
    }
    InstrumentUtility.sendReports("error_reports", errorLogs) { response ->
      try {
        if (response.error == null && response.jsonObject?.getBoolean("success") == true) {
          validReports.forEach { it.clear() }
        }
      } catch (e: JSONException) {
        /* no op */
      }
    }
  }

  @JvmStatic
  fun listErrorReportFiles(): Array<File> {
    val reportDir = InstrumentUtility.getInstrumentReportDir() ?: return arrayOf()
    return reportDir.listFiles { dir, name ->
      name.matches(Regex(String.format("^%s[0-9]+.json$", InstrumentUtility.ERROR_REPORT_PREFIX)))
    }
  }
}
