/**
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

package com.facebook.appevents;

import android.content.Context;

import com.facebook.FacebookSdk;
import com.facebook.internal.AttributionIdentifiers;

import java.util.HashMap;
import java.util.Set;

class AppEventCollection {
    private final HashMap<AccessTokenAppIdPair, SessionEventsState> stateMap;

    public AppEventCollection() {
        stateMap = new HashMap<>();
    }

    public synchronized void addPersistedEvents(PersistedEvents persistedEvents) {
        if (persistedEvents == null) {
            return;
        }

        for (AccessTokenAppIdPair accessTokenAppIdPair : persistedEvents.keySet()) {
            SessionEventsState sessionEventsState = getSessionEventsState(accessTokenAppIdPair);

            for (AppEvent appEvent : persistedEvents.get(accessTokenAppIdPair)) {
                sessionEventsState.addEvent(appEvent);
            }
        }
    }

    public synchronized void addEvent(
            AccessTokenAppIdPair accessTokenAppIdPair,
            AppEvent appEvent) {
        SessionEventsState eventsState = getSessionEventsState(accessTokenAppIdPair);
        eventsState.addEvent(appEvent);
    }

    public synchronized Set<AccessTokenAppIdPair> keySet() {
        return stateMap.keySet();
    }

    public synchronized SessionEventsState get(AccessTokenAppIdPair accessTokenAppIdPair) {
        return stateMap.get(accessTokenAppIdPair);
    }

    public synchronized int getEventCount() {
        int count = 0;
        for (SessionEventsState sessionEventsState : stateMap.values()) {
            count += sessionEventsState.getAccumulatedEventCount();
        }

        return count;
    }

    private synchronized SessionEventsState getSessionEventsState(
            AccessTokenAppIdPair accessTokenAppId) {
        SessionEventsState eventsState = stateMap.get(accessTokenAppId);
        if (eventsState == null) {
            Context context = FacebookSdk.getApplicationContext();

            // Retrieve attributionId, but we will only send it if attribution is supported for the
            // app.
            eventsState = new SessionEventsState(
                    AttributionIdentifiers.getAttributionIdentifiers(context),
                    AppEventsLogger.getAnonymousAppDeviceGUID(context));
        }

        stateMap.put(accessTokenAppId, eventsState);

        return eventsState;
    }
}
