/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.ondeviceprocessing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.eventdeactivation.EventDeactivationManager
import com.facebook.appevents.ondeviceprocessing.RemoteServiceParametersHelper.buildEventsBundle
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class)
class RemoteServiceParametersHelperTest : FacebookPowerMockTestCase() {
  private val applicationId = "mock_app_id"
  private val implicitEvent: AppEvent =
      AppEvent("context_name", "implicit_event", 0.0, null, true, false, null)
  private val explicitEvent: AppEvent =
      AppEvent("context_name", "explicit_event", 0.0, null, false, false, null)
  private val deprecatedEvent: AppEvent =
      AppEvent("context_name", "deprecated_event", 0.0, null, false, false, null)
  private val invalidChecksumEvent: AppEvent =
      AppEvent("context_name", "invalid_checksum_event", 0.0, null, false, false, null)
  private lateinit var context: Context

  @Before
  fun setUp() {
    Whitebox.setInternalState(invalidChecksumEvent, "checksum", "invalid_checksum")
    context = ApplicationProvider.getApplicationContext()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(context)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
  }

  @Test
  fun testBuildEventsBundle_FiltersEvents() {
    // Arrange
    Whitebox.setInternalState(EventDeactivationManager::class.java, "enabled", true)
    Whitebox.setInternalState(
        EventDeactivationManager::class.java,
        "deprecatedEvents",
        HashSet(listOf(deprecatedEvent.name)))
    val mockAppSettings: FetchedAppSettings = mock()
    whenever(mockAppSettings.supportsImplicitLogging()).thenReturn(false)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    whenever(queryAppSettings(any(), any())).thenReturn(mockAppSettings)
    val appEvents = listOf(implicitEvent, deprecatedEvent, explicitEvent, invalidChecksumEvent)
    val eventType = RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS

    // Act
    val eventsBundle = buildEventsBundle(eventType, applicationId, appEvents)

    // Assert
    assertThat(eventsBundle?.getString("event")).isEqualTo(eventType.toString())
    assertThat(eventsBundle?.getString("app_id")).isEqualTo(applicationId)
    val expectedEventsJson = String.format("[%s]", explicitEvent.jsonObject.toString())
    assertThat(eventsBundle?.getString("custom_events")).isEqualTo(expectedEventsJson)
  }

  @Test
  fun testBuildEventsBundle_ReturnNull() {
    // Arrange
    val appEvents = listOf(implicitEvent)

    // Act
    val eventsBundle =
        buildEventsBundle(
            RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS, applicationId, appEvents)

    // Assert
    assertThat(eventsBundle).isNull()
  }
}
