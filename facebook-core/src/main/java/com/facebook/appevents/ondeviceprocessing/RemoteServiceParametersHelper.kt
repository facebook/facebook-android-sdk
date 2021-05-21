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
package com.facebook.appevents.ondeviceprocessing

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.facebook.appevents.AppEvent
import com.facebook.appevents.eventdeactivation.EventDeactivationManager
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.Utility.logd
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.ArrayList
import org.json.JSONArray

@AutoHandleExceptions
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object RemoteServiceParametersHelper {
  private val TAG = RemoteServiceWrapper::class.java.simpleName

  @JvmStatic
  fun buildEventsBundle(
      eventType: RemoteServiceWrapper.EventType,
      applicationId: String,
      appEvents: List<AppEvent>
  ): Bundle? {
    val appEventsList = ArrayList(appEvents)
    val eventBundle = Bundle()
    eventBundle.putString("event", eventType.toString())
    eventBundle.putString("app_id", applicationId)
    if (RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS == eventType) {
      val filteredEventsJson = buildEventsJson(appEventsList, applicationId)
      if (filteredEventsJson.length() == 0) {
        return null
      }
      eventBundle.putString("custom_events", filteredEventsJson.toString())
    }
    return eventBundle
  }

  private fun buildEventsJson(appEvents: List<AppEvent>, applicationId: String): JSONArray {
    val filteredEventsJsonArray = JSONArray()

    // Drop deprecated events
    EventDeactivationManager.processEvents(appEvents)
    val includeImplicitEvents = includeImplicitEvents(applicationId)
    for (event in appEvents) {
      if (event.isChecksumValid) {
        val isExplicitEvent = !event.isImplicit
        if (isExplicitEvent || event.isImplicit && includeImplicitEvents) {
          filteredEventsJsonArray.put(event.jsonObject)
        }
      } else {
        logd(TAG, "Event with invalid checksum: $event")
      }
    }
    return filteredEventsJsonArray
  }

  private fun includeImplicitEvents(applicationId: String): Boolean {
    var supportsImplicitLogging = false
    val appSettings = queryAppSettings(applicationId, false)
    if (appSettings != null) {
      supportsImplicitLogging = appSettings.supportsImplicitLogging()
    }
    return supportsImplicitLogging
  }
}
