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
import com.facebook.appevents.internal.AppEventUtility
import com.facebook.internal.AttributionIdentifiers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(AppEventDiskStore::class, AppEventUtility::class, PersistedEvents::class)
class AppEventStoreTest : FacebookPowerMockTestCase() {

  private lateinit var lastPersistedEvents: PersistedEvents

  private val accessTokenAppIdPair = AccessTokenAppIdPair("swagtoken", "yoloapplication")
  private val appevent = AppEvent("ctxName", "eventName", 0.0, Bundle(), true, true, null)
  private val accessTokenAppIdPair2 = AccessTokenAppIdPair("anothertoken1337", "yoloapplication")
  private val sessionEventsState = SessionEventsState(AttributionIdentifiers(), "anonGUID")

  @Before
  fun init() {
    PowerMockito.mockStatic(AppEventDiskStore::class.java)
    PowerMockito.mockStatic(AppEventUtility::class.java)

    whenever(AppEventDiskStore.saveEventsToDisk(any())).thenAnswer {
      lastPersistedEvents = it.getArgument(0) as PersistedEvents
      null
    }
    val map = hashMapOf(accessTokenAppIdPair to mutableListOf(appevent))
    val persistedEvents = PersistedEvents(map)
    whenever(AppEventDiskStore.readAndClearStore()).thenReturn(persistedEvents)
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
    whenever(appeventCollection.get(any())).thenReturn(sessionEventsState)
    whenever(appeventCollection.keySet()).thenReturn(mutableSetOf(accessTokenAppIdPair2))
    whenever(sessionEventsState.eventsToPersist).thenCallRealMethod()
    sessionEventsState.addEvent(appevent)

    AppEventStore.persistEvents(appeventCollection)
    assertEquals(2, lastPersistedEvents.keySet().size)
  }

  @Test
  fun `same tokenpair size same persist event collection`() {
    val sessionEventsState = spy(SessionEventsState(AttributionIdentifiers(), "anonGUID"))
    val appeventCollection: AppEventCollection = mock()
    whenever(appeventCollection[any()]).thenReturn(sessionEventsState)
    whenever(appeventCollection.keySet()).thenReturn(mutableSetOf(accessTokenAppIdPair))
    whenever(sessionEventsState.eventsToPersist).thenCallRealMethod()
    sessionEventsState.addEvent(appevent)

    AppEventStore.persistEvents(appeventCollection)
    assertEquals(1, lastPersistedEvents.keySet().size)
  }
}
