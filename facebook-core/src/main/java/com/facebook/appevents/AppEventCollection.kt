/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import com.facebook.FacebookSdk
import com.facebook.internal.AttributionIdentifiers.Companion.getAttributionIdentifiers
import java.util.HashMap

internal class AppEventCollection {
  private val stateMap: HashMap<AccessTokenAppIdPair, SessionEventsState> = HashMap()

  @Synchronized
  fun addPersistedEvents(persistedEvents: PersistedEvents?) {
    if (persistedEvents == null) {
      return
    }
    for (entry in persistedEvents.entrySet()) {
      val state = getSessionEventsState(entry.key)
      if (state != null) {
        for (appEvent in entry.value) {
          state.addEvent(appEvent)
        }
      }
    }
  }

  @Synchronized
  fun addEvent(accessTokenAppIdPair: AccessTokenAppIdPair, appEvent: AppEvent) {
    getSessionEventsState(accessTokenAppIdPair)?.addEvent(appEvent)
  }

  @Synchronized fun keySet(): Set<AccessTokenAppIdPair> = stateMap.keys

  @Synchronized
  operator fun get(accessTokenAppIdPair: AccessTokenAppIdPair): SessionEventsState? {
    return stateMap[accessTokenAppIdPair]
  }

  @get:Synchronized
  val eventCount: Int
    get() {
      var count = 0
      for (sessionEventsState in stateMap.values) {
        count += sessionEventsState.accumulatedEventCount
      }
      return count
    }

  @Synchronized
  private fun getSessionEventsState(accessTokenAppId: AccessTokenAppIdPair): SessionEventsState? {
    var eventsState = stateMap[accessTokenAppId]
    if (eventsState == null) {
      val context = FacebookSdk.getApplicationContext()

      // Retrieve attributionId, but we will only send it if attribution is supported for the
      // app.
      val identifier = getAttributionIdentifiers(context)
      if (identifier != null) {
        eventsState =
            SessionEventsState(identifier, AppEventsLogger.getAnonymousAppDeviceGUID(context))
      }
    }
    if (eventsState == null) {
      return null
    }
    stateMap[accessTokenAppId] = eventsState
    return eventsState
  }
}
