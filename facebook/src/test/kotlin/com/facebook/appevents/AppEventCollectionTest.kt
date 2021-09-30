package com.facebook.appevents

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.AttributionIdentifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.isA
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    AppEventCollection::class,
    FacebookSdk::class,
    AttributionIdentifiers::class,
    AppEventsLogger::class)
class AppEventCollectionTest : FacebookPowerMockTestCase() {

  private val mockExecutor = FacebookSerialExecutor()

  private lateinit var appEventCollection: AppEventCollection
  private val accessTokenAppIdPair = AccessTokenAppIdPair("swagtoken", "yoloapplication")
  private val appEvent1 = AppEvent("ctxName", "eventName1", 0.0, Bundle(), true, true, null)
  private val accessTokenAppIdPair2 = AccessTokenAppIdPair("anothertoken1337", "yoloapplication")
  private val appEvent2 = AppEvent("ctxName", "eventName2", 0.0, Bundle(), true, true, null)
  private val attributionIdentifiers = AttributionIdentifiers()

  @Before
  fun init() {
    appEventCollection = AppEventCollection()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())

    PowerMockito.mockStatic(AttributionIdentifiers::class.java)
    PowerMockito.`when`(AttributionIdentifiers.getAttributionIdentifiers(isA(Context::class.java)))
        .thenReturn(attributionIdentifiers)

    PowerMockito.mockStatic(AppEventsLogger::class.java)
    PowerMockito.`when`(AppEventsLogger.getAnonymousAppDeviceGUID(isA(Context::class.java)))
        .thenReturn("anonGUID")
  }

  @Test
  fun `test functions addEvent and getters functions`() {
    // Before add any event, the collection should be empty
    assertTrue(appEventCollection.keySet().isEmpty())
    assertNull(appEventCollection.get(accessTokenAppIdPair))
    assertEquals(0, appEventCollection.eventCount)

    // Add the first event to the collection
    appEventCollection.addEvent(accessTokenAppIdPair, appEvent1)
    // Now the collection contains one event, and its keySet contains one token
    assertEquals(setOf(accessTokenAppIdPair), appEventCollection.keySet())
    assertEquals(1, appEventCollection.eventCount)
    assertNotNull(appEventCollection.get(accessTokenAppIdPair))
  }

  @Test
  fun `test addPersistedEvents function`() {
    val map = hashMapOf(accessTokenAppIdPair2 to mutableListOf(appEvent2))
    val persistedEvents = PersistedEvents(map)
    // Before add the persisted event, the collection should be empty
    assertTrue(appEventCollection.keySet().isEmpty())
    assertEquals(0, appEventCollection.eventCount)

    // Add the first persisted event to the collection
    appEventCollection.addPersistedEvents(persistedEvents)
    // Now the collection contains one persisted event, and its keySet contains one token
    assertEquals(setOf(accessTokenAppIdPair2), appEventCollection.keySet())
    assertEquals(1, appEventCollection.eventCount)
    assertNotNull(appEventCollection.get(accessTokenAppIdPair2))
  }
}
