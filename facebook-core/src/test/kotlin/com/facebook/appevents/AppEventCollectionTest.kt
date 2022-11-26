/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.AttributionIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FacebookSdk::class)
class AppEventCollectionTest : FacebookPowerMockTestCase() {
  private val mockExecutor = FacebookSerialExecutor()

  private lateinit var appEventCollection: AppEventCollection
  private val accessTokenAppIdPair = AccessTokenAppIdPair("swagtoken", "yoloapplication")
  private val appEvent1 = AppEvent("ctxName", "eventName1", 0.0, Bundle(), true, true, null)
  private val accessTokenAppIdPair2 = AccessTokenAppIdPair("anothertoken1337", "yoloapplication")
  private val appEvent2 = AppEvent("ctxName", "eventName2", 0.0, Bundle(), true, true, null)
  private val mockAttributionIdentifiers = PowerMockito.mock(AttributionIdentifiers::class.java)

  @Before
  fun init() {
    appEventCollection = AppEventCollection()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())

    val mockAttributionIdentifierCompanion =
        PowerMockito.mock(AttributionIdentifiers.Companion::class.java)
    WhiteboxImpl.setInternalState(
        AttributionIdentifiers::class.java, "Companion", mockAttributionIdentifierCompanion)
    PowerMockito.`when`(
            mockAttributionIdentifierCompanion.getAttributionIdentifiers(
                ApplicationProvider.getApplicationContext()))
        .thenReturn(mockAttributionIdentifiers)

    val mockCompanion = PowerMockito.mock(AppEventsLogger.Companion::class.java)
    WhiteboxImpl.setInternalState(AppEventsLogger::class.java, "Companion", mockCompanion)
    whenever(mockCompanion.getAnonymousAppDeviceGUID(any())).thenReturn("anonGUID")
  }

  @Test
  fun `test functions addEvent and getters functions`() {
    // Before add any event, the collection should be empty
    assertThat(appEventCollection.keySet().isEmpty()).isTrue
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
    assertThat(appEventCollection.keySet().isEmpty()).isTrue
    assertEquals(0, appEventCollection.eventCount)

    // Add the first persisted event to the collection
    appEventCollection.addPersistedEvents(persistedEvents)
    // Now the collection contains one persisted event, and its keySet contains one token
    assertEquals(setOf(accessTokenAppIdPair2), appEventCollection.keySet())
    assertEquals(1, appEventCollection.eventCount)
    assertNotNull(appEventCollection.get(accessTokenAppIdPair2))
  }
}
