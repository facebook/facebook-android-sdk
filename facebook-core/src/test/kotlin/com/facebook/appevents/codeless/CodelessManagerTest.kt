/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.appevents.codeless.internal.Constants
import com.facebook.appevents.internal.AppEventUtility
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class, AppEventUtility::class)
class CodelessManagerTest : FacebookPowerMockTestCase() {
  private lateinit var mockAppSettings: FetchedAppSettings
  private lateinit var mockApplicationContext: Context
  private lateinit var mockActivity: Activity
  private lateinit var mockAccelerometer: Sensor
  private lateinit var mockSensorManager: SensorManager
  private lateinit var mockExecutor: FacebookSerialExecutor
  private lateinit var mockCodelessMatcher: CodelessMatcher
  private lateinit var viewIndexingTriggerCaptor: KArgumentCaptor<ViewIndexingTrigger>
  private lateinit var mockGraphRequestCompanion: GraphRequest.Companion
  private lateinit var mockPostRequest: GraphRequest
  private lateinit var postRequestGraphPathCaptor: KArgumentCaptor<String>
  private lateinit var bigShakeEvent: SensorEvent

  private var appId = "123456"

  override fun setup() {
    super.setup()
    // preparing sdk configuration mocks
    mockStatic(FacebookSdk::class.java)
    mockExecutor = FacebookSerialExecutor()
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(appId)
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    whenever(FacebookSdk.getCodelessSetupEnabled()).thenReturn(true)

    mockAppSettings = mock()
    mockStatic(FetchedAppSettingsManager::class.java)
    whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId))
        .thenReturn(mockAppSettings)

    mockStatic(AppEventUtility::class.java)
    whenever(AppEventUtility.isEmulator()).thenReturn(true)
    Whitebox.setInternalState(CodelessManager::class.java, "isCheckingSession", false)

    val mockAttributionIdentifiersCompanion = mock<AttributionIdentifiers.Companion>()
    whenever(mockAttributionIdentifiersCompanion.getAttributionIdentifiers(anyOrNull()))
        .thenReturn(null)
    Whitebox.setInternalState(
        AttributionIdentifiers::class.java, "Companion", mockAttributionIdentifiersCompanion)

    // preparing sensor mocks
    mockSensorManager = mock()
    mockAccelerometer = mock()
    whenever(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))
        .thenReturn(mockAccelerometer)
    viewIndexingTriggerCaptor = argumentCaptor()
    whenever(
            mockSensorManager.registerListener(
                viewIndexingTriggerCaptor.capture(), eq(mockAccelerometer), anyOrNull()))
        .thenReturn(true)
    bigShakeEvent = mock()
    Whitebox.setInternalState(bigShakeEvent, "values", floatArrayOf(999.0f, 999.0f, 999.0f))

    mockApplicationContext = mock()
    whenever(mockApplicationContext.getSystemService(Context.SENSOR_SERVICE))
        .thenReturn(mockSensorManager)

    mockActivity = mock()
    whenever(mockActivity.applicationContext).thenReturn(mockApplicationContext)

    // preparing graph request mocks
    mockGraphRequestCompanion = mock()
    mockPostRequest = mock()
    postRequestGraphPathCaptor = argumentCaptor()
    whenever(
            mockGraphRequestCompanion.newPostRequestWithBundle(
                anyOrNull(), postRequestGraphPathCaptor.capture(), anyOrNull(), anyOrNull()))
        .thenReturn(mockPostRequest)
    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    whenever(mockPostRequest.executeAndWait()).thenReturn(mock())

    // preparing codeless matcher mock
    mockCodelessMatcher = mock()
    val mockCodelessMatcherCompanion = mock<CodelessMatcher.Companion>()
    whenever(mockCodelessMatcherCompanion.getInstance()).thenReturn(mockCodelessMatcher)
    Whitebox.setInternalState(
        CodelessMatcher::class.java, "Companion", mockCodelessMatcherCompanion)
  }

  @Test
  fun `test when codeless is disabled no listener is registered with sensor manager`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    CodelessManager.disable()

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext, never()).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager, never())
        .registerListener(any<ViewIndexingTrigger>(), any(), eq(SensorManager.SENSOR_DELAY_UI))
    verify(mockCodelessMatcher, never()).add(mockActivity)
  }

  @Test
  fun `test when codeless events enabled a shake will trigger checking codeless session`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    CodelessManager.enable()
    val mockResponse = mock<GraphResponse>()
    val responseData = JSONObject()
    responseData.put(Constants.APP_INDEXING_ENABLED, true)
    whenever(mockResponse.getJSONObject()).thenReturn(responseData)
    whenever(mockResponse.jsonObject).thenReturn(responseData)
    whenever(mockPostRequest.executeAndWait()).thenReturn(mockResponse)

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockApplicationContext).getSystemService(Context.SENSOR_SERVICE)
    verify(mockSensorManager)
        .registerListener(
            any<ViewIndexingTrigger>(), eq(mockAccelerometer), eq(SensorManager.SENSOR_DELAY_UI))
    verify(mockCodelessMatcher).add(mockActivity)

    val capturedViewIndexingTrigger = viewIndexingTriggerCaptor.firstValue
    capturedViewIndexingTrigger.onSensorChanged(bigShakeEvent)

    verify(mockGraphRequestCompanion, atLeastOnce())
        .newPostRequestWithBundle(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
    assertThat(postRequestGraphPathCaptor.firstValue).isEqualTo("$appId/app_indexing_session")
    verify(mockPostRequest, atLeastOnce()).executeAndWait()
    assertThat(CodelessManager.getIsAppIndexingEnabled()).isTrue
  }

  @Test
  fun `test activity will be destroyed from matcher when it is destroyed`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    CodelessManager.enable()

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockCodelessMatcher, atLeastOnce()).add(mockActivity)

    CodelessManager.onActivityDestroyed(mockActivity)
    verify(mockCodelessMatcher, atLeastOnce()).destroy(mockActivity)
  }

  @Test
  fun `test activity will be removed from matcher when it pauses`() {
    whenever(mockAppSettings.codelessEventsEnabled).thenReturn(true)
    CodelessManager.enable()

    CodelessManager.onActivityResumed(mockActivity)
    verify(mockCodelessMatcher, atLeastOnce()).add(mockActivity)

    CodelessManager.onActivityPaused(mockActivity)
    verify(mockCodelessMatcher).remove(mockActivity)
    verify(mockSensorManager).unregisterListener(viewIndexingTriggerCaptor.firstValue)
  }
}
