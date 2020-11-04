package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(PersistedEvents::class)
class PersistedEventsTest : FacebookPowerMockTestCase() {

  private lateinit var persistedEvents: PersistedEvents
  private val accessTokenAppIdPair = AccessTokenAppIdPair("swagtoken", "yoloapplication")
  private val appEvent1 = AppEvent("ctxName", "eventName1", 0.0, Bundle(), true, true, null)
  private val appEvent2 = AppEvent("ctxName", "eventName2", 0.0, Bundle(), true, true, null)

  @Before
  fun init() {
    val map = hashMapOf(accessTokenAppIdPair to mutableListOf(appEvent1))
    persistedEvents = PersistedEvents(map)
  }

  @Test
  fun `test class initialization`() {
    assertTrue(persistedEvents.containsKey(accessTokenAppIdPair))
    assertEquals(setOf(accessTokenAppIdPair), persistedEvents.keySet())
  }

  @Test
  fun `test add an event with the same token`() {
    assertEquals(mutableListOf(appEvent1), persistedEvents.get(accessTokenAppIdPair))
    persistedEvents.addEvents(accessTokenAppIdPair, listOf(appEvent2))
    assertEquals(mutableListOf(appEvent1, appEvent2), persistedEvents.get(accessTokenAppIdPair))
  }

  @Test
  fun `test add an event with new token`() {
    val accessTokenAppIdPair2 = AccessTokenAppIdPair("anothertoken1337", "yoloapplication")
    val appEvent3 = AppEvent("ctxName", "eventName3", 0.0, Bundle(), true, true, null)
    assertFalse(persistedEvents.containsKey(accessTokenAppIdPair2))
    persistedEvents.addEvents(accessTokenAppIdPair2, listOf(appEvent3))
    assertTrue(persistedEvents.containsKey(accessTokenAppIdPair2))
    assertEquals(mutableListOf(appEvent3), persistedEvents.get(accessTokenAppIdPair2))
  }
}
