/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
