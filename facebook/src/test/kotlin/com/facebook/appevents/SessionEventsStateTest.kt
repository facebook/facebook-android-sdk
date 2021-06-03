package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.GraphRequest
import com.facebook.appevents.eventdeactivation.EventDeactivationManager
import com.facebook.appevents.internal.AppEventsLoggerUtility
import com.facebook.internal.AttributionIdentifiers
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    EventDeactivationManager::class, AppEventsLoggerUtility::class, SessionEventsState::class)
class SessionEventsStateTest : FacebookPowerMockTestCase() {

  private lateinit var sessionEventsState: SessionEventsState
  private val appevent = AppEvent("ctxName", "eventName", 0.0, Bundle(), true, true, null)

  @Before
  fun init() {
    sessionEventsState = spy(SessionEventsState(AttributionIdentifiers(), "anonGUID"))
    Whitebox.setInternalState(SessionEventsState::class.java, "MAX_ACCUMULATED_LOG_EVENTS", 2)
    mockStatic(AppEventsLoggerUtility::class.java)
    mockStatic(EventDeactivationManager::class.java)
  }

  @Test
  fun `test get events`() {
    sessionEventsState.addEvent(appevent)
    assertEquals(1, sessionEventsState.eventsToPersist.size)
    // "The getter is doing more than get, it also clears it :)"
    assertEquals(0, sessionEventsState.eventsToPersist.size)
  }

  @Test
  fun `multiple adds`() {
    val appevent1 = AppEvent("ctxName", "eventName1", 0.0, Bundle(), true, true, null)
    val appevent2 = AppEvent("ctxName", "eventName2", 0.0, Bundle(), true, true, null)
    val appevent3 = AppEvent("ctxName", "eventName3", 0.0, Bundle(), true, true, null)
    sessionEventsState.addEvent(appevent1)
    sessionEventsState.addEvent(appevent2)
    sessionEventsState.addEvent(appevent3)

    assertEquals(2, sessionEventsState.accumulatedEventCount)
    val accumulatedEvents = sessionEventsState.eventsToPersist
    assertEquals(0, sessionEventsState.accumulatedEventCount)
    assertEquals(2, accumulatedEvents.size)
    assertEquals("eventName1", accumulatedEvents[0].name)
    assertEquals("eventName2", accumulatedEvents[1].name)
  }

  @Test
  fun `populate request include implicit`() {
    sessionEventsState.addEvent(appevent)
    val result = sessionEventsState.populateRequest(GraphRequest(), mock(), true, false)
    assertEquals(1, result)
  }

  @Test
  fun `populate request implicit event only`() {
    sessionEventsState.addEvent(appevent)
    val result = sessionEventsState.populateRequest(GraphRequest(), mock(), false, false)
    assertEquals(0, result)
  }

  @Test
  fun `populate no implicit event nor logging`() {
    val appevent1 =
        AppEvent("ctxName", "eventName3", 0.0, Bundle(), /*implicit logged*/ false, true, null)
    sessionEventsState.addEvent(appevent1)
    val result = sessionEventsState.populateRequest(GraphRequest(), mock(), false, false)
    assertEquals(1, result)
  }
}
