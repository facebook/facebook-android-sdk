/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
    scheduledExecutorService.scheduleWithFixedDelay(
        anrDetectorRunnable, 0, DETECTION_INTERVAL_IN_MS.toLong(), TimeUnit.MILLISECONDS)
  }
}
