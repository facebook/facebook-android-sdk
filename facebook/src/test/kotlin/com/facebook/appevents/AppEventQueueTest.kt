package com.facebook.appevents

import android.content.Context
import com.facebook.*
import com.facebook.appevents.AppEventQueue.flushAndWait
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito.*
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.util.ReflectionHelpers

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

    mockGraphRequest = mock(GraphRequest::class.java)
    mockFetchedAppSettings = mock(FetchedAppSettings::class.java)
    mockContext = mock(Context::class.java)
    mockAppEvent = mock(AppEvent::class.java)
    mockFacebookRequestError = mock(FacebookRequestError::class.java)

    mockGraphResponse = mock(GraphResponse::class.java)
    mockPersistedEvents = mock(PersistedEvents::class.java)
    whenCalled(mockGraphResponse.error).thenReturn(mockFacebookRequestError)

    whenCalled(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    whenCalled(FacebookSdk.isInitialized()).thenReturn(true)
    Whitebox.setInternalState(FacebookSdk::class.java, "executor", executor)
    whenCalled(FacebookSdk.getExecutor()).thenReturn(executor)

    whenCalled(FetchedAppSettingsManager.queryAppSettings(isA(String::class.java), anyBoolean()))
        .thenReturn(mockFetchedAppSettings)

    mockAccessTokenAppIdPairSet = HashSet<AccessTokenAppIdPair>()
    mockAccessTokenAppIdPairSet.add(accessTokenAppIdPair)
    mockSessionEventsState = mock(SessionEventsState::class.java)
    mockAppEventCollection = mock(AppEventCollection::class.java)
    whenCalled(mockAppEventCollection.keySet()).thenReturn(mockAccessTokenAppIdPairSet)
    whenCalled(mockAppEventCollection.get(accessTokenAppIdPair)).thenReturn(mockSessionEventsState)

    whenCalled(AppEventsLogger.getFlushBehavior())
        .thenReturn(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY)

    whenCalled(AppEventStore.persistEvents(isA(AppEventCollection::class.java))).thenAnswer {
      lastAppEventCollection = it.getArgument(0) as AppEventCollection
      null
    }
    whenCalled(AppEventStore.readAndClearStore()).thenReturn(mockPersistedEvents)
    ReflectionHelpers.setStaticField(
        AppEventQueue::class.java,
        "NUM_LOG_EVENTS_TO_TRY_TO_FLUSH_AFTER",
        numLogEventsToTryToFlushAfter)

    mockScheduledExecutor = spy(FacebookSerialThreadPoolExecutor(1))
    Whitebox.setInternalState(
        AppEventQueue::class.java, "singleThreadExecutor", mockScheduledExecutor)
    Whitebox.setInternalState(
        AppEventQueue::class.java, "appEventCollection", mockAppEventCollection)
    whenCalled(
            AppEventQueue.buildRequestForSession(
                isA(AccessTokenAppIdPair::class.java),
                isA(SessionEventsState::class.java),
                anyBoolean(),
                isA(FlushStatistics::class.java)))
        .thenReturn(mockGraphRequest)
    whenCalled(AppEventQueue.persistToDisk()).thenCallRealMethod()
    whenCalled(AppEventQueue.flush(isA(FlushReason::class.java))).thenCallRealMethod()
    whenCalled(flushAndWait(isA(FlushReason::class.java))).thenCallRealMethod()
    whenCalled(
            AppEventQueue.handleResponse(
                isA(AccessTokenAppIdPair::class.java),
                isA(GraphRequest::class.java),
                isA(GraphResponse::class.java),
                isA(SessionEventsState::class.java),
                isA(FlushStatistics::class.java)))
        .thenCallRealMethod()
    whenCalled(
            AppEventQueue.buildRequests(
                isA(AppEventCollection::class.java), isA(FlushStatistics::class.java)))
        .thenCallRealMethod()
    whenCalled(AppEventQueue.add(isA(AccessTokenAppIdPair::class.java), isA(AppEvent::class.java)))
        .thenCallRealMethod()
  }

  @Test
  fun `persist to disk`() {
    AppEventQueue.persistToDisk()
    assertEquals(mockAppEventCollection, lastAppEventCollection)
  }

  @Test
  fun `flush and wait`() {
    whenCalled(AppEventStore.readAndClearStore()).thenReturn(mockPersistedEvents)
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
    whenCalled(flushAndWait(isA(FlushReason::class.java))).thenAnswer {
      flushAndWaitHasBeenCalledTimes++
    }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(1, flushAndWaitHasBeenCalledTimes)
  }

  @Test
  fun `add when flush behavior is EXPLICIT_ONLY and try to flush`() {
    whenCalled(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter + 1)
    var flushAndWaitHasBeenCalledTimes = 0
    whenCalled(flushAndWait(isA(FlushReason::class.java))).thenAnswer {
      flushAndWaitHasBeenCalledTimes++
    }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(0, flushAndWaitHasBeenCalledTimes)
  }

  @Test
  fun `add when flush behavior is not EXPLICIT_ONLY and not try to flush`() {
    whenCalled(AppEventsLogger.getFlushBehavior()).thenReturn(AppEventsLogger.FlushBehavior.AUTO)
    whenCalled(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter - 1)
    var flushAndWaitHasBeenCalledTimes = 0
    whenCalled(flushAndWait(isA(FlushReason::class.java))).thenAnswer {
      flushAndWaitHasBeenCalledTimes++
    }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(0, flushAndWaitHasBeenCalledTimes)
  }

  @Test
  fun `add when flush behavior is EXPLICIT_ONLY and not try to flush`() {
    whenCalled(mockAppEventCollection.eventCount).thenReturn(numLogEventsToTryToFlushAfter - 1)
    var flushAndWaitHasBeenCalledTimes = 0
    whenCalled(flushAndWait(isA(FlushReason::class.java))).thenAnswer {
      flushAndWaitHasBeenCalledTimes++
    }
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    assertEquals(0, flushAndWaitHasBeenCalledTimes)
  }

  @Test
  fun `add when scheduledFuture is null`() {
    Whitebox.setInternalState(
        AppEventQueue::class.java, "scheduledFuture", null as ScheduledFuture<*>?)
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    verify(mockScheduledExecutor)
        .schedule(any(Runnable::class.java), anyLong(), any(TimeUnit::class.java))
  }

  @Test
  fun `add when scheduledFuture is not null`() {
    val mockScheduleFuture = mock(ScheduledFuture::class.java)
    Whitebox.setInternalState(AppEventQueue::class.java, "scheduledFuture", mockScheduleFuture)
    AppEventQueue.add(accessTokenAppIdPair, mockAppEvent)
    verify(mockScheduledExecutor, never())
        .schedule(any(Runnable::class.java), anyLong(), any(TimeUnit::class.java))
  }
}
