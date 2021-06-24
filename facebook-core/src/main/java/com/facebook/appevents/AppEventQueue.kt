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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphRequest.Companion.newPostRequest
import com.facebook.GraphResponse
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventStore.persistEvents
import com.facebook.appevents.AppEventStore.readAndClearStore
import com.facebook.appevents.InternalAppEventsLogger.Companion.getPushNotificationsRegistrationId
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONException

@AutoHandleExceptions
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object AppEventQueue {
  private val TAG = AppEventQueue::class.java.name
  private val NUM_LOG_EVENTS_TO_TRY_TO_FLUSH_AFTER = 100
  private const val FLUSH_PERIOD_IN_SECONDS = 15
  private const val NO_CONNECTIVITY_ERROR_CODE = -1

  @Volatile private var appEventCollection = AppEventCollection()
  private val singleThreadExecutor = Executors.newSingleThreadScheduledExecutor()
  private var scheduledFuture: ScheduledFuture<*>? = null

  // Only call for the singleThreadExecutor
  private val flushRunnable = Runnable {
    scheduledFuture = null
    if (AppEventsLogger.getFlushBehavior() != AppEventsLogger.FlushBehavior.EXPLICIT_ONLY) {
      flushAndWait(FlushReason.TIMER)
    }
  }

  @JvmStatic
  fun persistToDisk() {
    singleThreadExecutor.execute {
      persistEvents(appEventCollection)
      appEventCollection = AppEventCollection()
    }
  }

  @JvmStatic
  fun flush(reason: FlushReason) {
    singleThreadExecutor.execute { flushAndWait(reason) }
  }

  @JvmStatic
  fun add(accessTokenAppId: AccessTokenAppIdPair?, appEvent: AppEvent?) {
    singleThreadExecutor.execute {
      appEventCollection.addEvent(accessTokenAppId, appEvent)
      if (AppEventsLogger.getFlushBehavior() != AppEventsLogger.FlushBehavior.EXPLICIT_ONLY &&
          appEventCollection.eventCount > NUM_LOG_EVENTS_TO_TRY_TO_FLUSH_AFTER) {
        flushAndWait(FlushReason.EVENT_THRESHOLD)
      } else if (scheduledFuture == null) {
        scheduledFuture =
            singleThreadExecutor.schedule(
                flushRunnable, FLUSH_PERIOD_IN_SECONDS.toLong(), TimeUnit.SECONDS)
      }
    }
  }

  @JvmStatic
  fun getKeySet(): Set<AccessTokenAppIdPair> {
    // This is safe to call outside of the singleThreadExecutor since
    // the appEventCollection is volatile and the modifying methods within the
    // class are synchronized.
    return appEventCollection.keySet()
  }

  @JvmStatic
  fun flushAndWait(reason: FlushReason) {
    // Read and send any persisted events
    val result = readAndClearStore()
    // Add any of the persisted app events to our list of events to send
    appEventCollection.addPersistedEvents(result)
    val flushResults: FlushStatistics?
    flushResults =
        try {
          sendEventsToServer(reason, appEventCollection)
        } catch (e: Exception) {
          Log.w(TAG, "Caught unexpected exception while flushing app events: ", e)
          return
        }
    if (flushResults != null) {
      val intent = Intent(AppEventsLogger.ACTION_APP_EVENTS_FLUSHED)
      intent.putExtra(AppEventsLogger.APP_EVENTS_EXTRA_NUM_EVENTS_FLUSHED, flushResults.numEvents)
      intent.putExtra(AppEventsLogger.APP_EVENTS_EXTRA_FLUSH_RESULT, flushResults.result)
      val context = FacebookSdk.getApplicationContext()
      LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
  }

  private fun sendEventsToServer(
      reason: FlushReason,
      appEventCollection: AppEventCollection
  ): FlushStatistics? {
    val flushResults = FlushStatistics()
    val requestsToExecute = buildRequests(appEventCollection, flushResults)
    if (requestsToExecute.isNotEmpty()) {
      log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "Flushing %d events due to %s.",
          flushResults.numEvents,
          reason.toString())
      for (request in requestsToExecute) {
        // Execute the request synchronously. Callbacks will take care of handling errors
        // and updating our final overall result.
        request.executeAndWait()
      }
      return flushResults
    }
    return null
  }

  @JvmStatic
  fun buildRequests(
      appEventCollection: AppEventCollection,
      flushResults: FlushStatistics
  ): List<GraphRequest> {
    val context = FacebookSdk.getApplicationContext()
    val limitEventUsage = FacebookSdk.getLimitEventAndDataUsage(context)
    val requestsToExecute = ArrayList<GraphRequest>()
    for (accessTokenAppId in appEventCollection.keySet()) {
      val request =
          buildRequestForSession(
              accessTokenAppId, appEventCollection[accessTokenAppId], limitEventUsage, flushResults)
      if (request != null) {
        requestsToExecute.add(request)
      }
    }
    return requestsToExecute
  }

  @JvmStatic
  fun buildRequestForSession(
      accessTokenAppId: AccessTokenAppIdPair,
      appEvents: SessionEventsState,
      limitEventUsage: Boolean,
      flushState: FlushStatistics
  ): GraphRequest? {
    val applicationId = accessTokenAppId.applicationId
    val fetchedAppSettings = queryAppSettings(applicationId, false)
    val postRequest =
        newPostRequest(null, String.format("%s/activities", applicationId), null, null)
    var requestParameters = postRequest.parameters
    if (requestParameters == null) {
      requestParameters = Bundle()
    }
    requestParameters.putString("access_token", accessTokenAppId.accessTokenString)
    val pushNotificationsRegistrationId = getPushNotificationsRegistrationId()
    if (pushNotificationsRegistrationId != null) {
      requestParameters.putString("device_token", pushNotificationsRegistrationId)
    }
    val installReferrer = AppEventsLoggerImpl.getInstallReferrer()
    if (installReferrer != null) {
      requestParameters.putString("install_referrer", installReferrer)
    }
    postRequest.parameters = requestParameters
    var supportsImplicitLogging = false
    if (fetchedAppSettings != null) {
      supportsImplicitLogging = fetchedAppSettings.supportsImplicitLogging()
    }
    val numEvents =
        appEvents.populateRequest(
            postRequest,
            FacebookSdk.getApplicationContext(),
            supportsImplicitLogging,
            limitEventUsage)
    if (numEvents == 0) {
      return null
    }
    flushState.numEvents += numEvents
    postRequest.callback =
        GraphRequest.Callback { response ->
          handleResponse(accessTokenAppId, postRequest, response, appEvents, flushState)
        }
    return postRequest
  }

  @JvmStatic
  fun handleResponse(
      accessTokenAppId: AccessTokenAppIdPair,
      request: GraphRequest,
      response: GraphResponse,
      appEvents: SessionEventsState,
      flushState: FlushStatistics
  ) {
    val error = response.error
    var resultDescription = "Success"
    var flushResult = FlushResult.SUCCESS
    if (error != null) {
      if (error.errorCode == NO_CONNECTIVITY_ERROR_CODE) {
        resultDescription = "Failed: No Connectivity"
        flushResult = FlushResult.NO_CONNECTIVITY
      } else {
        resultDescription =
            String.format(
                "Failed:\n  Response: %s\n  Error %s", response.toString(), error.toString())
        flushResult = FlushResult.SERVER_ERROR
      }
    }
    if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.APP_EVENTS)) {
      val eventsJsonString = request.tag as String?
      val prettyPrintedEvents: String
      prettyPrintedEvents =
          try {
            val jsonArray = JSONArray(eventsJsonString)
            jsonArray.toString(2)
          } catch (exc: JSONException) {
            "<Can't encode events for debug logging>"
          }
      log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "Flush completed\nParams: %s\n  Result: %s\n  Events JSON: %s",
          request.graphObject.toString(),
          resultDescription,
          prettyPrintedEvents)
    }
    appEvents.clearInFlightAndStats(error != null)
    if (flushResult === FlushResult.NO_CONNECTIVITY) {
      // We may call this for multiple requests in a batch, which is slightly inefficient
      // since in principle we could call it once for all failed requests, but the impact is
      // likely to be minimal. We don't call this for other server errors, because if an event
      // failed because it was malformed, etc., continually retrying it will cause subsequent
      // events to not be logged either.
      FacebookSdk.getExecutor().execute { persistEvents(accessTokenAppId, appEvents) }
    }
    if (flushResult !== FlushResult.SUCCESS) {
      // We assume that connectivity issues are more significant to report than server issues.
      if (flushState.result !== FlushResult.NO_CONNECTIVITY) {
        flushState.result = flushResult
      }
    }
  }
}
