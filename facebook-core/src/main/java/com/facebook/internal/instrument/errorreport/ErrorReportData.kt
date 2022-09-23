/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
