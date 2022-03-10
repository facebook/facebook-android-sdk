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
