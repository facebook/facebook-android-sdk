/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved. <p> You are hereby granted a
 * non-exclusive, worldwide, royalty-free license to use, copy, modify, and distribute this software
 * in source code or binary form for use in connection with the web services and APIs provided by
 * Facebook. <p> As with any software that integrates with the Facebook platform, your use of this
 * software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY
 * OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
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
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito.*
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
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
  private lateinit var mockCodelessSessionChecker: CodelessManager.CodelessSessionChecker
  private lateinit var mockExecutor: FacebookSerialExecutor
  private var appId = "123456"

  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    mockExecutor = FacebookSerialExecutor()
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    whenCalled(FacebookSdk.getApplicationId()).thenReturn(appId)
    whenCalled(FacebookSdk.getExecutor()).thenReturn(mockExecutor)

    mockAppSettings = mock(FetchedAppSettings::class.java)
    mockStatic(FetchedAppSettingsManager::class.java)
    whenCalled(FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId))
        .thenReturn(mockAppSettings)

    mockSensorManager = mock(SensorManager::class.java)
    mockAccelerometer = mock(Sensor::class.java)
    whenCalled(mockSensorManager.getDefaultSensor(eq(Sensor.TYPE_ACCELEROMETER)))
        .thenReturn(mockAccelerometer)

    mockApplicationContext = mock(Context::class.java)
    whenCalled(mockApplicationContext.getSystemService(eq(Context.SENSOR_SERVICE)))
        .thenReturn(mockSensorManager)

    mockActivity = mock(Activity::class.java)
    whenCalled(mockActivity.applicationContext).thenReturn(mockApplicationContext)

    mockStatic(CodelessManager::class.java)
    spy(CodelessManager::class.java)
    mockCodelessSessionChecker = mock(CodelessManager.CodelessSessionChecker::class.java)
    CodelessManager.setCodelessSessionChecker(mockCodelessSessionChecker)
    ReflectionHelpers.setStaticField(
        CodelessManager::class.java, "isCodelessEnabled", AtomicBoolean(true))
  }

  @Test
  fun `onActivityResumed when isCodelessEnabled is false`() {
    ReflectionHelpers.setStaticField(
        CodelessManager::class.java, "isCodelessEnabled", AtomicBoolean(false))
    whenCalled(CodelessManager.isDebugOnEmulator()).thenReturn(true)
    whenCalled(mockAppSettings.codelessEventsEnabled).thenReturn(true)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext, never()).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager, never())
        .registerListener(
            any(ViewIndexingTrigger::class.java),
            any(Sensor::class.java),
            eq(SensorManager.SENSOR_DELAY_UI))

    verify(mockCodelessSessionChecker, never()).checkCodelessSession(appId)
  }

  @Test
  fun `onActivityResumed when codeless events enabled, is debug on emulator`() {
    whenCalled(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    whenCalled(CodelessManager.isDebugOnEmulator()).thenReturn(true)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager)
        .registerListener(
            any(ViewIndexingTrigger::class.java),
            any(Sensor::class.java),
            eq(SensorManager.SENSOR_DELAY_UI))

    verify(mockCodelessSessionChecker).checkCodelessSession(appId)
  }

  @Test
  fun `onActivityResumed when codeless isnt events enabled, is debug on emulator`() {
    whenCalled(mockAppSettings.codelessEventsEnabled).thenReturn(false)
    whenCalled(CodelessManager.isDebugOnEmulator()).thenReturn(true)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager)
        .registerListener(
            any(ViewIndexingTrigger::class.java),
            any(Sensor::class.java),
            eq(SensorManager.SENSOR_DELAY_UI))

    verify(mockCodelessSessionChecker).checkCodelessSession(appId)
  }

  @Test
  fun `onActivityResumed when codeless events isnt enabled, isnt debug on emulator`() {
    whenCalled(mockAppSettings.codelessEventsEnabled).thenReturn(false)
    whenCalled(CodelessManager.isDebugOnEmulator()).thenReturn(false)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext, never()).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager, never())
        .registerListener(
            any(ViewIndexingTrigger::class.java),
            any(Sensor::class.java),
            eq(SensorManager.SENSOR_DELAY_UI))

    verify(mockCodelessSessionChecker, never()).checkCodelessSession(appId)
  }

  @Test
  fun `onActivityResumed when codeless events is enabled, isnt debug on emulator`() {
    whenCalled(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    whenCalled(CodelessManager.isDebugOnEmulator()).thenReturn(false)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager)
        .registerListener(
            any(ViewIndexingTrigger::class.java),
            any(Sensor::class.java),
            eq(SensorManager.SENSOR_DELAY_UI))

    verify(mockCodelessSessionChecker, never()).checkCodelessSession(appId)
  }
}
