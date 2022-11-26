/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.MockSharedPreference
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import java.util.concurrent.ScheduledFuture
import kotlin.test.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(
    AppEventDiskStore::class,
    AppEventStore::class,
    FacebookSdk::class,
    FetchedAppSettingsManager::class,
    LocalBroadcastManager::class)
class AppEventQueueTest : FacebookPowerMockTestCase() {

  private lateinit var mockAppEventCollection: AppEventCollection
  private lateinit var lastAppEventCollection: AppEventCollection
  private lateinit var mockAppEvent: AppEvent
  private lateinit var lastSessionEventsState: SessionEventsState
  private lateinit var mockSessionEventsState: SessionEventsState
  private lateinit var mockAccessTokenAppIdPairSet: HashSet<AccessTokenAppIdPair>
  private lateinit var mockGraphRequest: GraphRequest
  private lateinit var mockGraphResponse: GraphResponse
  private lateinit var mockContext: Context
  private lateinit var mockFetchedAppSettings: FetchedAppSettings
  private lateinit var mockScheduledExecutor: FacebookSerialThreadPoolExecutor
  private lateinit var mockPersistedEvents: PersistedEvents
  private lateinit var mockFacebookRequestError: FacebookRequestError
  private lateinit var mockLocalBroadcastManager: LocalBroadcastManager

  private val executor = FacebookSerialExecutor()
  private val accessTokenAppIdPair = AccessTokenAppIdPair("swagtoken", "yoloapplication")
  private val flushStatistics = FlushStatistics()
  private val flushReason = FlushReason.EVENT_THRESHOLD
  private val numLogEventsToTryToFlushAfter = 100

