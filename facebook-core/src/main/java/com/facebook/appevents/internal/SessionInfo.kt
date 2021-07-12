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

import android.preference.PreferenceManager
import com.facebook.FacebookSdk
import java.util.UUID

internal class SessionInfo
@JvmOverloads
constructor(
    val sessionStartTime: Long?,
    var sessionLastEventTime: Long?,
    var sessionId: UUID = UUID.randomUUID()
) {
  var interruptionCount = 0
    private set
  var diskRestoreTime: Long? = null
    get() = field ?: 0
  var sourceApplicationInfo: SourceApplicationInfo? = null

  fun incrementInterruptionCount() {
    interruptionCount++
  }

  val sessionLength: Long
    get() =
        if (sessionStartTime == null || sessionLastEventTime == null) {
          0
        } else checkNotNull(sessionLastEventTime) - sessionStartTime

  /** Performs disk IO. Do not call from main thread */
  fun writeSessionToDisk() {
    val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
    val editor = sharedPreferences.edit()
    editor.putLong(LAST_SESSION_INFO_START_KEY, sessionStartTime ?: 0)
    editor.putLong(LAST_SESSION_INFO_END_KEY, sessionLastEventTime ?: 0)
    editor.putInt(INTERRUPTION_COUNT_KEY, interruptionCount)
    editor.putString(SESSION_ID_KEY, sessionId.toString())
    editor.apply()
    if (sourceApplicationInfo != null) {
      sourceApplicationInfo?.writeSourceApplicationInfoToDisk()
    }
  }

  companion object {
    private const val LAST_SESSION_INFO_START_KEY =
        "com.facebook.appevents.SessionInfo.sessionStartTime"
    private const val LAST_SESSION_INFO_END_KEY =
        "com.facebook.appevents.SessionInfo.sessionEndTime"
    private const val INTERRUPTION_COUNT_KEY =
        "com.facebook.appevents.SessionInfo.interruptionCount"
    private const val SESSION_ID_KEY = "com.facebook.appevents.SessionInfo.sessionId"

    /**
     * Performs disk IO. Do not call from main thread
     *
     * @return
     */
    @JvmStatic
    fun getStoredSessionInfo(): SessionInfo? {
      val sharedPreferences =
          PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
      val startTime = sharedPreferences.getLong(LAST_SESSION_INFO_START_KEY, 0)
      val endTime = sharedPreferences.getLong(LAST_SESSION_INFO_END_KEY, 0)
      val sessionIDStr = sharedPreferences.getString(SESSION_ID_KEY, null)
      if (startTime == 0L || endTime == 0L || sessionIDStr == null) {
        return null
      }
      val sessionInfo = SessionInfo(startTime, endTime)
      sessionInfo.interruptionCount = sharedPreferences.getInt(INTERRUPTION_COUNT_KEY, 0)
      sessionInfo.sourceApplicationInfo = SourceApplicationInfo.getStoredSourceApplicatioInfo()
      sessionInfo.diskRestoreTime = System.currentTimeMillis()
      sessionInfo.sessionId = UUID.fromString(sessionIDStr)
      return sessionInfo
    }

    @JvmStatic
    fun clearSavedSessionFromDisk() {
      val sharedPreferences =
          PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
      val editor = sharedPreferences.edit()
      editor.remove(LAST_SESSION_INFO_START_KEY)
      editor.remove(LAST_SESSION_INFO_END_KEY)
      editor.remove(INTERRUPTION_COUNT_KEY)
      editor.remove(SESSION_ID_KEY)
      editor.apply()
      SourceApplicationInfo.clearSavedSourceApplicationInfoFromDisk()
    }
  }
}
