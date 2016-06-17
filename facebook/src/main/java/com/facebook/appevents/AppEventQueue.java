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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.LoggingBehavior;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class AppEventQueue {
    private static final String TAG = AppEventQueue.class.getName();

    private static final int NUM_LOG_EVENTS_TO_TRY_TO_FLUSH_AFTER = 100;
    private static final int FLUSH_PERIOD_IN_SECONDS = 15;

    private static volatile AppEventCollection appEventCollection = new AppEventCollection();
    private static final ScheduledExecutorService singleThreadExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture scheduledFuture;

    // Only call for the singleThreadExecutor
    private static final Runnable flushRunnable = new Runnable() {
        @Override
        public void run() {
            scheduledFuture = null;

            if (AppEventsLogger.getFlushBehavior() !=
                    AppEventsLogger.FlushBehavior.EXPLICIT_ONLY) {
                flushAndWait(FlushReason.TIMER);
            }
        }
    };

    public static void persistToDisk() {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                AppEventStore.persistEvents(appEventCollection);
                appEventCollection = new AppEventCollection();
            }
        });
    }

    public static void flush(
            final FlushReason reason) {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                flushAndWait(reason);
            }
        });
    }

    public static void add(
            final AccessTokenAppIdPair accessTokenAppId,
            final AppEvent appEvent) {
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                appEventCollection.addEvent(accessTokenAppId, appEvent);

                if (AppEventsLogger.getFlushBehavior() !=
                        AppEventsLogger.FlushBehavior.EXPLICIT_ONLY
                        && appEventCollection.getEventCount() >
                        NUM_LOG_EVENTS_TO_TRY_TO_FLUSH_AFTER) {
                    flushAndWait(FlushReason.EVENT_THRESHOLD);
                } else if (scheduledFuture == null) {
                    scheduledFuture = singleThreadExecutor.schedule(
                            flushRunnable,
                            FLUSH_PERIOD_IN_SECONDS,
                            TimeUnit.SECONDS
                    );
                }
            }
        });
    }

    public static Set<AccessTokenAppIdPair> getKeySet() {
        // This is safe to call outside of the singleThreadExecutor since
        // the appEventCollection is volatile and the modifying methods within the
        // class are synchronized.
        return appEventCollection.keySet();
    }

    static void flushAndWait(FlushReason reason) {
        // Read and send any persisted events
        PersistedEvents result = AppEventStore.readAndClearStore();
        // Add any of the persisted app events to our list of events to send
        appEventCollection.addPersistedEvents(result);

        FlushStatistics flushResults;

        try {
            flushResults = sendEventsToServer(
                    reason,
                    appEventCollection);
        } catch (Exception e) {
            Log.w(TAG, "Caught unexpected exception while flushing app events: ", e);
            return;
        }

        if (flushResults != null) {
            final Intent intent = new Intent(AppEventsLogger.ACTION_APP_EVENTS_FLUSHED);
            intent.putExtra(
                    AppEventsLogger.APP_EVENTS_EXTRA_NUM_EVENTS_FLUSHED,
                    flushResults.numEvents);
            intent.putExtra(
                    AppEventsLogger.APP_EVENTS_EXTRA_FLUSH_RESULT,
                    flushResults.result);
            Context context = FacebookSdk.getApplicationContext();
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    private static FlushStatistics sendEventsToServer(
            FlushReason reason,
            AppEventCollection appEventCollection) {
        FlushStatistics flushResults = new FlushStatistics();

        Context context = FacebookSdk.getApplicationContext();
        boolean limitEventUsage = FacebookSdk.getLimitEventAndDataUsage(context);

        List<GraphRequest> requestsToExecute = new ArrayList<>();
        for (AccessTokenAppIdPair accessTokenAppId : appEventCollection.keySet()) {
            GraphRequest request = buildRequestForSession(
                    accessTokenAppId,
                    appEventCollection.get(accessTokenAppId),
                    limitEventUsage,
                    flushResults);
            if (request != null) {
                requestsToExecute.add(request);
            }
        }

        if (requestsToExecute.size() > 0) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "Flushing %d events due to %s.",
                    flushResults.numEvents,
                    reason.toString());

            for (GraphRequest request : requestsToExecute) {
                // Execute the request synchronously. Callbacks will take care of handling errors
                // and updating our final overall result.
                request.executeAndWait();
            }
            return flushResults;
        }

        return null;
    }

    private static GraphRequest buildRequestForSession(
            final AccessTokenAppIdPair accessTokenAppId,
            final SessionEventsState appEvents,
            final boolean limitEventUsage,
            final FlushStatistics flushState) {
        String applicationId = accessTokenAppId.getApplicationId();

        Utility.FetchedAppSettings fetchedAppSettings =
                Utility.queryAppSettings(applicationId, false);

        final GraphRequest postRequest = GraphRequest.newPostRequest(
                null,
                String.format("%s/activities", applicationId),
                null,
                null);

        Bundle requestParameters = postRequest.getParameters();
        if (requestParameters == null) {
            requestParameters = new Bundle();
        }
        requestParameters.putString("access_token", accessTokenAppId.getAccessTokenString());
        String pushNotificationsRegistrationId =
                AppEventsLogger.getPushNotificationsRegistrationId();
        if (pushNotificationsRegistrationId != null) {
            requestParameters.putString("device_token", pushNotificationsRegistrationId);
        }

        postRequest.setParameters(requestParameters);

        if (fetchedAppSettings == null) {
            return null;
        }

        int numEvents = appEvents.populateRequest(
                postRequest,
                FacebookSdk.getApplicationContext(),
                fetchedAppSettings.supportsImplicitLogging(),
                limitEventUsage);

        if (numEvents == 0) {
            return null;
        }

        flushState.numEvents += numEvents;

        postRequest.setCallback(new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                handleResponse(
                        accessTokenAppId,
                        postRequest,
                        response,
                        appEvents,
                        flushState);
            }
        });

        return postRequest;
    }

    private static void handleResponse(
            final AccessTokenAppIdPair accessTokenAppId,
            GraphRequest request,
            GraphResponse response,
            final SessionEventsState appEvents,
            FlushStatistics flushState) {
        FacebookRequestError error = response.getError();
        String resultDescription = "Success";

        FlushResult flushResult = FlushResult.SUCCESS;

        if (error != null) {
            final int NO_CONNECTIVITY_ERROR_CODE = -1;
            if (error.getErrorCode() == NO_CONNECTIVITY_ERROR_CODE) {
                resultDescription = "Failed: No Connectivity";
                flushResult = FlushResult.NO_CONNECTIVITY;
            } else {
                resultDescription = String.format("Failed:\n  Response: %s\n  Error %s",
                        response.toString(),
                        error.toString());
                flushResult = FlushResult.SERVER_ERROR;
            }
        }

        if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.APP_EVENTS)) {
            String eventsJsonString = (String) request.getTag();
            String prettyPrintedEvents;

            try {
                JSONArray jsonArray = new JSONArray(eventsJsonString);
                prettyPrintedEvents = jsonArray.toString(2);
            } catch (JSONException exc) {
                prettyPrintedEvents = "<Can't encode events for debug logging>";
            }

            Logger.log(LoggingBehavior.APP_EVENTS, TAG,
                    "Flush completed\nParams: %s\n  Result: %s\n  Events JSON: %s",
                    request.getGraphObject().toString(),
                    resultDescription,
                    prettyPrintedEvents);
        }

        appEvents.clearInFlightAndStats(error != null);

        if (flushResult == FlushResult.NO_CONNECTIVITY) {
            // We may call this for multiple requests in a batch, which is slightly inefficient
            // since in principle we could call it once for all failed requests, but the impact is
            // likely to be minimal. We don't call this for other server errors, because if an event
            // failed because it was malformed, etc., continually retrying it will cause subsequent
            // events to not be logged either.
            FacebookSdk.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    AppEventStore.persistEvents(accessTokenAppId, appEvents);
                }
            });
        }

        if (flushResult != FlushResult.SUCCESS) {
            // We assume that connectivity issues are more significant to report than server issues.
            if (flushState.result != FlushResult.NO_CONNECTIVITY) {
                flushState.result = flushResult;
            }
        }
    }
}
