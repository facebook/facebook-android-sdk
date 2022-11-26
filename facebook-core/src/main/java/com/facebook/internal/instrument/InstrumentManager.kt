/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument

import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.internal.FeatureManager
import com.facebook.internal.instrument.anrreport.ANRHandler
import com.facebook.internal.instrument.crashreport.CrashHandler
import com.facebook.internal.instrument.crashshield.CrashShieldHandler
import com.facebook.internal.instrument.errorreport.ErrorReportHandler
import com.facebook.internal.instrument.threadcheck.ThreadCheckHandler

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InstrumentManager {
  /**
   * Start Instrument functionality.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown when loading and sending crash reports.
   */
  @JvmStatic
  fun start() {
    if (!FacebookSdk.getAutoLogAppEventsEnabled()) {
      return
    }
    FeatureManager.checkFeature(FeatureManager.Feature.CrashReport) { enabled ->
      if (enabled) {
        CrashHandler.enable()
        if (FeatureManager.isEnabled(FeatureManager.Feature.CrashShield)) {
          ExceptionAnalyzer.enable()
          CrashShieldHandler.enable()
        }
        if (FeatureManager.isEnabled(FeatureManager.Feature.ThreadCheck)) {
          ThreadCheckHandler.enable()
        }
      }
    }
    FeatureManager.checkFeature(FeatureManager.Feature.ErrorReport) { enabled ->
      if (enabled) {
        ErrorReportHandler.enable()
      }
    }
    FeatureManager.checkFeature(FeatureManager.Feature.AnrReport) { enabled ->
      if (enabled) {
        ANRHandler.enable()
      }
    }
  }
}
