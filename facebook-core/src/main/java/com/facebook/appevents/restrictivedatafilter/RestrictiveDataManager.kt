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
