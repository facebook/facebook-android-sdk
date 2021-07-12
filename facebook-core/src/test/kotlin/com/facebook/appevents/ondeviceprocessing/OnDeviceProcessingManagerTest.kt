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
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager.isOnDeviceProcessingEnabled
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager.sendCustomEventAsync
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager.sendInstallEventAsync
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.ServiceResult
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.isServiceAvailable
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.sendCustomEvents
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper.sendInstallEvent
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, RemoteServiceWrapper::class)
class OnDeviceProcessingManagerTest : FacebookPowerMockTestCase() {
  private val applicationId = "app_id"
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(context)
    PowerMockito.`when`(FacebookSdk.getExecutor()).thenCallRealMethod()
    PowerMockito.mockStatic(RemoteServiceWrapper::class.java)
  }

  @Test
  fun testIsOnDeviceProcessingEnabled() {
    setupPreconditions(true, true)
    assertThat(isOnDeviceProcessingEnabled()).isTrue()
    setupPreconditions(false, true)
    assertThat(isOnDeviceProcessingEnabled()).isFalse()
    setupPreconditions(true, false)
    assertThat(isOnDeviceProcessingEnabled()).isFalse()
    setupPreconditions(false, false)
    assertThat(isOnDeviceProcessingEnabled()).isFalse()
  }

  @Test
  fun testSendCustomEventAsync_AllowedEvents() {
    // Arrange
    val latch = CountDownLatch(4)
    val captor = setupSendCustomEventsArgumentCaptor(latch)

    // Act
    sendCustomEventAsync(applicationId, createEvent("explicit_event", false))
    sendCustomEventAsync(applicationId, createEvent(AppEventsConstants.EVENT_NAME_PURCHASED, true))
    sendCustomEventAsync(applicationId, createEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, true))
    sendCustomEventAsync(
        applicationId, createEvent(AppEventsConstants.EVENT_NAME_START_TRIAL, true))
    latch.await(6, TimeUnit.SECONDS)
    // Assert : RemoteServiceWrapper.sendCustomEvents(...) was invoked 4 times
    assertThat(captor.allValues.size).isEqualTo(4)
  }

  @Test
  fun testSendCustomEventAsync_NotAllowedEvents() {
    // Arrange
    val latch = CountDownLatch(1)
    val captor = setupSendCustomEventsArgumentCaptor(latch)

    // Act
    sendCustomEventAsync(applicationId, createEvent("other_implicit_event", true))
    latch.await(1, TimeUnit.SECONDS)

    // Assert : RemoteServiceWrapper.sendCustomEvents(...) never invoked
    assertThat(captor.allValues.size).isEqualTo(0)
  }

  @Test
  fun testSendInstallEventAsync_NonNullArguments() {
    // Arrange
    val latch = CountDownLatch(1)
    val captor = setupSendInstallEventArgumentCaptor(latch)

    // Act
    sendInstallEventAsync(applicationId, "preferences_name")
    latch.await(7, TimeUnit.SECONDS)

    // Assert : RemoteServiceWrapper.sendInstallEvent(...) invoked once
    assertThat(captor.allValues.size).isEqualTo(1)
  }

  @Test
  fun testSendInstallEventAsync_NullArguments() {
    // Arrange
    val latch = CountDownLatch(1)
    val captor = setupSendInstallEventArgumentCaptor(latch)

    // Act
    sendInstallEventAsync(null, null)
    sendInstallEventAsync(null, "preferences_name")
    sendInstallEventAsync(applicationId, null)
    val completed = latch.await(3, TimeUnit.SECONDS)

    // Assert : RemoteServiceWrapper.sendInstallEvent(...) never invoked
    assertThat(captor.allValues.size).isEqualTo(0)
    Assert.assertFalse(completed)
  }

  private fun createEvent(eventName: String, isImplicitlyLogged: Boolean): AppEvent {
    return AppEvent("context_name", eventName, 0.0, Bundle(), isImplicitlyLogged, false, null)
  }

  private fun setupPreconditions(
      isApplicationTrackingEnabled: Boolean,
      isServiceAvailable: Boolean
  ) {
    PowerMockito.`when`(FacebookSdk.getLimitEventAndDataUsage(context))
        .thenReturn(!isApplicationTrackingEnabled)
    PowerMockito.`when`(isServiceAvailable()).thenReturn(isServiceAvailable)
  }

  private fun setupSendCustomEventsArgumentCaptor(
      latch: CountDownLatch
  ): KArgumentCaptor<List<AppEvent>> {
    val captor = argumentCaptor<List<AppEvent>>()
    PowerMockito.`when`(sendCustomEvents(any(), captor.capture())).thenAnswer {
      latch.countDown()
      ServiceResult.OPERATION_SUCCESS
    }
    return captor
  }

  private fun setupSendInstallEventArgumentCaptor(latch: CountDownLatch): KArgumentCaptor<String> {
    val captor = argumentCaptor<String>()
    PowerMockito.`when`(sendInstallEvent(captor.capture())).thenAnswer {
      latch.countDown()
      ServiceResult.OPERATION_SUCCESS
    }
    return captor
  }
}
