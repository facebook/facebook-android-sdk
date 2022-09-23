/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.eventdeactivation

import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.Utility.convertJSONArrayToList
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.Exception
import java.util.ArrayList
import java.util.HashSet
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object EventDeactivationManager {
  private var enabled = false
  private val deprecatedParamFilters: MutableList<DeprecatedParamFilter> = ArrayList()
  private val deprecatedEvents: MutableSet<String> = HashSet()

  @JvmStatic
  fun enable() {
    enabled = true
    initialize()
  }

  @Synchronized
  private fun initialize() {
    try {
      val settings = queryAppSettings(FacebookSdk.getApplicationId(), false) ?: return
      val eventFilterResponse = settings.restrictiveDataSetting
      if (eventFilterResponse !== null && eventFilterResponse.isNotEmpty()) {
        val jsonObject = JSONObject(eventFilterResponse)
        deprecatedParamFilters.clear()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val json = jsonObject.getJSONObject(key)
          if (json != null) {
            if (json.optBoolean("is_deprecated_event")) {
              deprecatedEvents.add(key)
            } else {
              val deprecatedParamJsonArray = json.optJSONArray("deprecated_param")
              val deprecatedParamFilter = DeprecatedParamFilter(key, ArrayList())
              if (deprecatedParamJsonArray != null) {
                deprecatedParamFilter.deprecateParams =
                    convertJSONArrayToList(deprecatedParamJsonArray)
              }
              deprecatedParamFilters.add(deprecatedParamFilter)
            }
          }
        }
      }
    } catch (e: Exception) {
      /* swallow */
    }
  }

  @JvmStatic
  fun processEvents(events: MutableList<AppEvent>) {
    if (!enabled) {
      return
    }
    val iterator = events.iterator()
    while (iterator.hasNext()) {
      val event = iterator.next()
      if (deprecatedEvents.contains(event.name)) {
        iterator.remove()
      }
    }
  }

  @JvmStatic
  fun processDeprecatedParameters(parameters: MutableMap<String, String?>, eventName: String) {
    if (!enabled) {
      return
    }
    val keys: List<String> = ArrayList(parameters.keys)
    val deprecatedParamFiltersCopy: List<DeprecatedParamFilter> = ArrayList(deprecatedParamFilters)
    for (filter in deprecatedParamFiltersCopy) {
      if (filter.eventName != eventName) {
        continue
      }
      for (key in keys) {
        if (filter.deprecateParams.contains(key)) {
          parameters.remove(key)
        }
      }
    }
  }

  internal class DeprecatedParamFilter(var eventName: String, var deprecateParams: List<String>)
}
