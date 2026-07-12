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
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.gps.GpsDebugLogger
import com.facebook.appevents.internal.Constants.EVENT_NAME_EVENT_KEY
import com.facebook.internal.AnalyticsEvents
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
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
    private var outcomeReceiver: OutcomeReceiver<Any, Exception>? = null
    private var originalSdkInt = 0

    @Before
    fun setUp() {
        registerTriggerCalledTimes = 0
        outcomeReceiver = null

        // Robolectric 4.4 does not support emulating SDK 33+, so fake the SDK level
        // that GpsAraTriggersManager requires for ARA trigger registration.
        originalSdkInt = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 33)

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
            outcomeReceiver = invocation.getArgument<OutcomeReceiver<Any, Exception>>(2)
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

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
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

    @Test
    fun testOutcomeReceiverDoesNotCrashWhenLoggerIsUnavailable() {
        val event = createEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT)
        GpsAraTriggersManager.registerTrigger(applicationId, event)
        val receiver = checkNotNull(outcomeReceiver)

        // Simulate the state where the debug logger was never initialized, e.g. because
        // enable() partially failed. The async AdServices callbacks, which can fire long
        // after registration (e.g. when a backgrounded app's trigger is rejected), must
        // handle the failure internally instead of crashing the host app.
        Whitebox.setInternalState(
            GpsAraTriggersManager::class.java,
            "gpsDebugLogger",
            null as GpsDebugLogger?
        )

        receiver.onError(Exception("registration rejected"))
        receiver.onResult(Any())
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
