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
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.eventdeactivation.EventDeactivationManager
import com.facebook.appevents.ondeviceprocessing.RemoteServiceParametersHelper.buildEventsBundle
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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
