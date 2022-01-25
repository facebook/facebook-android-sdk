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
import com.facebook.internal.instrument.InstrumentUtility
import java.io.File
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ErrorReportData {
  private var filename: String
  private var errorMessage: String? = null
  private var timestamp: Long? = null

  constructor(message: String?) {
    timestamp = System.currentTimeMillis() / 1000
    errorMessage = message
    filename =
        StringBuffer()
            .append(InstrumentUtility.ERROR_REPORT_PREFIX)
            .append(timestamp as Long)
            .append(".json")
            .toString()
  }

  constructor(file: File) {
    filename = file.name
    val obj = InstrumentUtility.readFile(filename, true)
    if (obj != null) {
      timestamp = obj.optLong(PARAM_TIMESTAMP, 0)
      errorMessage = obj.optString(PRARAM_ERROR_MESSAGE, null)
    }
  }

  operator fun compareTo(data: ErrorReportData): Int {
    val ts = timestamp ?: return -1
    val dts = data.timestamp ?: return 1
    return dts.compareTo(ts)
  }

  val isValid: Boolean
    get() = errorMessage != null && timestamp != null

  fun save() {
    if (isValid) {
      InstrumentUtility.writeFile(filename, this.toString())
    }
  }

  fun clear() {
    InstrumentUtility.deleteFile(filename)
  }

  override fun toString(): String {
    val params = parameters ?: return super.toString()
    return params.toString()
  }

  val parameters: JSONObject?
    get() {
      val obj = JSONObject()
      try {
        if (timestamp != null) {
          obj.put(PARAM_TIMESTAMP, timestamp)
        }
        obj.put(PRARAM_ERROR_MESSAGE, errorMessage)
        return obj
      } catch (e: JSONException) {
        /* no op */
      }
      return null
    }

  companion object {
    private const val PRARAM_ERROR_MESSAGE = "error_message"
    private const val PARAM_TIMESTAMP = "timestamp"
  }
}
