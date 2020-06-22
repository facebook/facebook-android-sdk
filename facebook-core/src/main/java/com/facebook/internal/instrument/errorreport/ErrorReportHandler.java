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
package com.facebook.internal.instrument.errorreport;

import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.InstrumentUtility;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONException;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ErrorReportHandler {
  private static final int MAX_ERROR_REPORT_NUM = 1000;

  public static void save(String msg) {
    try {
      ErrorReportData errorReport = new ErrorReportData(msg);
      errorReport.save();
    } catch (Exception ex) {
      /*no op*/
    }
  }

  public static void enable() {
    if (FacebookSdk.getAutoLogAppEventsEnabled()) {
      sendErrorReports();
    }
  }

  /**
   * Load cached error reports from cache directory defined in {@link
   * InstrumentUtility#getInstrumentReportDir()}, create Graph Request and send the request to
   * Facebook along with crash reports.
   */
  public static void sendErrorReports() {
    if (Utility.isDataProcessingRestricted()) {
      return;
    }
    File[] reports = listErrorReportFiles();
    final ArrayList<ErrorReportData> validReports = new ArrayList<>();
    for (File report : reports) {
      ErrorReportData errorData = new ErrorReportData(report);
      if (errorData.isValid()) {
        validReports.add(errorData);
      }
    }
    Collections.sort(
        validReports,
        new Comparator<ErrorReportData>() {
          @Override
          public int compare(ErrorReportData o1, ErrorReportData o2) {
            return o1.compareTo(o2);
          }
        });

    final JSONArray errorLogs = new JSONArray();
    for (int i = 0; i < validReports.size() && i < MAX_ERROR_REPORT_NUM; i++) {
      errorLogs.put(validReports.get(i));
    }

    InstrumentUtility.sendReports(
        "error_reports",
        errorLogs,
        new GraphRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {
            try {
              if (response.getError() == null && response.getJSONObject().getBoolean("success")) {
                for (int i = 0; validReports.size() > i; i++) {
                  validReports.get(i).clear();
                }
              }
            } catch (JSONException e) {
              /* no op */
            }
          }
        });
  }

  public static File[] listErrorReportFiles() {
    final File reportDir = InstrumentUtility.getInstrumentReportDir();
    if (reportDir == null) {
      return new File[] {};
    }

    return reportDir.listFiles(
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.matches(
                String.format("^%s[0-9]+.json$", InstrumentUtility.ERROR_REPORT_PREFIX));
          }
        });
  }
}
