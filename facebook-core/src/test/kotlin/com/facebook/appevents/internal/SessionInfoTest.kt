/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.internal

import android.content.Context
import android.preference.PreferenceManager
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers.isA
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FacebookSdk::class, PreferenceManager::class, SourceApplicationInfo::class)
class SessionInfoTest : FacebookPowerMockTestCase() {

  private lateinit var mockContext: Context
  private lateinit var mockSharedPreferences: MockSharedPreference
  private lateinit var mockSessionInfo: SessionInfo

  private val sessionStartTime = 1L
  private val sessionLastEventTime = 2L
  private val interruptionCount = 0
  private val sessionId = UUID(2, 1)
  private var clearSavedSourceApplicationInfoFromDiskHasBeenCalledTime = 0

  @Before
  fun init() {
    clearSavedSourceApplicationInfoFromDiskHasBeenCalledTime = 0

    mockContext = mock(Context::class.java)
    mockSharedPreferences = MockSharedPreference()
    mockSessionInfo = mock(SessionInfo::class.java)

    Whitebox.setInternalState(mockSessionInfo, "sessionStartTime", sessionStartTime)
    Whitebox.setInternalState(mockSessionInfo, "sessionLastEventTime", sessionLastEventTime)
    Whitebox.setInternalState(mockSessionInfo, "interruptionCount", interruptionCount)
    Whitebox.setInternalState(mockSessionInfo, "sessionId", sessionId)
    whenever(mockSessionInfo.writeSessionToDisk()).thenCallRealMethod()

    mockStatic(FacebookSdk::class.java)
    mockStatic(PreferenceManager::class.java)

    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(PreferenceManager.getDefaultSharedPreferences(isA(Context::class.java)))
        .thenReturn(mockSharedPreferences)

    val mockSourceAppInfoCompanion = mock(SourceApplicationInfo.Companion::class.java)
    WhiteboxImpl.setInternalState(
        SourceApplicationInfo::class.java, "Companion", mockSourceAppInfoCompanion)
    whenever(mockSourceAppInfoCompanion.clearSavedSourceApplicationInfoFromDisk()).then {
      clearSavedSourceApplicationInfoFromDiskHasBeenCalledTime++
    }
  }

  @Test
  fun `write session to disk and get the session info when source application info is null`() {
    mockSessionInfo.writeSessionToDisk()
    val sessionInfo = SessionInfo.getStoredSessionInfo()
    assertEquals(sessionStartTime, sessionInfo?.sessionStartTime)
    assertEquals(sessionLastEventTime, sessionInfo?.sessionLastEventTime)
    assertEquals(interruptionCount, sessionInfo?.interruptionCount)
    assertEquals(sessionId, sessionInfo?.sessionId)
  }

  @Test
  fun `write session to disk and get the session info when source application info is not null`() {
    val mockSourceApplicationInfo = mock(SourceApplicationInfo::class.java)
    Whitebox.setInternalState(mockSessionInfo, "sourceApplicationInfo", mockSourceApplicationInfo)
    mockSessionInfo.writeSessionToDisk()
    val sessionInfo = SessionInfo.getStoredSessionInfo()
    assertEquals(sessionStartTime, sessionInfo?.sessionStartTime)
    assertEquals(sessionLastEventTime, sessionInfo?.sessionLastEventTime)
    assertEquals(interruptionCount, sessionInfo?.interruptionCount)
    assertEquals(sessionId, sessionInfo?.sessionId)
    verify(mockSourceApplicationInfo).writeSourceApplicationInfoToDisk()
  }

  @Test
  fun `get stored session info when there is no stored session info`() {
    val sessionInfo = SessionInfo.getStoredSessionInfo()
    assertNull(sessionInfo)
  }

  @Test
  fun `clear saved session from disk`() {
    mockSessionInfo.writeSessionToDisk()
    SessionInfo.clearSavedSessionFromDisk()
    val sessionInfo = SessionInfo.getStoredSessionInfo()
    assertNull(sessionInfo)
    assertEquals(1, clearSavedSourceApplicationInfoFromDiskHasBeenCalledTime)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T103601731
  @Test
  fun `clear saved session from disk when there is no saved session`() {
    SessionInfo.clearSavedSessionFromDisk()
    val sessionInfo = SessionInfo.getStoredSessionInfo()
    assertNull(sessionInfo)
    assertEquals(1, clearSavedSourceApplicationInfoFromDiskHasBeenCalledTime)
  }
}
