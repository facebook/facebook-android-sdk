/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved. <p> You are hereby granted a
 * non-exclusive, worldwide, royalty-free license to use, copy, modify, and distribute this software
 * in source code or binary form for use in connection with the web services and APIs provided by
 * Facebook. <p> As with any software that integrates with the Facebook platform, your use of this
 * software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY
 * OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.facebook.appevents.internal

import android.os.Bundle
import android.text.format.DateUtils
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.InternalAppEventsLogger
import java.util.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito.*
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(SessionLogger::class)
class SessionLoggerTest : FacebookPowerMockTestCase() {

  private lateinit var mockSessionInfo: SessionInfo
  private lateinit var mockBundle: Bundle
  private lateinit var mockInternalAppEventsLogger: InternalAppEventsLogger
  private lateinit var doubleArgumentCaptor: ArgumentCaptor<Double>

  private val activityName = "swagactivity"
  private val appId = "yoloapplication"
  private val diskRestoreTime = 10L
  private val sessionLastEventTime = 1L
  private val zeroDelta = 0.0

  @Before
  fun init() {
    doubleArgumentCaptor = ArgumentCaptor.forClass(Double::class.java)
    mockSessionInfo = mock(SessionInfo::class.java)
    whenCalled(mockSessionInfo.sessionLength).thenReturn(10L)
    Whitebox.setInternalState(mockSessionInfo, "sessionLastEventTime", sessionLastEventTime)
    Whitebox.setInternalState(mockSessionInfo, "diskRestoreTime", diskRestoreTime)

    mockBundle = mock(Bundle::class.java)
    whenNew(Bundle::class.java).withNoArguments().thenReturn(mockBundle)

    mockInternalAppEventsLogger = mock(InternalAppEventsLogger::class.java)
    doNothing()
        .`when`(mockInternalAppEventsLogger)
        .logEvent(anyString(), anyDouble(), any(Bundle::class.java))
    whenNew(InternalAppEventsLogger::class.java)
        .withArguments(activityName, appId, null)
        .thenReturn(mockInternalAppEventsLogger)

    mockStatic(SessionLogger::class.java)
    whenCalled(
            SessionLogger.logDeactivateApp(anyString(), any(SessionInfo::class.java), anyString()))
        .thenCallRealMethod()
  }

  @Test
  fun `logDeactivateApp when sessionInfo is null`() {
    SessionLogger.logDeactivateApp(activityName, null, appId)
    verifyNew(Bundle::class.java, never()).withNoArguments()
  }

  @Test
  fun `logDeactivateApp when sessionInfo is not null and sessionLength is negative`() {
    val expectedValueToSum = 0.0
    val sessionLengthNegative = -1L
    whenCalled(mockSessionInfo.sessionLength).thenReturn(sessionLengthNegative)

    SessionLogger.logDeactivateApp(activityName, mockSessionInfo, appId)
    verifyNew(Bundle::class.java).withNoArguments()
    verifyNew(InternalAppEventsLogger::class.java).withArguments(activityName, appId, null)
    verify(mockInternalAppEventsLogger)
        .logEvent(
            same(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP),
            doubleArgumentCaptor.capture(),
            same(mockBundle))
    assertEquals(doubleArgumentCaptor.value, expectedValueToSum, zeroDelta)
  }

  @Test
  fun `logDeactivateApp when sessionInfo is not null and interruptionDurationMillis is negative`() {
    val sessionLastEventTime2 = 100L
    val diskRestoreTime2 = 1L
    Whitebox.setInternalState(mockSessionInfo, "sessionLastEventTime", sessionLastEventTime2)
    Whitebox.setInternalState(mockSessionInfo, "diskRestoreTime", diskRestoreTime2)

    val interruptionDurationMillis = 0L
    val fbMobileTimeBetweenSessions =
        String.format(
            Locale.ROOT,
            "session_quanta_%d",
            SessionLogger.getQuantaIndex(interruptionDurationMillis))
    val expectedValueToSum = mockSessionInfo.sessionLength.toDouble() / DateUtils.SECOND_IN_MILLIS

    SessionLogger.logDeactivateApp(activityName, mockSessionInfo, appId)
    verifyNew(Bundle::class.java).withNoArguments()
    verifyNew(InternalAppEventsLogger::class.java).withArguments(activityName, appId, null)
    verify(mockBundle)
        .putString(AppEventsConstants.EVENT_NAME_TIME_BETWEEN_SESSIONS, fbMobileTimeBetweenSessions)
    verify(mockInternalAppEventsLogger)
        .logEvent(
            same(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP),
            doubleArgumentCaptor.capture(),
            same(mockBundle))
    assertEquals(doubleArgumentCaptor.value, expectedValueToSum, zeroDelta)
  }

  @Test
  fun `logDeactivateApp when sessionInfo is not null, sessionLength is positive, and interruptionDurationMillis is positive`() {
    val interruptionDurationMillis = diskRestoreTime - sessionLastEventTime
    val expectedValueToSum = mockSessionInfo.sessionLength.toDouble() / DateUtils.SECOND_IN_MILLIS
    val fbMobileTimeBetweenSessions =
        String.format(
            Locale.ROOT,
            "session_quanta_%d",
            SessionLogger.getQuantaIndex(interruptionDurationMillis))

    SessionLogger.logDeactivateApp(activityName, mockSessionInfo, appId)
    verifyNew(Bundle::class.java).withNoArguments()
    verifyNew(InternalAppEventsLogger::class.java).withArguments(activityName, appId, null)
    verify(mockBundle)
        .putString(AppEventsConstants.EVENT_NAME_TIME_BETWEEN_SESSIONS, fbMobileTimeBetweenSessions)
    verify(mockInternalAppEventsLogger)
        .logEvent(
            same(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP),
            doubleArgumentCaptor.capture(),
            same(mockBundle))
    assertEquals(doubleArgumentCaptor.value, expectedValueToSum, zeroDelta)
  }
}
