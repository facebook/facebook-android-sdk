/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.ondeviceprocessing

import android.os.Bundle
import com.facebook.appevents.AppEvent
import com.facebook.appevents.eventdeactivation.EventDeactivationManager
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONArray

@AutoHandleExceptions
internal object RemoteServiceParametersHelper {
    private val TAG = RemoteServiceWrapper::class.java.simpleName

    @JvmStatic
    fun buildEventsBundle(
        eventType: RemoteServiceWrapper.EventType,
        applicationId: String,
        appEvents: List<AppEvent>
    ): Bundle? {
        val eventBundle = Bundle()
        eventBundle.putString("event", eventType.toString())
        eventBundle.putString("app_id", applicationId)
        if (RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS == eventType) {
            val filteredEventsJson = buildEventsJson(appEvents, applicationId)
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
        val mutableAppEvents = appEvents.toMutableList()
        EventDeactivationManager.processEvents(mutableAppEvents)
        val includeImplicitEvents = includeImplicitEvents(applicationId)
        for (event in mutableAppEvents) {
            val isExplicitEvent = !event.isImplicit
            if (isExplicitEvent || event.isImplicit && includeImplicitEvents) {
                filteredEventsJsonArray.put(event.jsonObject)
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
