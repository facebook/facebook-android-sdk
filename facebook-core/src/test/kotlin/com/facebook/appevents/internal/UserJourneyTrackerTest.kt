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
import android.content.Context
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLoggerImpl
import com.facebook.internal.FeatureManager
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FeatureManager::class, UserJourneyTracker::class)
class UserJourneyTrackerTest : FacebookPowerMockTestCase() {
  private lateinit var mockActivity: Activity
  private lateinit var mockLogger: AppEventsLoggerImpl
  private var metadataBasicEnabled = true
  private var checkFeatureCalls = 0

  @Before
  fun init() {
    mockActivity = mock()
    whenever(mockActivity.title).thenReturn("Home")
    mockLogger = mock()
    metadataBasicEnabled = true
    checkFeatureCalls = 0

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mock<Context>())
    PowerMockito.mockStatic(FeatureManager::class.java)
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.MetadataBasic)).thenAnswer {
      metadataBasicEnabled
    }
    whenever(FeatureManager.checkFeature(eq(FeatureManager.Feature.MetadataBasic), any())).then {
      checkFeatureCalls++
      (it.arguments[1] as FeatureManager.Callback).onCompleted(metadataBasicEnabled)
      Unit
    }
    PowerMockito.whenNew(AppEventsLoggerImpl::class.java).withAnyArguments().thenReturn(mockLogger)

    UserJourneyTracker.clear()
    Whitebox.setInternalState(UserJourneyTracker::class.java, "tracking", AtomicBoolean(false))
  }

  @Test
  fun `startTracking registers lifecycle callbacks only once`() {
    val application = mock<Application>()

    UserJourneyTracker.startTracking(application)
    UserJourneyTracker.startTracking(application)

    verify(application, times(1)).registerActivityLifecycleCallbacks(any())
  }

  @Test
  fun `getCurrentScreenTitle returns null before any activity is resumed`() {
    assertNull(UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `resuming an activity captures its title and logs an implicit screen view`() {
    UserJourneyTracker.onActivityResumed(mockActivity)

    assertEquals("Home", UserJourneyTracker.getCurrentScreenTitle())
    verify(mockLogger, times(1))
        .logEventImplicitly(
            eq(Constants.EVENT_NAME_SCREEN_VIEW), isNull<Double>(), isNull<Bundle>())
    verifyNoMoreInteractions(mockLogger)
  }

  @Test
  fun `logEvent does not check the gatekeeper when metadata collection is disabled`() {
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(false)

    UserJourneyTracker.logEvent(Constants.EVENT_NAME_SCREEN_VIEW, null)

    assertEquals(0, checkFeatureCalls)
    verifyZeroInteractions(mockLogger)
  }

  @Test
  fun `empty title is captured as null`() {
    whenever(mockActivity.title).thenReturn("")

    UserJourneyTracker.onActivityResumed(mockActivity)

    assertNull(UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `resuming the same activity with the same title does not log again`() {
    UserJourneyTracker.onActivityResumed(mockActivity)
    UserJourneyTracker.onActivityResumed(mockActivity)

    verify(mockLogger, times(1))
        .logEventImplicitly(
            eq(Constants.EVENT_NAME_SCREEN_VIEW), isNull<Double>(), anyOrNull<Bundle>())
  }

  @Test
  fun `title change on the same activity logs a new screen view`() {
    UserJourneyTracker.onActivityResumed(mockActivity)
    whenever(mockActivity.title).thenReturn("Cart")

    UserJourneyTracker.onActivityResumed(mockActivity)

    assertEquals("Cart", UserJourneyTracker.getCurrentScreenTitle())
    verify(mockLogger, times(2))
        .logEventImplicitly(
            eq(Constants.EVENT_NAME_SCREEN_VIEW), isNull<Double>(), anyOrNull<Bundle>())
  }

  @Test
  fun `switching activities logs a screen view for each`() {
    val activityB: Activity = mock()
    whenever(activityB.title).thenReturn("Checkout")

    UserJourneyTracker.onActivityResumed(mockActivity)
    UserJourneyTracker.onActivityResumed(activityB)

    assertEquals("Checkout", UserJourneyTracker.getCurrentScreenTitle())
    verify(mockLogger, times(2))
        .logEventImplicitly(
            eq(Constants.EVENT_NAME_SCREEN_VIEW), isNull<Double>(), anyOrNull<Bundle>())
  }

  @Test
  fun `nothing is captured or logged when metadata collection flag is disabled`() {
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(false)

    UserJourneyTracker.onActivityResumed(mockActivity)

    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(true)
    assertNull(UserJourneyTracker.getCurrentScreenTitle())
    verifyZeroInteractions(mockLogger)
  }

  @Test
  fun `disabling metadata collection clears a previously captured title`() {
    UserJourneyTracker.onActivityResumed(mockActivity)
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(false)

    UserJourneyTracker.onActivityResumed(mockActivity)
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(true)

    assertNull(UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `screen view is not logged when MetadataBasic is disabled`() {
    metadataBasicEnabled = false

    UserJourneyTracker.onActivityResumed(mockActivity)

    verifyZeroInteractions(mockLogger)
  }

  @Test
  fun `captured title becomes available once MetadataBasic is enabled`() {
    metadataBasicEnabled = false
    UserJourneyTracker.onActivityResumed(mockActivity)
    assertNull(UserJourneyTracker.getCurrentScreenTitle())

    metadataBasicEnabled = true

    assertEquals("Home", UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `screen view is logged even when AutoLogAppEvents is disabled`() {
    whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)

    UserJourneyTracker.onActivityResumed(mockActivity)

    verify(mockLogger, times(1))
        .logEventImplicitly(
            eq(Constants.EVENT_NAME_SCREEN_VIEW), isNull<Double>(), isNull<Bundle>())
  }

  @Test
  fun `destroying the current activity clears its title`() {
    UserJourneyTracker.onActivityResumed(mockActivity)

    UserJourneyTracker.onActivityDestroyed(mockActivity)

    assertNull(UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `destroying a previous activity does not clear the current title`() {
    val activityB: Activity = mock()
    whenever(activityB.title).thenReturn("Checkout")
    UserJourneyTracker.onActivityResumed(mockActivity)
    UserJourneyTracker.onActivityResumed(activityB)

    UserJourneyTracker.onActivityDestroyed(mockActivity)

    assertEquals("Checkout", UserJourneyTracker.getCurrentScreenTitle())
  }
}
