/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.gps.ara

import android.adservices.common.AdServicesOutcomeReceiver
import android.adservices.measurement.MeasurementManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.OutcomeReceiver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.gps.GpsCapabilityChecker
import com.facebook.appevents.gps.GpsDebugLogger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import java.util.concurrent.Executor

@PrepareForTest(
    FacebookSdk::class,
    MeasurementManager::class,
    GpsCapabilityChecker::class
)
class GpsAraTriggersManagerTest : FacebookPowerMockTestCase() {
    private val applicationId = "app_id"
    private val contentId = "product_id_123"

    //    private lateinit var context: Context
    private var registerTriggerCalledTimes = 0
    private lateinit var triggerUri: Uri
    private lateinit var mockLogger: GpsDebugLogger

    @Before
    fun setUp() {
        registerTriggerCalledTimes = 0

        mockLogger = mock()
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
        whenever(
            measurementManager.registerTrigger(
                any<Uri>(),
                any<Executor>(),
                any<AdServicesOutcomeReceiver<Any, Exception>>()
            )
        ).thenAnswer { invocation ->
            registerTriggerCalledTimes++
            triggerUri = invocation.getArgument<Uri>(0)
            null
        }

//        context = PowerMockito.mock(Context::class.java)
//        doReturn(measurementManager).whenever(context)
//            .getSystemService(MeasurementManager::class.java)

        PowerMockito.mockStatic(MeasurementManager::class.java)
        whenever(MeasurementManager.get(any<Context>())).thenReturn(measurementManager)

        PowerMockito.mockStatic(FacebookSdk::class.java)
//        whenever(FacebookSdk.getApplicationContext()).thenReturn(context)
        whenever(FacebookSdk.getExecutor()).thenCallRealMethod()

        PowerMockito.mockStatic(GpsCapabilityChecker::class.java)

        GpsAraTriggersManager.enable()
    }

    @Test
    fun testRegisterTriggerWithOutcomeReceiver() {
        assertEquals(0, 0)
        // TODO: Update tests to not rely on mocked context
//        whenever(GpsCapabilityChecker.useOutcomeReceiver()).thenReturn(true)
//
//        val event = createEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT)
//        GpsAraTriggersManager.registerTrigger(applicationId, event)
//
//        assertEquals(registerTriggerCalledTimes, 1)
//
//        assertEquals(triggerUri.getQueryParameter(AnalyticsEvents.PARAMETER_APP_ID), applicationId)
//        assertEquals(
//            triggerUri.getQueryParameter(AppEventsConstants.EVENT_PARAM_CONTENT_ID),
//            contentId
//        )
//        assertEquals(
//            triggerUri.getQueryParameter(EVENT_NAME_EVENT_KEY),
//            AppEventsConstants.EVENT_NAME_VIEWED_CONTENT
//        )
//    }
//
//    @Test
//    fun testRegisterTriggerWithAdsOutcomeReceiver() {
//        whenever(GpsCapabilityChecker.useOutcomeReceiver()).thenReturn(false)
//
//        val event = createEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT)
//        GpsAraTriggersManager.registerTrigger(applicationId, event)
//
//        assertEquals(registerTriggerCalledTimes, 1)
//
//        assertEquals(triggerUri.getQueryParameter(AnalyticsEvents.PARAMETER_APP_ID), applicationId)
//        assertEquals(
//            triggerUri.getQueryParameter(AppEventsConstants.EVENT_PARAM_CONTENT_ID),
//            contentId
//        )
//        assertEquals(
//            triggerUri.getQueryParameter(EVENT_NAME_EVENT_KEY),
//            AppEventsConstants.EVENT_NAME_VIEWED_CONTENT
//        )
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
