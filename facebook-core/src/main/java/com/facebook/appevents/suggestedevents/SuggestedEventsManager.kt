/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.suggestedevents

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.appevents.internal.ActivityLifecycleTracker.getCurrentActivity
import com.facebook.appevents.ml.ModelManager
import com.facebook.appevents.ml.ModelManager.getRuleFile
import com.facebook.appevents.suggestedevents.FeatureExtractor.initialize
import com.facebook.appevents.suggestedevents.FeatureExtractor.isInitialized
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SuggestedEventsManager {
  private val enabled = AtomicBoolean(false)
  private val productionEvents: MutableSet<String> = mutableSetOf()
  private val eligibleEvents: MutableSet<String> = mutableSetOf()
  private const val PRODUCTION_EVENTS_KEY = "production_events"
  private const val ELIGIBLE_EVENTS_KEY = "eligible_for_prediction_events"

  @Synchronized
  @JvmStatic
  fun enable() {
    FacebookSdk.getExecutor()
        .execute(
            Runnable {
              if (enabled.get()) {
                return@Runnable
              }
              enabled.set(true)
              initialize()
            })
  }

  private fun initialize() {
    try {
      val settings = queryAppSettings(FacebookSdk.getApplicationId(), false) ?: return
      val rawSuggestedEventSetting = settings.suggestedEventsSetting ?: return
      populateEventsFromRawJsonString(rawSuggestedEventSetting)
      if (productionEvents.isNotEmpty() || eligibleEvents.isNotEmpty()) {
        val ruleFile = getRuleFile(ModelManager.Task.MTML_APP_EVENT_PREDICTION) ?: return
        initialize(ruleFile)
        val currActivity = getCurrentActivity()
        if (currActivity != null) {
          trackActivity(currActivity)
        }
      }
    } catch (e: Exception) {
      /*no op*/
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun populateEventsFromRawJsonString(rawSuggestedEventSetting: String?) {
    try {
      val jsonObject = JSONObject(rawSuggestedEventSetting)
      if (jsonObject.has(PRODUCTION_EVENTS_KEY)) {
        val jsonArray = jsonObject.getJSONArray(PRODUCTION_EVENTS_KEY)
        for (i in 0 until jsonArray.length()) {
          productionEvents.add(jsonArray.getString(i))
        }
      }
      if (jsonObject.has(ELIGIBLE_EVENTS_KEY)) {
        val jsonArray = jsonObject.getJSONArray(ELIGIBLE_EVENTS_KEY)
        for (i in 0 until jsonArray.length()) {
          eligibleEvents.add(jsonArray.getString(i))
        }
      }
    } catch (e: Exception) {
      /*noop*/
    }
  }

  @JvmStatic
  fun trackActivity(activity: Activity) {
    try {
      if (enabled.get() &&
          isInitialized() &&
          (!productionEvents.isEmpty() || !eligibleEvents.isEmpty())) {
        ViewObserver.startTrackingActivity(activity)
      } else {
        ViewObserver.stopTrackingActivity(activity)
      }
    } catch (e: Exception) {
      /*no op*/
    }
  }

  @JvmStatic
  fun isEnabled(): Boolean {
    return enabled.get()
  }

  @JvmStatic
  internal fun isProductionEvents(event: String): Boolean {
    return productionEvents.contains(event)
  }

  @JvmStatic
  internal fun isEligibleEvents(event: String): Boolean {
    return eligibleEvents.contains(event)
  }
}
