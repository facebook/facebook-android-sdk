/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLoggerImpl
import com.facebook.internal.FeatureManager
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Collects app metadata (the current screen title) and logs the UserJourney events. Collection is
 * independent of AutoLogAppEvents and is gated only by the AutoLogMetaDataEnabled flag
 * and the MetadataBasic feature.
 */
@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object UserJourneyTracker {
  private val tracking = AtomicBoolean(false)
  private val currentScreenTitle = AtomicReference<String?>()

  @Volatile private var currentActivity: WeakReference<Activity>? = null

  @JvmStatic
  fun startTracking(application: Application) {
    if (!tracking.compareAndSet(false, true)) {
      return
    }
    application.registerActivityLifecycleCallbacks(
        object : Application.ActivityLifecycleCallbacks {
          override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

          override fun onActivityStarted(activity: Activity) = Unit

          override fun onActivityResumed(activity: Activity) =
              UserJourneyTracker.onActivityResumed(activity)

          override fun onActivityPaused(activity: Activity) = Unit

          override fun onActivityStopped(activity: Activity) = Unit

          override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

          override fun onActivityDestroyed(activity: Activity) =
              UserJourneyTracker.onActivityDestroyed(activity)
        })
  }

  @JvmStatic
  fun isMetadataCollectionEnabled(): Boolean =
      FacebookSdk.getAutoLogMetaDataEnabled() &&
          FeatureManager.isEnabled(FeatureManager.Feature.MetadataBasic)

  @JvmStatic
  fun getCurrentScreenTitle(): String? =
      if (isMetadataCollectionEnabled()) currentScreenTitle.get() else null

  @JvmStatic
  fun clear() {
    currentActivity = null
    currentScreenTitle.set(null)
  }

  /**
   * Logs a UserJourney event as an implicit internal event. It goes through AppEventsLoggerImpl
   * directly because InternalAppEventsLogger is gated on AutoLogAppEvents. The feature check waits
   * for gatekeepers to load so events at app launch are not dropped.
   */
  @JvmStatic
  fun logEvent(eventName: String, parameters: Bundle?) {
    if (!FacebookSdk.getAutoLogMetaDataEnabled()) {
      return
    }
    FeatureManager.checkFeature(FeatureManager.Feature.MetadataBasic) { enabled ->
      if (enabled) {
        AppEventsLoggerImpl(FacebookSdk.getApplicationContext(), null, null)
            .logEventImplicitly(eventName, null, parameters)
      }
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun onActivityResumed(activity: Activity) {
    // The title is captured before the gatekeeper check so the first screen is not lost while
    // gatekeepers load; getCurrentScreenTitle gates whether it can be transmitted.
    if (!FacebookSdk.getAutoLogMetaDataEnabled()) {
      clear()
      return
    }
    val title = activity.title?.toString()?.takeIf { it.isNotEmpty() }
    val isNewScreen = currentActivity?.get() !== activity || currentScreenTitle.get() != title
    currentActivity = WeakReference(activity)
    currentScreenTitle.set(title)
    if (isNewScreen) {
      logEvent(Constants.EVENT_NAME_SCREEN_VIEW, null)
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun onActivityDestroyed(activity: Activity) {
    if (currentActivity?.get() === activity) {
      clear()
    }
  }
}
