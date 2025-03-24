/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.content.Context
import com.facebook.GraphRequest
import com.facebook.appevents.eventdeactivation.EventDeactivationManager.processEvents
import com.facebook.appevents.internal.AppEventsLoggerUtility
import com.facebook.appevents.internal.AppEventsLoggerUtility.getJSONObjectForGraphAPICall
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
internal class SessionEventsState(
    private val attributionIdentifiers: AttributionIdentifiers,
    private val anonymousAppDeviceGUID: String
) {
    private var accumulatedEvents: MutableList<AppEvent> = mutableListOf()
    private val inFlightEvents: MutableList<AppEvent> = mutableListOf()
    private var numSkippedEventsDueToFullBuffer = 0

    // Synchronize here and in other methods on this class, because could be coming in from
    // different AppEventsLoggers on different threads pointing at the same session.
    @Synchronized
    fun addEvent(event: AppEvent) {
        if (accumulatedEvents.size + inFlightEvents.size >= MAX_ACCUMULATED_LOG_EVENTS) {
            numSkippedEventsDueToFullBuffer++
        } else {
            accumulatedEvents.add(event)
        }
    }

    @get:Synchronized
    val accumulatedEventCount: Int
        get() = accumulatedEvents.size

    @Synchronized
    fun clearInFlightAndStats(moveToAccumulated: Boolean) {
        if (moveToAccumulated) {
            accumulatedEvents.addAll(inFlightEvents)
        }
        inFlightEvents.clear()
        numSkippedEventsDueToFullBuffer = 0
    }

    fun populateRequest(
        request: GraphRequest,
        applicationContext: Context,
        includeImplicitEvents: Boolean,
        limitEventUsage: Boolean
    ): Int {
        var numSkipped: Int
        var jsonArray: JSONArray
        var operationalJsonArray: JSONArray
        synchronized(this) {
            numSkipped = numSkippedEventsDueToFullBuffer

            // drop deprecated events
            processEvents(accumulatedEvents)

            // move all accumulated events to inFlight.
            inFlightEvents.addAll(accumulatedEvents)
            accumulatedEvents.clear()
            jsonArray = JSONArray()
            operationalJsonArray = JSONArray()
            for (event in inFlightEvents) {
                if (includeImplicitEvents || !event.isImplicit) {
                    jsonArray.put(event.jsonObject)
                    operationalJsonArray.put(event.operationalJsonObject)
                }
            }
            if (jsonArray.length() == 0) {
                return 0
            }
        }
        populateRequest(
            request,
            applicationContext,
            numSkipped,
            jsonArray,
            operationalJsonArray,
            limitEventUsage
        )
        return jsonArray.length()
    }

    // We will only persist accumulated events, not ones currently in-flight. This means if
    // an in-flight request fails, those requests will not be persisted and thus might be
    // lost if the process terminates while the flush is in progress.
    @get:Synchronized
    val eventsToPersist: List<AppEvent>
        get() {
            // We will only persist accumulated events, not ones currently in-flight. This means if
            // an in-flight request fails, those requests will not be persisted and thus might be
            // lost if the process terminates while the flush is in progress.
            val result: List<AppEvent> = accumulatedEvents
            accumulatedEvents = mutableListOf()
            return result
        }

    @Synchronized
    fun accumulatePersistedEvents(events: List<AppEvent>) {
        // We won't skip events due to a full buffer, since we already accumulated them once and
        // persisted them. But they will count against the buffer size when further events are
        // accumulated.
        accumulatedEvents.addAll(events)
    }

    private fun populateRequest(
        request: GraphRequest,
        applicationContext: Context,
        numSkipped: Int,
        events: JSONArray,
        operationalParameters: JSONArray,
        limitEventUsage: Boolean
    ) {
        var publishParams: JSONObject?
        try {
            publishParams =
                getJSONObjectForGraphAPICall(
                    AppEventsLoggerUtility.GraphAPIActivityType.CUSTOM_APP_EVENTS,
                    attributionIdentifiers,
                    anonymousAppDeviceGUID,
                    limitEventUsage,
                    applicationContext
                )
            if (numSkippedEventsDueToFullBuffer > 0) {
                publishParams.put("num_skipped_events", numSkipped)
            }
        } catch (e: JSONException) {
            // Swallow
            publishParams = JSONObject()
        }
        request.graphObject = publishParams
        val requestParameters = request.parameters
        val jsonString = events.toString()
        requestParameters.putString("custom_events", jsonString)
        if (isEnabled(FeatureManager.Feature.IapLoggingLib5To7)) {
            requestParameters.putString("operational_parameters", operationalParameters.toString())
        }
        request.tag = jsonString
        request.parameters = requestParameters
    }

    companion object {
        private val TAG = SessionEventsState::class.java.simpleName
        private val MAX_ACCUMULATED_LOG_EVENTS = 1000
    }
}
