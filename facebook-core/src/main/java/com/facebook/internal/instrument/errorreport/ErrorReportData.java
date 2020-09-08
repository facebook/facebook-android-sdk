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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.internal.instrument.InstrumentUtility;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ErrorReportData {

  private static final String PRARAM_ERROR_MESSAGE = "error_message";
  private static final String PARAM_TIMESTAMP = "timestamp";

  private String filename;
  @Nullable private String errorMessage;
  @Nullable private Long timestamp;

  public ErrorReportData(String message) {
    timestamp = System.currentTimeMillis() / 1000;
    errorMessage = message;
    filename =
        new StringBuffer()
            .append(InstrumentUtility.ERROR_REPORT_PREFIX)
            .append(timestamp)
            .append(".json")
            .toString();
  }

  public ErrorReportData(File file) {
    filename = file.getName();
    final JSONObject object = InstrumentUtility.readFile(filename, true);
    if (object != null) {
      timestamp = object.optLong(PARAM_TIMESTAMP, 0);
      errorMessage = object.optString(PRARAM_ERROR_MESSAGE, null);
    }
  }

  public int compareTo(ErrorReportData data) {
    if (timestamp == null) {
      return -1;
    }
    if (data.timestamp == null) {
      return 1;
    }
    return data.timestamp.compareTo(timestamp);
  }

  public boolean isValid() {
    return errorMessage != null && timestamp != null;
  }

  public void save() {
    if (isValid()) {
      InstrumentUtility.writeFile(filename, this.toString());
    }
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
  public JSONObject getParameters() {
    JSONObject object = new JSONObject();
    try {
      if (timestamp != null) {
        object.put(PARAM_TIMESTAMP, timestamp);
      }
      object.put(PRARAM_ERROR_MESSAGE, errorMessage);
      return object;
    } catch (JSONException e) {
      /* no op */
    }
    return null;
  }
}
