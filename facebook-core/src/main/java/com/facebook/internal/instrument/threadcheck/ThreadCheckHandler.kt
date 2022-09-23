/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.threadcheck

import android.os.Looper
import android.util.Log
import androidx.annotation.RestrictTo
import com.facebook.internal.instrument.InstrumentData
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ThreadCheckHandler {
  private val TAG = ThreadCheckHandler::class.java.canonicalName
  private var enabled = false

  @JvmStatic
  fun enable() {
    enabled = true
  }

  @JvmStatic
  fun uiThreadViolationDetected(clazz: Class<*>, methodName: String, methodDesc: String) {
    log("@UiThread", clazz, methodName, methodDesc)
  }

  @JvmStatic
  fun workerThreadViolationDetected(clazz: Class<*>, methodName: String, methodDesc: String) {
    log("@WorkerThread", clazz, methodName, methodDesc)
  }

  private fun log(annotation: String, clazz: Class<*>, methodName: String, methodDesc: String) {
    if (!enabled) {
      return
    }
    val message =
        String.format(
            Locale.US,
            "%s annotation violation detected in %s.%s%s. Current looper is %s and main looper is %s.",
            annotation,
            clazz.name,
            methodName,
            methodDesc,
            Looper.myLooper(),
            Looper.getMainLooper())
    val e = Exception()
    Log.e(TAG, message, e)
    InstrumentData.Builder.build(e, InstrumentData.Type.ThreadCheck).save()
  }
}
