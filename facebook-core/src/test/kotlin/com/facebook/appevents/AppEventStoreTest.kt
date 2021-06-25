package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.internal.AppEventUtility
import com.facebook.internal.AttributionIdentifiers
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(AppEventStore::class, AppEventUtility::class, PersistedEvents::class)
class AppEventStoreTest : FacebookPowerMockTestCase() {

  private lateinit var lastPersistedEvents: PersistedEvents

  private val accessTokenAppIdPair = AccessTokenAppIdPair("swagtoken", "yoloapplication")
  private val appevent = AppEvent("ctxName", "eventName", 0.0, Bundle(), true, true, null)
  private val accessTokenAppIdPair2 = AccessTokenAppIdPair("anothertoken1337", "yoloapplication")
  private val sessionEventsState = SessionEventsState(AttributionIdentifiers(), "anonGUID")

  @Before
  fun init() {
    PowerMockito.mockStatic(AppEventStore::class.java)
    PowerMockito.mockStatic(AppEventUtility::class.java)

    PowerMockito.`when`(AppEventStore.persistEvents(any(), any())).thenCallRealMethod()
    PowerMockito.`when`(AppEventStore.persistEvents(any())).thenCallRealMethod()
    PowerMockito.`when`(AppEventStore.saveEventsToDisk(any())).thenAnswer {
      lastPersistedEvents = it.getArgument(0) as PersistedEvents
      null
    }
    val map = hashMapOf(accessTokenAppIdPair to mutableListOf(appevent))
    val persistedEvents = PersistedEvents(map)
    PowerMockito.`when`(AppEventStore.readAndClearStore()).thenReturn(persistedEvents)
    sessionEventsState.addEvent(appevent)
  }

  @Test
  fun `different tokenpair size increase`() {
    AppEventStore.persistEvents(accessTokenAppIdPair2, sessionEventsState)
    assertEquals(2, lastPersistedEvents.keySet().size)
  }

  @Test
  fun `same tokenpair size same`() {
    AppEventStore.persistEvents(accessTokenAppIdPair, sessionEventsState)
    assertEquals(1, lastPersistedEvents.keySet().size)
  }
  @Test
  fun `different tokenpair size same persist event collection`() {
    val sessionEventsState = spy(SessionEventsState(AttributionIdentifiers(), "anonGUID"))
    val appeventCollection: AppEventCollection = mock()
    PowerMockito.`when`(appeventCollection.get(any())).thenReturn(sessionEventsState)
    PowerMockito.`when`(appeventCollection.keySet()).thenReturn(mutableSetOf(accessTokenAppIdPair2))
    PowerMockito.`when`(sessionEventsState.eventsToPersist).thenCallRealMethod()
    sessionEventsState.addEvent(appevent)

    AppEventStore.persistEvents(appeventCollection)
    assertEquals(2, lastPersistedEvents.keySet().size)
  }

  @Test
  fun `same tokenpair size same persist event collection`() {
    val sessionEventsState = spy(SessionEventsState(AttributionIdentifiers(), "anonGUID"))
    val appeventCollection: AppEventCollection = mock()
    PowerMockito.`when`(appeventCollection[any()]).thenReturn(sessionEventsState)
    PowerMockito.`when`(appeventCollection.keySet()).thenReturn(mutableSetOf(accessTokenAppIdPair))
    PowerMockito.`when`(sessionEventsState.eventsToPersist).thenCallRealMethod()
    sessionEventsState.addEvent(appevent)

    AppEventStore.persistEvents(appeventCollection)
    assertEquals(1, lastPersistedEvents.keySet().size)
  }
}
