/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.GraphRequest
import com.facebook.appevents.eventdeactivation.EventDeactivationManager
import com.facebook.appevents.internal.AppEventsLoggerUtility
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FeatureManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    EventDeactivationManager::class,
    AppEventsLoggerUtility::class,
    SessionEventsState::class,
    FeatureManager::class
)
class SessionEventsStateTest : FacebookPowerMockTestCase() {

    private lateinit var sessionEventsState: SessionEventsState
    private val appEvent = AppEvent("ctxName", "eventName", 0.0, Bundle(), true, true, null)
    private val appEventWithOperationalData =
        AppEvent("ctxName", "eventName", 0.0, Bundle(), true, true, null, OperationalData())

    @Before
    fun init() {
        sessionEventsState = spy(SessionEventsState(AttributionIdentifiers(), "anonGUID"))
        Whitebox.setInternalState(SessionEventsState::class.java, "MAX_ACCUMULATED_LOG_EVENTS", 2)
        mockStatic(AppEventsLoggerUtility::class.java)
        mockStatic(EventDeactivationManager::class.java)
        mockStatic(FeatureManager::class.java)
    }

    @Test
    fun `test get events`() {
        sessionEventsState.addEvent(appEvent)
        assertEquals(1, sessionEventsState.eventsToPersist.size)
        // "The getter is doing more than get, it also clears it :)"
        assertEquals(0, sessionEventsState.eventsToPersist.size)
    }

    @Test
    fun `multiple adds`() {
        val appEvent1 = AppEvent("ctxName", "eventName1", 0.0, Bundle(), true, true, null)
        val appEvent2 = AppEvent("ctxName", "eventName2", 0.0, Bundle(), true, true, null)
        val appEvent3 = AppEvent("ctxName", "eventName3", 0.0, Bundle(), true, true, null)
        sessionEventsState.addEvent(appEvent1)
        sessionEventsState.addEvent(appEvent2)
        sessionEventsState.addEvent(appEvent3)

        assertEquals(2, sessionEventsState.accumulatedEventCount)
        val accumulatedEvents = sessionEventsState.eventsToPersist
        assertEquals(0, sessionEventsState.accumulatedEventCount)
        assertEquals(2, accumulatedEvents.size)
        assertEquals("eventName1", accumulatedEvents[0].name)
        assertEquals("eventName2", accumulatedEvents[1].name)
    }

    @Test
    fun `populate request include implicit`() {
        sessionEventsState.addEvent(appEvent)
        val result = sessionEventsState.populateRequest(GraphRequest(), mock(), true, false)
        assertEquals(1, result)
    }

    @Test
    fun `populate request without operational parameters`() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib5To7))
            .thenReturn(false)
        sessionEventsState.addEvent(appEventWithOperationalData)
        val request = GraphRequest()
        val result = sessionEventsState.populateRequest(request, mock(), true, false)
        assertEquals(1, result)
        assertEquals(request.parameters.getCharSequence("operational_parameters"), null)
    }

    @Test
    fun `populate request with operational parameters`() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib5To7))
            .thenReturn(true)
        sessionEventsState.addEvent(appEventWithOperationalData)
        val request = GraphRequest()
        val result = sessionEventsState.populateRequest(request, mock(), true, false)
        assertEquals(1, result)
        assertEquals(request.parameters.getCharSequence("operational_parameters"), "[{}]")
    }

    @Test
    fun `populate request implicit event only`() {
        sessionEventsState.addEvent(appEvent)
        val result = sessionEventsState.populateRequest(GraphRequest(), mock(), false, false)
        assertEquals(0, result)
    }

    @Test
    fun `populate no implicit event nor logging`() {
        val appEvent1 =
            AppEvent("ctxName", "eventName3", 0.0, Bundle(), /*implicit logged*/ false, true, null)
        sessionEventsState.addEvent(appEvent1)
        val result = sessionEventsState.populateRequest(GraphRequest(), mock(), false, false)
        assertEquals(1, result)
    }
}
