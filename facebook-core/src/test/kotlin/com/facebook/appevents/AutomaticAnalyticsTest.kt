/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.app.Activity
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.appevents.internal.ActivityLifecycleTracker
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FeatureManager
import com.facebook.internal.FetchedAppGateKeepersManager
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.FetchedAppSettingsManager.parseAppSettingsFromJSON
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
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
import org.powermock.reflect.internal.WhiteboxImpl
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment

@PrepareForTest(
    AppEventQueue::class,
    AppEventDiskStore::class,
    AttributionIdentifiers::class,
    ActivityLifecycleTracker::class,
    FacebookSdk::class,
    FeatureManager::class,
    FetchedAppSettingsManager::class,
    FetchedAppGateKeepersManager::class,
    Executors::class,
    GraphRequest::class,
    AttributionIdentifiers.Companion::class)
class AutomaticAnalyticsTest : FacebookPowerMockTestCase() {

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("1234")
    whenever(FacebookSdk.getApplicationContext()).thenReturn(RuntimeEnvironment.application)

    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
  }

  @Test
  fun testAutomaticLoggingEnabledServerConfiguration() {
    val settingsJSON = JSONObject()
    settingsJSON.put("app_events_feature_bitmask", "0")
    var settings = parseAppSettingsFromJSON("123", settingsJSON)
    assertThat(settings.automaticLoggingEnabled).isFalse
    settingsJSON.put("app_events_feature_bitmask", "7")
    settings = parseAppSettingsFromJSON("123", settingsJSON)
    assertThat(settings.automaticLoggingEnabled).isFalse
    settingsJSON.put("app_events_feature_bitmask", "23")
    settings = parseAppSettingsFromJSON("123", settingsJSON)
    assertThat(settings.automaticLoggingEnabled).isFalse
    settingsJSON.put("app_events_feature_bitmask", "8")
    settings = parseAppSettingsFromJSON("123", settingsJSON)
    assertThat(settings.automaticLoggingEnabled).isTrue
    settingsJSON.put("app_events_feature_bitmask", "9")
    settings = parseAppSettingsFromJSON("123", settingsJSON)
    assertThat(settings.automaticLoggingEnabled).isTrue
    val noBitmaskFieldSettings = JSONObject()
    settings = parseAppSettingsFromJSON("123", noBitmaskFieldSettings)
    assertThat(settings.automaticLoggingEnabled).isFalse
  }

  @Test
  fun testAutoTrackingWhenInitialized() {
    val mockExecutor: ScheduledExecutorService = FacebookSerialThreadPoolExecutor(1)
    Whitebox.setInternalState(
        ActivityLifecycleTracker::class.java, "singleThreadExecutor", mockExecutor)
    PowerMockito.mockStatic(ActivityLifecycleTracker::class.java)
    val activity =
        Robolectric.buildActivity(Activity::class.java).create().start().resume().visible().get()
    whenever(ActivityLifecycleTracker.onActivityCreated(activity)).thenCallRealMethod()
  }

  @Test
  fun testLogAndSendAppEvent() {
    val mockExecutor: ScheduledExecutorService = FacebookSerialThreadPoolExecutor(1)
    Whitebox.setInternalState(AppEventQueue::class.java, "singleThreadExecutor", mockExecutor)
    // Mock App Settings to avoid App Setting request

    // Disable Gatekeeper
    PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
    whenever(FetchedAppGateKeepersManager.getGateKeeperForKey(any(), any(), any()))
        .thenReturn(false)

    // Mock FeatureManger to avoid GK request
    PowerMockito.mockStatic(FeatureManager::class.java)

    // Stub mock IDs for AttributionIdentifiers
    val mockAdvertiserID = "fb_mock_advertiserID"
    val mockAttributionID = "fb_mock_attributionID"
    val mockIdentifiers: AttributionIdentifiers = mock()
    whenever(mockIdentifiers.androidAdvertiserId).thenReturn(mockAdvertiserID)
    whenever(mockIdentifiers.attributionId).thenReturn(mockAttributionID)
    val mockAttributionIdentifierCompanion =
        PowerMockito.mock(AttributionIdentifiers.Companion::class.java)
    WhiteboxImpl.setInternalState(
        AttributionIdentifiers::class.java, "Companion", mockAttributionIdentifierCompanion)
    whenever(mockAttributionIdentifierCompanion.getAttributionIdentifiers(any()))
        .thenReturn(mockIdentifiers)

    // Mock App Event Store
    PowerMockito.mockStatic(AppEventDiskStore::class.java)
    val accessTokenAppIdPair = AccessTokenAppIdPair(null, "1234")
    val appEvent = AppEvent("ctxName", "eventName2", 0.0, Bundle(), true, true, null)
    val map = hashMapOf(accessTokenAppIdPair to mutableListOf(appEvent))
    val persistedEvents: PersistedEvents = PersistedEvents(map)
    whenever(AppEventDiskStore.readAndClearStore()).thenReturn(persistedEvents)

    // Mock graph request
    val mockRequest: GraphRequest = mock()
    PowerMockito.whenNew(GraphRequest::class.java).withAnyArguments().thenReturn(mockRequest)
    PowerMockito.spy(AppEventQueue::class.java)
    PowerMockito.doReturn(mockRequest)
        .`when`(
            AppEventQueue::class.java,
            "buildRequestForSession",
            any<AccessTokenAppIdPair>(),
            any<SessionEventsState>(),
            any<Boolean>(),
            any<FlushStatistics>())
    val loggerImpl = AppEventsLoggerImpl(RuntimeEnvironment.application, "1234", null)
    loggerImpl.logEvent("fb_mock_event", 1.0, Bundle(), true, null)
    loggerImpl.flush()
    Thread.sleep(200)
    verify(mockRequest).executeAndWait()
  }
}
