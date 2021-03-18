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