  @Before
  fun init() {
    mockStatic(AppEventDiskStore::class.java)
    mockStatic(AppEventStore::class.java)
    mockStatic(FacebookSdk::class.java)
    mockStatic(FetchedAppSettingsManager::class.java)

    val mockFetchedAppSettingsManager: FetchedAppSettingsManager = mock()

    mockGraphRequest = mock()
    mockFetchedAppSettings = mock()
    mockContext = mock()
    mockAppEvent = mock()
    mockFacebookRequestError = mock()

    mockGraphResponse = mock()
    mockPersistedEvents = mock()
    whenever(mockGraphResponse.error).thenReturn(mockFacebookRequestError)

    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    whenever(mockContext.getSharedPreferences(any<String>(), any()))
        .thenReturn(MockSharedPreference())
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", executor)
    whenever(FacebookSdk.getExecutor()).thenReturn(executor)

    whenever(mockFetchedAppSettingsManager.queryAppSettings(any(), any()))
        .thenReturn(mockFetchedAppSettings)

    Whitebox.setInternalState(
        FetchedAppSettingsManager::class.java, "INSTANCE", mockFetchedAppSettingsManager)

    mockAccessTokenAppIdPairSet = HashSet<AccessTokenAppIdPair>()
    mockAccessTokenAppIdPairSet.add(accessTokenAppIdPair)
    mockSessionEventsState = mock()
    whenever(mockSessionEventsState.populateRequest(any(), any(), any(), any())).thenReturn(1)
    mockAppEventCollection = mock()
    whenever(mockAppEventCollection.keySet()).thenReturn(mockAccessTokenAppIdPairSet)
    whenever(mockAppEventCollection.get(accessTokenAppIdPair)).thenReturn(mockSessionEventsState)

    val mockCompanion: AppEventsLogger.Companion = mock()
    WhiteboxImpl.setInternalState(AppEventsLogger::class.java, "Companion", mockCompanion)
    whenever(mockCompanion.getFlushBehavior())
        .thenReturn(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY)

    whenever(AppEventStore.persistEvents(any())).thenAnswer {
      lastAppEventCollection = it.getArgument(0) as AppEventCollection
      null
    }
    whenever(AppEventDiskStore.readAndClearStore()).thenReturn(mockPersistedEvents)
    mockScheduledExecutor = spy(FacebookSerialThreadPoolExecutor(1))
    Whitebox.setInternalState(
        AppEventQueue::class.java, "singleThreadExecutor", mockScheduledExecutor)
    Whitebox.setInternalState(
        AppEventQueue::class.java, "appEventCollection", mockAppEventCollection)

    val mockGraphRequestCompanion = mock<GraphRequest.Companion>()
    whenever(
            mockGraphRequestCompanion.newPostRequest(
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
        .thenReturn(mockGraphRequest)

    mockLocalBroadcastManager = mock<LocalBroadcastManager>()
    mockStatic(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(any())).thenReturn(mockLocalBroadcastManager)
  }

  @Test
  fun `persist to disk`() {
    AppEventQueue.persistToDisk()
    assertEquals(mockAppEventCollection, lastAppEventCollection)
  }

  @Test
  fun `flush with explicit reason`() {
    val intentCaptor = argumentCaptor<Intent>()
    whenever(AppEventDiskStore.readAndClearStore()).thenReturn(mockPersistedEvents)

    AppEventQueue.flush(FlushReason.EXPLICIT)

    verify(mockAppEventCollection).addPersistedEvents(mockPersistedEvents)
    verify(mockLocalBroadcastManager).sendBroadcast(intentCaptor.capture())
    assertEquals(intentCaptor.firstValue.action, AppEventsLogger.ACTION_APP_EVENTS_FLUSHED)
  }

  @Test
  fun `flush with eager flushing event reason`() {
    val intentCaptor = argumentCaptor<Intent>()
    whenever(AppEventDiskStore.readAndClearStore()).thenReturn(mockPersistedEvents)

    AppEventQueue.flush(FlushReason.EAGER_FLUSHING_EVENT)

    verify(mockAppEventCollection).addPersistedEvents(mockPersistedEvents)
    verify(mockLocalBroadcastManager).sendBroadcast(intentCaptor.capture())
    assertEquals(intentCaptor.firstValue.action, AppEventsLogger.ACTION_APP_EVENTS_FLUSHED)
  }

  @Test
  fun `get key set`() {
    assertEquals(mockAppEventCollection.keySet(), AppEventQueue.getKeySet())
    AppEventQueue.persistToDisk()
    assertEquals(emptySet<AccessTokenAppIdPair>(), AppEventQueue.getKeySet())
  }

  @Test
  fun `flush and wait`() {
    val intentCaptor = argumentCaptor<Intent>()
    whenever(AppEventDiskStore.readAndClearStore()).thenReturn(mockPersistedEvents)

    AppEventQueue.flushAndWait(flushReason)

    verify(mockAppEventCollection).addPersistedEvents(mockPersistedEvents)
    verify(mockLocalBroadcastManager).sendBroadcast(intentCaptor.capture())
    assertEquals(intentCaptor.firstValue.action, AppEventsLogger.ACTION_APP_EVENTS_FLUSHED)
  }

  @Test
  fun `send events to server`() {
    val flushStatistics =
        AppEventQueue.sendEventsToServer(FlushReason.EXPLICIT, mockAppEventCollection)
    assertEquals(FlushResult.NO_CONNECTIVITY, flushStatistics?.result)
    assertEquals(1, flushStatistics?.numEvents)
  }

  @Test
  fun `build requests`() {
    val requestsList = AppEventQueue.buildRequests(mockAppEventCollection, flushStatistics)
    assertEquals(1, requestsList.size)
  }

  @Test
  fun `build request for session`() {
    val accessTokenAppId = mockAppEventCollection.keySet().first()
    val flushStatistics = FlushStatistics()
    val request =
        AppEventQueue.buildRequestForSession(
            accessTokenAppId,
            checkNotNull(mockAppEventCollection[accessTokenAppId]),
            limitEventUsage = false,
            flushStatistics)
    assertNotNull(request)
    assertEquals("yoloapplication/activities", request.graphPath)
    assertEquals("swagtoken", request.parameters["access_token"])
  }

  @Test
  fun `handle response if error is no connectivity`() {
    whenever(mockFacebookRequestError.errorCode).thenReturn(-1)
    whenever(mockGraphResponse.error).thenReturn(mockFacebookRequestError)
    whenever(AppEventDiskStore.readAndClearStore()).thenReturn(mockPersistedEvents)
    whenever(AppEventStore.persistEvents(accessTokenAppIdPair, mockSessionEventsState)).thenAnswer {
      lastSessionEventsState = it.getArgument(1) as SessionEventsState
      null
    }
    AppEventQueue.handleResponse(
        accessTokenAppIdPair,
        mockGraphRequest,
        mockGraphResponse,
        mockSessionEventsState,
        flushStatistics)
    assertEquals(mockSessionEventsState, lastSessionEventsState)
  }

  @Test
  fun `handle response if error is not no connectivity`() {
    whenever(mockFacebookRequestError.errorCode).thenReturn(0)
    var persistEventsHasBeenCalledTimes = 0
    whenever(AppEventStore.persistEvents(accessTokenAppIdPair, mockSessionEventsState)).thenAnswer {
      persistEventsHasBeenCalledTimes++
    }
    AppEventQueue.handleResponse(
        accessTokenAppIdPair,
        mockGraphRequest,
        mockGraphResponse,
        mockSessionEventsState,
        flushStatistics)
    assertEquals(0, persistEventsHasBeenCalledTimes)
  }

  @Test
  fun `add when flush behavior is not EXPLICIT_ONLY and try to flush`() {
    whenever(AppEventsLogger.getFlushBehavior()).thenReturn(AppEventsLogger.FlushBehavior.AUTO)
    whenever(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter + 1)

    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)

    verify(mockAppEventCollection).addPersistedEvents(anyOrNull())
  }

  @Test
  fun `add when flush behavior is EXPLICIT_ONLY and try to flush`() {
    whenever(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter + 1)

    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)

    verify(mockAppEventCollection, times(0)).addPersistedEvents(anyOrNull())
  }

  @Test
  fun `add when flush behavior is not EXPLICIT_ONLY and not try to flush`() {
    whenever(AppEventsLogger.getFlushBehavior()).thenReturn(AppEventsLogger.FlushBehavior.AUTO)
    whenever(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter - 1)

    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)

    verify(mockAppEventCollection, times(0)).addPersistedEvents(anyOrNull())
  }

  @Test
  fun `add when flush behavior is EXPLICIT_ONLY and not try to flush`() {
    whenever(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter - 1)

    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)

    verify(mockAppEventCollection, times(0)).addPersistedEvents(anyOrNull())
  }

  @Test
  fun `add when scheduledFuture is null`() {
    Whitebox.setInternalState(
        AppEventQueue::class.java, "scheduledFuture", null as ScheduledFuture<*>?)

    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)

    verify(mockScheduledExecutor).schedule(any(), any(), any())
  }

  @Test
  fun `add when scheduledFuture is not null`() {
    val mockScheduleFuture: ScheduledFuture<*> = mock()
    Whitebox.setInternalState(AppEventQueue::class.java, "scheduledFuture", mockScheduleFuture)

    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)

    verify(mockScheduledExecutor, never()).schedule(any(), any(), any())
  }
}
