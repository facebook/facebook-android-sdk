/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
