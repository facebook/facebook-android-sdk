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

import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.internal.AttributionIdentifiers.Companion.getAttributionIdentifiers
import java.util.HashMap

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class AppEventCollection {
  private val stateMap: HashMap<AccessTokenAppIdPair, SessionEventsState> = HashMap()

  @Synchronized
  fun addPersistedEvents(persistedEvents: PersistedEvents?) {
    if (persistedEvents == null) {
      return
    }
    for (accessTokenAppIdPair in persistedEvents.keySet()) {
      getSessionEventsState(accessTokenAppIdPair)?.let {
        for (appEvent in checkNotNull(persistedEvents[accessTokenAppIdPair])) {
          it.addEvent(appEvent)
        }
      }
    }
  }

  @Synchronized
  fun addEvent(accessTokenAppIdPair: AccessTokenAppIdPair, appEvent: AppEvent) {
    getSessionEventsState(accessTokenAppIdPair)?.addEvent(appEvent)
  }

  @Synchronized
  fun keySet(): Set<AccessTokenAppIdPair> {
    return stateMap.keys
  }

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
      eventsState =
          getAttributionIdentifiers(context)?.let {
            SessionEventsState(it, AppEventsLogger.getAnonymousAppDeviceGUID(context))
          }
    }
    if (eventsState == null) {
      return null
    }
    stateMap[accessTokenAppId] = eventsState
    return eventsState
  }
}
