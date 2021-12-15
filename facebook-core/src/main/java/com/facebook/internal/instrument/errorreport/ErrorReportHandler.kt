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
