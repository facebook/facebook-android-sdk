/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
