/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.gps.ara

import android.adservices.measurement.MeasurementManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.OutcomeReceiver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.gps.GpsDebugLogger
import com.facebook.appevents.internal.Constants.EVENT_NAME_EVENT_KEY
import com.facebook.internal.AnalyticsEvents
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@PrepareForTest(
    FacebookSdk::class,
    MeasurementManager::class,
    GpsAraTriggersManager::class
)
@Config(sdk = [23])
class GpsAraTriggersManagerTest : FacebookPowerMockTestCase() {
    private val applicationId = "app_id"
    private val contentId = "product_id_123"

    private lateinit var context: Context
    private var registerTriggerCalledTimes = 0
    private lateinit var triggerUri: Uri
    private lateinit var mockLogger: GpsDebugLogger

    @Before
    fun setUp() {
        registerTriggerCalledTimes = 0

        mockLogger = PowerMockito.mock(GpsDebugLogger::class.java)
        whenNew(GpsDebugLogger::class.java)
            .withAnyArguments()
            .thenReturn(mockLogger)

        val measurementManager = PowerMockito.mock(MeasurementManager::class.java)
        whenever(
            measurementManager.registerTrigger(
                any<Uri>(),
                any<Executor>(),
                any<OutcomeReceiver<Any, Exception>>()
            )
        ).thenAnswer { invocation ->
            registerTriggerCalledTimes++
            triggerUri = invocation.getArgument<Uri>(0)
            null
        }

        context = mock()
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(MeasurementManager::class.java)).thenReturn(
            measurementManager
        )

        PowerMockito.mockStatic(MeasurementManager::class.java)
        whenever(MeasurementManager.get(any<Context>())).thenReturn(measurementManager)

        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationContext()).thenReturn(context)
        whenever(FacebookSdk.getExecutor()).thenCallRealMethod()
        whenever(FacebookSdk.getFacebookDomain()).thenCallRealMethod()
        whenever(FacebookSdk.isInitialized()).thenReturn(true)

        GpsAraTriggersManager.enable()
    }

    @Test
    fun testRegisterTriggerWithOutcomeReceiver() {
        val event = createEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT)
        GpsAraTriggersManager.registerTrigger(applicationId, event)

        assertEquals(registerTriggerCalledTimes, 1)

        assertEquals(triggerUri.getQueryParameter(AnalyticsEvents.PARAMETER_APP_ID), applicationId)
        assertEquals(
            triggerUri.getQueryParameter(AppEventsConstants.EVENT_PARAM_CONTENT_ID),
            contentId
        )
        assertEquals(
            triggerUri.getQueryParameter(EVENT_NAME_EVENT_KEY),
            AppEventsConstants.EVENT_NAME_VIEWED_CONTENT
        )
    }

    private fun createEvent(eventName: String): AppEvent {
        val params = Bundle()
        params.putCharSequence(AppEventsConstants.EVENT_PARAM_CONTENT_ID, contentId)
        return AppEvent(
            "context_name", eventName, 0.0, params, false,
            isInBackground = false,
            currentSessionId = null
        )
    }
}
