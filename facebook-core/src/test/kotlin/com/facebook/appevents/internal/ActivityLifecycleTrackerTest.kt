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
import com.facebook.appevents.aam.MetadataIndexer
import com.facebook.appevents.codeless.CodelessManager
import com.facebook.appevents.suggestedevents.SuggestedEventsManager
import com.facebook.internal.FeatureManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FeatureManager::class,
    CodelessManager::class,
    MetadataIndexer::class,
    SuggestedEventsManager::class)
class ActivityLifecycleTrackerTest : FacebookPowerMockTestCase() {

  private lateinit var mockApplication: Application
  private lateinit var mockActivity: Activity
  private lateinit var mockScheduledExecutor: FacebookSerialThreadPoolMockExecutor

  private val appID = "123"

  @Before
  fun init() {
    mockApplication = PowerMockito.mock(Application::class.java)
    mockActivity = PowerMockito.mock(Activity::class.java)
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.mockStatic(CodelessManager::class.java)
    PowerMockito.mockStatic(MetadataIndexer::class.java)
    PowerMockito.mockStatic(SuggestedEventsManager::class.java)

    mockScheduledExecutor = spy(FacebookSerialThreadPoolMockExecutor(1))
    Whitebox.setInternalState(
        ActivityLifecycleTracker::class.java, "singleThreadExecutor", mockScheduledExecutor)
  }

  @Test
  fun `test start tracking`() {
    ActivityLifecycleTracker.startTracking(mockApplication, appID)
    verify(mockApplication, times(1)).registerActivityLifecycleCallbacks(any())
  }

  @Test
  fun `test create activity`() {
    ActivityLifecycleTracker.onActivityCreated(mockActivity)
    verify(mockScheduledExecutor).execute(any<Runnable>())
  }

  @Test
  fun `test resume activity`() {
    var codelessManagerCounter = 0
    var metadataIndexerCounter = 0
    var suggestedEventsManagerCounter = 0

    whenever(CodelessManager.onActivityResumed(eq(mockActivity))).thenAnswer {
      codelessManagerCounter++
    }
    whenever(MetadataIndexer.onActivityResumed(eq(mockActivity))).thenAnswer {
      metadataIndexerCounter++
    }
    whenever(SuggestedEventsManager.trackActivity(eq(mockActivity))).thenAnswer {
      suggestedEventsManagerCounter++
    }
    ActivityLifecycleTracker.onActivityResumed(mockActivity)
    assertEquals(1, codelessManagerCounter)
    assertEquals(1, metadataIndexerCounter)
    assertEquals(1, suggestedEventsManagerCounter)

    verify(mockScheduledExecutor).execute(any<Runnable>())

    assertEquals(mockActivity, ActivityLifecycleTracker.getCurrentActivity())
  }
}
