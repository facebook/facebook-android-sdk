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
import com.facebook.appevents.iap.InAppPurchaseManager
import com.facebook.appevents.suggestedevents.SuggestedEventsManager
import com.facebook.internal.FeatureManager
import com.facebook.internal.Utility
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.internal.mockcreation.DefaultMockCreator.mock
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FeatureManager::class,
    CodelessManager::class,
    MetadataIndexer::class,
    SuggestedEventsManager::class,
    InAppPurchaseManager::class,
    Utility::class,
)
class ActivityLifecycleTrackerTest : FacebookPowerMockTestCase() {

    private lateinit var mockApplication: Application
    private lateinit var mockActivity: Activity
    private lateinit var mockScheduledExecutor: FacebookSerialThreadPoolMockExecutor
    private lateinit var mockIapExecutor: FacebookSerialThreadPoolMockExecutor

    private val appID = "123"

    @Before
    fun init() {
        mockApplication = PowerMockito.mock(Application::class.java)
        mockActivity = PowerMockito.mock(Activity::class.java)
        PowerMockito.mockStatic(FeatureManager::class.java)
        PowerMockito.mockStatic(CodelessManager::class.java)
        PowerMockito.mockStatic(InAppPurchaseManager::class.java)
        PowerMockito.mockStatic(MetadataIndexer::class.java)
        PowerMockito.mockStatic(SuggestedEventsManager::class.java)
        PowerMockito.mockStatic(Utility::class.java)
        whenever(Utility.getActivityName(eq(mockActivity))).thenAnswer { "ProxyBillingActivity" }

        mockScheduledExecutor = spy(FacebookSerialThreadPoolMockExecutor(1))
        Whitebox.setInternalState(
            ActivityLifecycleTracker::class.java, "singleThreadExecutor", mockScheduledExecutor
        )

        mockIapExecutor = mock()
        Whitebox.setInternalState(
            ActivityLifecycleTracker::class.java,
            "iapExecutor",
            mockIapExecutor
        )
        Whitebox.setInternalState(
            ActivityLifecycleTracker::class.java, "singleThreadExecutor", mockScheduledExecutor
        )
        Whitebox.setInternalState(
            ActivityLifecycleTracker::class.java, "previousActivityName", "MainActivity"
        )
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
        verify(mockScheduledExecutor, times(1)).execute(any<Runnable>())
        assertEquals(mockActivity, ActivityLifecycleTracker.getCurrentActivity())
    }

    @Test
    fun `test resume activity after in-app purchase`() {
        var startTrackingCount = 0
        var codelessManagerCounter = 0
        var metadataIndexerCounter = 0
        var suggestedEventsManagerCounter = 0

        whenever(mockIapExecutor.execute(any())).thenAnswer { startTrackingCount++ }
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
        assertEquals(0, startTrackingCount)
        verify(mockScheduledExecutor, times(1)).execute(any<Runnable>())

        assertEquals(mockActivity, ActivityLifecycleTracker.getCurrentActivity())

        // New activity, after IAP event
        whenever(Utility.getActivityName(eq(mockActivity))).thenAnswer { "NOTProxyBillingActivity" }
        ActivityLifecycleTracker.onActivityResumed(mockActivity)
        assertEquals(startTrackingCount, 1)
        verify(mockScheduledExecutor, times(2)).execute(any<Runnable>())
    }

    @Test
    fun `test resume activity after non in-app purchase`() {
        var startTrackingCount = 0
        var codelessManagerCounter = 0
        var metadataIndexerCounter = 0
        var suggestedEventsManagerCounter = 0
        whenever(Utility.getActivityName(eq(mockActivity))).thenAnswer { "MainActivity" }
        whenever(mockIapExecutor.execute(any())).thenAnswer { startTrackingCount++ }
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
        assertEquals(startTrackingCount, 0)
        verify(mockScheduledExecutor, times(1)).execute(any<Runnable>())

        assertEquals(mockActivity, ActivityLifecycleTracker.getCurrentActivity())

        // New activity, after IAP event
        ActivityLifecycleTracker.onActivityResumed(mockActivity)
        assertEquals(startTrackingCount, 0)
        verify(mockScheduledExecutor, times(2)).execute(any<Runnable>())

    }
}
