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

package com.facebook.appevents.codeless

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.util.ReflectionHelpers

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class, CodelessManager::class)
class CodelessManagerTest : FacebookPowerMockTestCase() {
  private lateinit var mockAppSettings: FetchedAppSettings
  private lateinit var mockApplicationContext: Context
  private lateinit var mockActivity: Activity
  private lateinit var mockAccelerometer: Sensor
  private lateinit var mockSensorManager: SensorManager
  private lateinit var mockExecutor: FacebookSerialExecutor
  private var appId = "123456"
  private var checkCodelessSessionTimes = 0

  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    mockExecutor = FacebookSerialExecutor()
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    whenever(FacebookSdk.getApplicationId()).thenReturn(appId)
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)

    mockAppSettings = mock()
    mockStatic(FetchedAppSettingsManager::class.java)
    whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId))
        .thenReturn(mockAppSettings)

    mockSensorManager = mock()
    mockAccelerometer = mock()
    whenever(mockSensorManager.getDefaultSensor(eq(Sensor.TYPE_ACCELEROMETER)))
        .thenReturn(mockAccelerometer)

    mockApplicationContext = mock()
    whenever(mockApplicationContext.getSystemService(eq(Context.SENSOR_SERVICE)))
        .thenReturn(mockSensorManager)

    mockActivity = mock()
    whenever(mockActivity.applicationContext).thenReturn(mockApplicationContext)

    PowerMockito.spy(CodelessManager::class.java)
    ReflectionHelpers.setStaticField(
        CodelessManager::class.java, "isCodelessEnabled", AtomicBoolean(true))
    ReflectionHelpers.setStaticField(CodelessManager::class.java, "isCheckingSession", true)
    whenever(CodelessManager.checkCodelessSession(appId)).then { checkCodelessSessionTimes++ }
  }

  @Test
  fun `onActivityResumed when isCodelessEnabled is false`() {
    ReflectionHelpers.setStaticField(
        CodelessManager::class.java, "isCodelessEnabled", AtomicBoolean(false))
    whenever(CodelessManager.isDebugOnEmulator()).thenReturn(true)
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(true)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext, never()).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager, never())
        .registerListener(any<ViewIndexingTrigger>(), any(), eq(SensorManager.SENSOR_DELAY_UI))

    assertThat(checkCodelessSessionTimes).isEqualTo(0)
  }

  @Test
  fun `onActivityResumed when codeless events enabled, is debug on emulator`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    whenever(CodelessManager.isDebugOnEmulator()).thenReturn(true)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager)
        .registerListener(any<ViewIndexingTrigger>(), any(), eq(SensorManager.SENSOR_DELAY_UI))

    assertThat(checkCodelessSessionTimes).isEqualTo(1)
  }

  @Test
  fun `onActivityResumed when codeless isnt events enabled, is debug on emulator`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(false)
    whenever(CodelessManager.isDebugOnEmulator()).thenReturn(true)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager)
        .registerListener(any<ViewIndexingTrigger>(), any(), eq(SensorManager.SENSOR_DELAY_UI))

    assertThat(checkCodelessSessionTimes).isEqualTo(1)
  }

  @Test
  fun `onActivityResumed when codeless events isnt enabled, isnt debug on emulator`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(false)
    whenever(CodelessManager.isDebugOnEmulator()).thenReturn(false)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext, never()).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager, never())
        .registerListener(any<ViewIndexingTrigger>(), any(), eq(SensorManager.SENSOR_DELAY_UI))

    assertThat(checkCodelessSessionTimes).isEqualTo(0)
  }

  @Test
  fun `onActivityResumed when codeless events is enabled, isnt debug on emulator`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    whenever(CodelessManager.isDebugOnEmulator()).thenReturn(false)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager)
        .registerListener(any<ViewIndexingTrigger>(), any(), eq(SensorManager.SENSOR_DELAY_UI))

    assertThat(checkCodelessSessionTimes).isEqualTo(0)
  }
}
