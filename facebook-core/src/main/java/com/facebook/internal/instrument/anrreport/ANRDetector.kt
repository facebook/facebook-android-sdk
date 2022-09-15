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

package com.facebook.internal.instrument.anrreport

import android.app.ActivityManager
import android.content.Context
import android.os.Looper
import android.os.Process
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.InstrumentData.Builder.build
import com.facebook.internal.instrument.InstrumentUtility.getStackTrace
import com.facebook.internal.instrument.InstrumentUtility.isSDKRelatedThread
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ANRDetector {
  private const val DETECTION_INTERVAL_IN_MS = 500
  private val myUid = Process.myUid() // the identity of its app-specific sandbox.
  private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private var previousStackTrace: String? = ""
  private val anrDetectorRunnable = Runnable {
    try {
      val am: ActivityManager? =
          FacebookSdk.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE)
              as ActivityManager
      checkProcessError(am)
    } catch (e: Exception) {
      /*no op*/
    }
  }

  @JvmStatic
  @VisibleForTesting
  fun checkProcessError(am: ActivityManager?) {
    am?.processesInErrorState?.forEach { info ->
      if (info.condition == ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING &&
          info.uid == myUid) {
        val mainThread = Looper.getMainLooper().thread
        val stackTrace = getStackTrace(mainThread)
        if (stackTrace == previousStackTrace || !isSDKRelatedThread(mainThread)) {
          return@forEach
        }
        previousStackTrace = stackTrace
        build(info.shortMsg, stackTrace).save()
      }
    }
  }

  // Should be only called by ANRHandler
  @JvmStatic
  @VisibleForTesting
  fun start() {
    scheduledExecutorService.scheduleAtFixedRate(
        anrDetectorRunnable, 0, DETECTION_INTERVAL_IN_MS.toLong(), TimeUnit.MILLISECONDS)
  }
}
