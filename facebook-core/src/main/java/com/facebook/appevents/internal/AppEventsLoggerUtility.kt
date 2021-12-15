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

package com.facebook.appevents.internal

import android.content.Context
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.Utility.dataProcessingOptions
import com.facebook.internal.Utility.setAppEventAttributionParameters
import com.facebook.internal.Utility.setAppEventExtendedDeviceInfoParameters
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.appevents.internal is solely for the use of other packages within the Facebook SDK
 * for Android. Use of any of the classes in this package is unsupported, and they may be modified
 * or removed without warning at any time.
 */
object AppEventsLoggerUtility {
  private val API_ACTIVITY_TYPE_TO_STRING: Map<GraphAPIActivityType, String> =
      hashMapOf(
          GraphAPIActivityType.MOBILE_INSTALL_EVENT to "MOBILE_APP_INSTALL",
          GraphAPIActivityType.CUSTOM_APP_EVENTS to "CUSTOM_APP_EVENTS")

  @Throws(JSONException::class)
  @JvmStatic
  fun getJSONObjectForGraphAPICall(
      activityType: GraphAPIActivityType,
      attributionIdentifiers: AttributionIdentifiers?,
      anonymousAppDeviceGUID: String?,
      limitEventUsage: Boolean,
      context: Context
  ): JSONObject {
    val publishParams = JSONObject()
    publishParams.put("event", API_ACTIVITY_TYPE_TO_STRING[activityType])
    val externalAnalyticsUserId = AppEventsLogger.getUserID()
    if (externalAnalyticsUserId != null) {
      publishParams.put("app_user_id", externalAnalyticsUserId)
    }
    setAppEventAttributionParameters(
        publishParams, attributionIdentifiers, anonymousAppDeviceGUID, limitEventUsage, context)

    // The code to get all the Extended info is safe but just in case we can wrap the
    // whole call in its own try/catch block since some of the things it does might
    // cause unexpected exceptions on rooted/funky devices:
    try {
      setAppEventExtendedDeviceInfoParameters(publishParams, context)
    } catch (e: Exception) {
      // Swallow but log
      log(
          LoggingBehavior.APP_EVENTS,
          "AppEvents",
          "Fetching extended device info parameters failed: '%s'",
          e.toString())
    }
    val dataProcessingOptions = dataProcessingOptions
    if (dataProcessingOptions != null) {
      val it = dataProcessingOptions.keys()
      while (it.hasNext()) {
        val key = it.next()
        publishParams.put(key, dataProcessingOptions[key])
      }
    }
    publishParams.put("application_package_name", context.packageName)
    return publishParams
  }

  enum class GraphAPIActivityType {
    MOBILE_INSTALL_EVENT,
    CUSTOM_APP_EVENTS
  }
}
