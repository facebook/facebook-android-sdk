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
