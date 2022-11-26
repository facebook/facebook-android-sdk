/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.crashshield

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.core.BuildConfig
import com.facebook.internal.instrument.ExceptionAnalyzer
import com.facebook.internal.instrument.InstrumentData
import java.util.Collections
import java.util.WeakHashMap

object CrashShieldHandler {
  private val crashingObjects = Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
  private var enabled = false

  @JvmStatic
  fun enable() {
    enabled = true
  }

  @VisibleForTesting
  @JvmStatic
  fun disable() {
    enabled = false
  }

  @JvmStatic
  fun handleThrowable(e: Throwable?, o: Any) {
    if (!enabled) {
      return
    }
    crashingObjects.add(o)
    if (FacebookSdk.getAutoLogAppEventsEnabled()) {
      ExceptionAnalyzer.execute(e)
      InstrumentData.Builder.build(e, InstrumentData.Type.CrashShield).save()
    }
    scheduleCrashInDebug(e)
  }

  @JvmStatic
  fun isObjectCrashing(o: Any): Boolean {
    return crashingObjects.contains(o)
  }

  @JvmStatic fun methodFinished(o: Any?) = Unit

  @JvmStatic
  fun reset() {
    resetCrashingObjects()
  }

  @JvmStatic
  fun resetCrashingObjects() {
    crashingObjects.clear()
  }

  @VisibleForTesting @JvmStatic fun isDebug() = BuildConfig.DEBUG

  @VisibleForTesting
  @JvmStatic
  fun scheduleCrashInDebug(e: Throwable?) {
    if (isDebug()) {
      Handler(Looper.getMainLooper())
          .post(
              object : Runnable {
                @NoAutoExceptionHandling
                override fun run() {
                  // throw on main thread during development to avoid catching by crash shield
                  throw RuntimeException(e)
                }
              })
    }
  }
}
