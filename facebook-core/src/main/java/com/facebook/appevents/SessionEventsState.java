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
import android.os.Bundle;

import com.facebook.GraphRequest;
import com.facebook.internal.AppEventsLoggerUtility;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

class SessionEventsState {
    private List<AppEvent> accumulatedEvents = new ArrayList<AppEvent>();
    private List<AppEvent> inFlightEvents = new ArrayList<AppEvent>();
    private int numSkippedEventsDueToFullBuffer;
    private AttributionIdentifiers attributionIdentifiers;
    private String anonymousAppDeviceGUID;

    private final int MAX_ACCUMULATED_LOG_EVENTS = 1000;

    public SessionEventsState(
            AttributionIdentifiers identifiers,
            String anonymousGUID) {
        this.attributionIdentifiers = identifiers;
        this.anonymousAppDeviceGUID = anonymousGUID;
    }

    // Synchronize here and in other methods on this class, because could be coming in from
    // different AppEventsLoggers on different threads pointing at the same session.
    public synchronized void addEvent(AppEvent event) {
        if (accumulatedEvents.size() + inFlightEvents.size() >= MAX_ACCUMULATED_LOG_EVENTS) {
            numSkippedEventsDueToFullBuffer++;
        } else {
            accumulatedEvents.add(event);
        }
    }

    public synchronized int getAccumulatedEventCount() {
        return accumulatedEvents.size();
    }

    public synchronized void clearInFlightAndStats(boolean moveToAccumulated) {
        if (moveToAccumulated) {
            accumulatedEvents.addAll(inFlightEvents);
        }
        inFlightEvents.clear();
        numSkippedEventsDueToFullBuffer = 0;
    }

    public int populateRequest(
            GraphRequest request,
            Context applicationContext,
            boolean includeImplicitEvents,
            boolean limitEventUsage) {

        int numSkipped;
        JSONArray jsonArray;
        synchronized (this) {
            numSkipped = numSkippedEventsDueToFullBuffer;

            // move all accumulated events to inFlight.
            inFlightEvents.addAll(accumulatedEvents);
            accumulatedEvents.clear();

            jsonArray = new JSONArray();
            for (AppEvent event : inFlightEvents) {
                if (event.isChecksumValid()) {
                    if (includeImplicitEvents || !event.getIsImplicit()) {
                        jsonArray.put(event.getJSONObject());
                    }
                } else {
                    Utility.logd("Event with invalid checksum: %s", event.toString());
                }
            }

            if (jsonArray.length() == 0) {
                return 0;
            }
        }

        populateRequest(
                request,
                applicationContext,
                numSkipped,
                jsonArray,
                limitEventUsage);
        return jsonArray.length();
    }

    public synchronized List<AppEvent> getEventsToPersist() {
        // We will only persist accumulated events, not ones currently in-flight. This means if
        // an in-flight request fails, those requests will not be persisted and thus might be
        // lost if the process terminates while the flush is in progress.
        List<AppEvent> result = accumulatedEvents;
        accumulatedEvents = new ArrayList<AppEvent>();
        return result;
    }

    public synchronized void accumulatePersistedEvents(List<AppEvent> events) {
        // We won't skip events due to a full buffer, since we already accumulated them once and
        // persisted them. But they will count against the buffer size when further events are
        // accumulated.
        accumulatedEvents.addAll(events);
    }

    private void populateRequest(
            GraphRequest request,
            Context applicationContext,
            int numSkipped,
            JSONArray events,
            boolean limitEventUsage) {
        JSONObject publishParams = null;
        try {
            publishParams = AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
                    AppEventsLoggerUtility.GraphAPIActivityType.CUSTOM_APP_EVENTS,
                    attributionIdentifiers,
                    anonymousAppDeviceGUID,
                    limitEventUsage,
                    applicationContext);

            if (numSkippedEventsDueToFullBuffer > 0) {
                publishParams.put("num_skipped_events", numSkipped);
            }
        } catch (JSONException e) {
            // Swallow
            publishParams = new JSONObject();
        }
        request.setGraphObject(publishParams);

        Bundle requestParameters = request.getParameters();
        if (requestParameters == null) {
            requestParameters = new Bundle();
        }

        String jsonString = events.toString();
        if (jsonString != null) {
            requestParameters.putByteArray(
                    "custom_events_file",
                    getStringAsByteArray(jsonString));
            request.setTag(jsonString);
        }
        request.setParameters(requestParameters);
    }

    private byte[] getStringAsByteArray(String jsonString) {
        byte[] jsonUtf8 = null;
        try {
            jsonUtf8 = jsonString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // shouldn't happen, but just in case:
            Utility.logd("Encoding exception: ", e);
        }
        return jsonUtf8;
    }
}
