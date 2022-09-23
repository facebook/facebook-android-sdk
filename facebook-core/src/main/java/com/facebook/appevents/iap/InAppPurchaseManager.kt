/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseManager {
  private const val GOOGLE_BILLINGCLIENT_VERSION = "com.google.android.play.billingclient.version"
  private val enabled = AtomicBoolean(false)

  @JvmStatic
  fun enableAutoLogging() {
    enabled.set(true)
    startTracking()
  }

  @JvmStatic
  fun startTracking() {
    if (enabled.get()) {
      if (usingBillingLib2Plus() && isEnabled(FeatureManager.Feature.IapLoggingLib2)) {
        InAppPurchaseAutoLogger.startIapLogging(getApplicationContext())
      } else {
        InAppPurchaseActivityLifecycleTracker.startIapLogging()
      }
    }
  }

  private fun usingBillingLib2Plus(): Boolean {
    return try {
      val context = getApplicationContext()
      val info =
          context.packageManager.getApplicationInfo(
              context.packageName, PackageManager.GET_META_DATA)
      if (info != null) {
        val version = info.metaData.getString(GOOGLE_BILLINGCLIENT_VERSION)
        val versionArray = if (version === null) return false else version.split(".", limit = 3)
        return versionArray[0].toInt() >= 2
      }
      false
    } catch (e: Exception) {
      false
    }
  }
}
