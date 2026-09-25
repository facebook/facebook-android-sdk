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
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FeatureManager
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FeatureManager::class)
class UserJourneyTrackerTest : FacebookPowerMockTestCase() {
  private lateinit var mockActivity: Activity

  @Before
  fun init() {
    mockActivity = mock()
    whenever(mockActivity.title).thenReturn("Home")

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(true)
    PowerMockito.mockStatic(FeatureManager::class.java)
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.MetadataBasic)).thenReturn(true)

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
  fun `resuming an activity captures its title`() {
    UserJourneyTracker.onActivityResumed(mockActivity)

    assertEquals("Home", UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `empty title is captured as null`() {
    whenever(mockActivity.title).thenReturn("")

    UserJourneyTracker.onActivityResumed(mockActivity)

    assertNull(UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `resuming the same activity picks up a changed title`() {
    UserJourneyTracker.onActivityResumed(mockActivity)
    whenever(mockActivity.title).thenReturn("Cart")

    UserJourneyTracker.onActivityResumed(mockActivity)

    assertEquals("Cart", UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `switching activities updates the title`() {
    val activityB: Activity = mock()
    whenever(activityB.title).thenReturn("Checkout")

    UserJourneyTracker.onActivityResumed(mockActivity)
    UserJourneyTracker.onActivityResumed(activityB)

    assertEquals("Checkout", UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `nothing is captured when metadata collection is disabled`() {
    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(false)

    UserJourneyTracker.onActivityResumed(mockActivity)

    whenever(FacebookSdk.getAutoLogMetaDataEnabled()).thenReturn(true)
    assertNull(UserJourneyTracker.getCurrentScreenTitle())
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
  fun `captured title becomes available once MetadataBasic is enabled`() {
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.MetadataBasic)).thenReturn(false)
    UserJourneyTracker.onActivityResumed(mockActivity)
    assertNull(UserJourneyTracker.getCurrentScreenTitle())

    whenever(FeatureManager.isEnabled(FeatureManager.Feature.MetadataBasic)).thenReturn(true)

    assertEquals("Home", UserJourneyTracker.getCurrentScreenTitle())
  }

  @Test
  fun `title is captured even when AutoLogAppEvents is disabled`() {
    whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)

    UserJourneyTracker.onActivityResumed(mockActivity)

    assertEquals("Home", UserJourneyTracker.getCurrentScreenTitle())
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
