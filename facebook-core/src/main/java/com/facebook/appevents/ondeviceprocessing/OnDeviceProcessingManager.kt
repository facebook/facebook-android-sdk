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
package com.facebook.appevents.ondeviceprocessing

import android.content.Context
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.isServiceAvailable
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.sendCustomEvents
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.sendInstallEvent
import com.facebook.internal.Utility.isDataProcessingRestricted
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object OnDeviceProcessingManager {
  private val ALLOWED_IMPLICIT_EVENTS: Set<String> =
      setOf(
          AppEventsConstants.EVENT_NAME_PURCHASED,
          AppEventsConstants.EVENT_NAME_START_TRIAL,
          AppEventsConstants.EVENT_NAME_SUBSCRIBE)

  @JvmStatic
  fun isOnDeviceProcessingEnabled(): Boolean {
    val context = FacebookSdk.getApplicationContext()
    val isApplicationTrackingEnabled =
        !FacebookSdk.getLimitEventAndDataUsage(context) && !isDataProcessingRestricted
    return isApplicationTrackingEnabled && isServiceAvailable()
  }

  @JvmStatic
  fun sendInstallEventAsync(applicationId: String?, preferencesName: String?) {
    val context = FacebookSdk.getApplicationContext()
    if (context != null && applicationId != null && preferencesName != null) {
      FacebookSdk.getExecutor().execute {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val pingKey = applicationId + "pingForOnDevice"
        var lastOnDevicePing = preferences.getLong(pingKey, 0)

        // Send install event only if have not sent before
        if (lastOnDevicePing == 0L) {
          sendInstallEvent(applicationId)

          // We denote success with any response from remote service as errors are not
          // recoverable
          val editor = preferences.edit()
          lastOnDevicePing = System.currentTimeMillis()
          editor.putLong(pingKey, lastOnDevicePing)
          editor.apply()
        }
      }
    }
  }

  @JvmStatic
  fun sendCustomEventAsync(applicationId: String, event: AppEvent) {
    if (isEventEligibleForOnDeviceProcessing(event)) {
      FacebookSdk.getExecutor().execute { sendCustomEvents(applicationId, listOf(event)) }
    }
  }

  private fun isEventEligibleForOnDeviceProcessing(event: AppEvent): Boolean {
    val isAllowedImplicitEvent = event.isImplicit && ALLOWED_IMPLICIT_EVENTS.contains(event.name)
    val isExplicitEvent = !event.isImplicit
    return isExplicitEvent || isAllowedImplicitEvent
  }
}
