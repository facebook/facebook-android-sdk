/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.restrictivedatafilter

import android.util.Log
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.Utility.convertJSONObjectToStringMap
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.CopyOnWriteArraySet
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object RestrictiveDataManager {
  private var enabled = false
  private val TAG = RestrictiveDataManager::class.java.canonicalName
  private val restrictiveParamFilters: MutableList<RestrictiveParamFilter> = ArrayList()
  private val restrictedEvents: MutableSet<String> = CopyOnWriteArraySet()
  private const val REPLACEMENT_STRING = "_removed_"
  private const val PROCESS_EVENT_NAME = "process_event_name"
  private const val RESTRICTIVE_PARAM = "restrictive_param"
  private const val RESTRICTIVE_PARAM_KEY = "_restrictedParams"

  @JvmStatic
  fun enable() {
    enabled = true
    initialize()
  }

  private fun initialize() {
    try {
      val settings = queryAppSettings(FacebookSdk.getApplicationId(), false) ?: return
      val restrictiveDataSetting = settings.restrictiveDataSetting
      if (restrictiveDataSetting == null || restrictiveDataSetting.isEmpty()) {
        return
      }
      val restrictiveData = JSONObject(restrictiveDataSetting)
      restrictiveParamFilters.clear()
      restrictedEvents.clear()
      val keys = restrictiveData.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val filteredValues = restrictiveData.getJSONObject(key)
        if (filteredValues != null) {
          val restrictiveParamJson = filteredValues.optJSONObject(RESTRICTIVE_PARAM)
          val restrictiveParamFilter = RestrictiveParamFilter(key, HashMap())
          if (restrictiveParamJson != null) {
            restrictiveParamFilter.restrictiveParams =
                convertJSONObjectToStringMap(restrictiveParamJson)
            restrictiveParamFilters.add(restrictiveParamFilter)
          }
          if (filteredValues.has(PROCESS_EVENT_NAME)) {
            restrictedEvents.add(restrictiveParamFilter.eventName)
          }
        }
      }
    } catch (e: Exception) {
      /* swallow */
    }
  }

  @JvmStatic
  fun processEvent(eventName: String): String {
    return if (enabled && isRestrictedEvent(eventName)) {
      REPLACEMENT_STRING
    } else eventName
  }

  @JvmStatic
  fun processParameters(parameters: MutableMap<String, String?>, eventName: String) {
    if (!enabled) {
      return
    }
    val restrictedParams: MutableMap<String, String> = HashMap()
    val keys: List<String> = ArrayList(parameters.keys)
    for (key in keys) {
      val type = getMatchedRuleType(eventName, key)
      if (type != null) {
        restrictedParams[key] = type
        parameters.remove(key)
      }
    }
    if (restrictedParams.isNotEmpty()) {
      try {
        val restrictedJSON = JSONObject()
        for ((key, value) in restrictedParams) {
          restrictedJSON.put(key, value)
        }
        parameters[RESTRICTIVE_PARAM_KEY] = restrictedJSON.toString()
      } catch (e: JSONException) {
        /* swallow */
      }
    }
  }

  private fun getMatchedRuleType(eventName: String, paramKey: String): String? {
    try {
      val restrictiveParamFiltersCopy: List<RestrictiveParamFilter> =
          ArrayList(restrictiveParamFilters)
      for (filter in restrictiveParamFiltersCopy) {
        if (filter == null) { // sanity check
          continue
        }
        if (eventName == filter.eventName) {
          for (param in filter.restrictiveParams.keys) {
            if (paramKey == param) {
              return filter.restrictiveParams[param]
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "getMatchedRuleType failed", e)
    }
    return null
  }

  private fun isRestrictedEvent(eventName: String): Boolean {
    return restrictedEvents.contains(eventName)
  }

  internal class RestrictiveParamFilter(
      var eventName: String,
      var restrictiveParams: Map<String, String?>
  )
}
