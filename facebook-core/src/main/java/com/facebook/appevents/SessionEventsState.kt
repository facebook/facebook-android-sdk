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

package com.facebook.appevents

import android.content.Context
import com.facebook.GraphRequest
import com.facebook.appevents.eventdeactivation.EventDeactivationManager.processEvents
import com.facebook.appevents.internal.AppEventsLoggerUtility
import com.facebook.appevents.internal.AppEventsLoggerUtility.getJSONObjectForGraphAPICall
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.Utility.logd
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
    synchronized(this) {
      numSkipped = numSkippedEventsDueToFullBuffer

      // drop deprecated events
      processEvents(accumulatedEvents)

      // move all accumulated events to inFlight.
      inFlightEvents.addAll(accumulatedEvents)
      accumulatedEvents.clear()
      jsonArray = JSONArray()
      for (event in inFlightEvents) {
        if (event.isChecksumValid) {
          if (includeImplicitEvents || !event.isImplicit) {
            jsonArray.put(event.jsonObject)
          }
        } else {
          logd(TAG, "Event with invalid checksum: $event")
        }
      }
      if (jsonArray.length() == 0) {
        return 0
      }
    }
    populateRequest(request, applicationContext, numSkipped, jsonArray, limitEventUsage)
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
              applicationContext)
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
    request.tag = jsonString
    request.parameters = requestParameters
  }

  companion object {
    private val TAG = SessionEventsState::class.java.simpleName
    private val MAX_ACCUMULATED_LOG_EVENTS = 1000
  }
}
