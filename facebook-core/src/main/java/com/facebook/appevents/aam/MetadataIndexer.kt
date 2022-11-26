/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.aam

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import com.facebook.FacebookSdk
import com.facebook.appevents.aam.MetadataRule.Companion.getRules
import com.facebook.appevents.aam.MetadataRule.Companion.updateRules
import com.facebook.internal.AttributionIdentifiers.Companion.isTrackingLimited
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.Utility.logd
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.Exception

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object MetadataIndexer {
  private val TAG = MetadataIndexer::class.java.canonicalName
  private var enabled = false
  @UiThread
  @JvmStatic
  fun onActivityResumed(activity: Activity) {
    try {
      if (!enabled || getRules().isEmpty()) {
        return
      }
      MetadataViewObserver.startTrackingActivity(activity)
    } catch (e: Exception) {}
  }

  private fun updateRules() {
    val settings = queryAppSettings(FacebookSdk.getApplicationId(), false) ?: return
    val rawRule = settings.rawAamRules ?: return
    updateRules(rawRule)
  }

  @JvmStatic
  fun enable() {
    try {
      FacebookSdk.getExecutor().execute {
        val context = FacebookSdk.getApplicationContext()
        if (!isTrackingLimited(context)) {
          updateRules()
          enabled = true
        }
      }
    } catch (e: Exception) {
      logd(TAG, e)
    }
  }
}
