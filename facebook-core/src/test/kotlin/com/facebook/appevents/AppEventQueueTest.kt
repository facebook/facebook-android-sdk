package com.facebook.appevents

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.appevents.AppEventQueue.flushAndWait
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import java.util.concurrent.ScheduledFuture
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    AppEventStore::class,
    AppEventQueue::class,
    FacebookSdk::class,
    GraphRequest::class,
    FetchedAppSettingsManager::class,
    AppEventsLogger::class)
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

  private val executor = FacebookSerialExecutor()
  private val accessTokenAppIdPair = AccessTokenAppIdPair("swagtoken", "yoloapplication")
  private val flushStatistics = FlushStatistics()
  private val flushReason = FlushReason.EVENT_THRESHOLD
  private val numLogEventsToTryToFlushAfter = 5

  @Before
  fun init() {
    mockStatic(AppEventStore::class.java)
    mockStatic(GraphRequest::class.java)
    mockStatic(AppEventQueue::class.java)
    mockStatic(AppEventsLogger::class.java)
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
    whenCalled(mockGraphResponse.error).thenReturn(mockFacebookRequestError)

    whenCalled(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    whenCalled(FacebookSdk.isInitialized()).thenReturn(true)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", executor)
    whenCalled(FacebookSdk.getExecutor()).thenReturn(executor)

    whenCalled(mockFetchedAppSettingsManager.queryAppSettings(any(), any()))
        .thenReturn(mockFetchedAppSettings)

    Whitebox.setInternalState(
        FetchedAppSettingsManager::class.java, "INSTANCE", mockFetchedAppSettingsManager)

    mockAccessTokenAppIdPairSet = HashSet<AccessTokenAppIdPair>()
    mockAccessTokenAppIdPairSet.add(accessTokenAppIdPair)
    mockSessionEventsState = mock()
    mockAppEventCollection = mock()
    whenCalled(mockAppEventCollection.keySet()).thenReturn(mockAccessTokenAppIdPairSet)
    whenCalled(mockAppEventCollection.get(accessTokenAppIdPair)).thenReturn(mockSessionEventsState)

    whenCalled(AppEventsLogger.getFlushBehavior())
        .thenReturn(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY)

    whenCalled(AppEventStore.persistEvents(any())).thenAnswer {
      lastAppEventCollection = it.getArgument(0) as AppEventCollection
      null
    }
    whenCalled(AppEventStore.readAndClearStore()).thenReturn(mockPersistedEvents)
    Whitebox.setInternalState(
        AppEventQueue::class.java,
        "NUM_LOG_EVENTS_TO_TRY_TO_FLUSH_AFTER",
        numLogEventsToTryToFlushAfter)
    mockScheduledExecutor = spy(FacebookSerialThreadPoolExecutor(1))
    Whitebox.setInternalState(
        AppEventQueue::class.java, "singleThreadExecutor", mockScheduledExecutor)
    Whitebox.setInternalState(
        AppEventQueue::class.java, "appEventCollection", mockAppEventCollection)
    whenCalled(AppEventQueue.buildRequestForSession(any(), any(), any(), any()))
        .thenReturn(mockGraphRequest)
    whenCalled(AppEventQueue.persistToDisk()).thenCallRealMethod()
    whenCalled(AppEventQueue.flush(any())).thenCallRealMethod()
    whenCalled(flushAndWait(any())).thenCallRealMethod()
    whenCalled(AppEventQueue.handleResponse(any(), any(), any(), any(), any())).thenCallRealMethod()
    whenCalled(AppEventQueue.buildRequests(any(), any())).thenCallRealMethod()
    whenCalled(AppEventQueue.add(any(), any())).thenCallRealMethod()
  }

  @Test
  fun `persist to disk`() {
    AppEventQueue.persistToDisk()
    assertEquals(mockAppEventCollection, lastAppEventCollection)
  }

  @Test
  fun `flush and wait`() {
    whenCalled(AppEventStore.readAndClearStore()).thenReturn(mockPersistedEvents)
    whenCalled(AppEventQueue.buildRequests(any(), any())).thenReturn(emptyList())
    flushAndWait(flushReason)
    verify(mockAppEventCollection).addPersistedEvents(mockPersistedEvents)
  }

  @Test
  fun `build requests`() {
    val requestsList = AppEventQueue.buildRequests(mockAppEventCollection, flushStatistics)
    assertEquals(1, requestsList.size)
  }

  @Test
  fun `handle response if error is no connectivity`() {
    whenCalled(mockFacebookRequestError.errorCode).thenReturn(-1)
    whenCalled(mockGraphResponse.error).thenReturn(mockFacebookRequestError)
    whenCalled(AppEventStore.readAndClearStore()).thenReturn(mockPersistedEvents)
    whenCalled(AppEventStore.persistEvents(accessTokenAppIdPair, mockSessionEventsState))
        .thenAnswer {
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
    whenCalled(mockFacebookRequestError.errorCode).thenReturn(0)
    var persistEventsHasBeenCalledTimes = 0
    whenCalled(AppEventStore.persistEvents(accessTokenAppIdPair, mockSessionEventsState))
        .thenAnswer { persistEventsHasBeenCalledTimes++ }
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
    whenCalled(AppEventsLogger.getFlushBehavior()).thenReturn(AppEventsLogger.FlushBehavior.AUTO)
    whenCalled(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter + 1)
    var flushAndWaitHasBeenCalledTimes = 0
    whenCalled(flushAndWait(any())).thenAnswer { flushAndWaitHasBeenCalledTimes++ }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(1, flushAndWaitHasBeenCalledTimes)
  }

  @Test
  fun `add when flush behavior is EXPLICIT_ONLY and try to flush`() {
    whenCalled(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter + 1)
    var flushAndWaitHasBeenCalledTimes = 0
    whenCalled(flushAndWait(any())).thenAnswer { flushAndWaitHasBeenCalledTimes++ }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(0, flushAndWaitHasBeenCalledTimes)
  }

  @Test
  fun `add when flush behavior is not EXPLICIT_ONLY and not try to flush`() {
    whenCalled(AppEventsLogger.getFlushBehavior()).thenReturn(AppEventsLogger.FlushBehavior.AUTO)
    whenCalled(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter - 1)
    var flushAndWaitHasBeenCalledTimes = 0
    whenCalled(flushAndWait(any())).thenAnswer { flushAndWaitHasBeenCalledTimes++ }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(0, flushAndWaitHasBeenCalledTimes)
  }

  @Test
  fun `add when flush behavior is EXPLICIT_ONLY and not try to flush`() {
    whenCalled(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter - 1)
    var flushAndWaitHasBeenCalledTimes = 0
    whenCalled(flushAndWait(any())).thenAnswer { flushAndWaitHasBeenCalledTimes++ }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(0, flushAndWaitHasBeenCalledTimes)
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
