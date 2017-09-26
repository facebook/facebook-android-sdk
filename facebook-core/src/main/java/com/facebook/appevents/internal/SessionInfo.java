/**
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

package com.facebook.appevents.internal;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.facebook.FacebookSdk;

import java.util.UUID;

class SessionInfo {
    private static final String LAST_SESSION_INFO_START_KEY
            = "com.facebook.appevents.SessionInfo.sessionStartTime";
    private static final String LAST_SESSION_INFO_END_KEY
            = "com.facebook.appevents.SessionInfo.sessionEndTime";
    private static final String INTERRUPTION_COUNT_KEY
            = "com.facebook.appevents.SessionInfo.interruptionCount";
    private static final String SESSION_ID_KEY
            = "com.facebook.appevents.SessionInfo.sessionId";

    private Long sessionStartTime;
    private Long sessionLastEventTime;
    private int interruptionCount;
    private Long diskRestoreTime;
    private SourceApplicationInfo sourceApplicationInfo;
    private UUID sessionId;

    public SessionInfo(Long sessionStartTime, Long sessionLastEventTime) {
        this(sessionStartTime, sessionLastEventTime, UUID.randomUUID());
    }

    public SessionInfo(Long sessionStartTime, Long sessionLastEventTime, UUID sessionId) {
        this.sessionStartTime = sessionStartTime;
        this.sessionLastEventTime = sessionLastEventTime;
        this.sessionId = sessionId;
    }

    /**
     * Performs disk IO. Do not call from main thread
     * @return
     */
    public static SessionInfo getStoredSessionInfo() {
        SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(
                    FacebookSdk.getApplicationContext());

        long startTime = sharedPreferences.getLong(LAST_SESSION_INFO_START_KEY, 0);
        long endTime = sharedPreferences.getLong(LAST_SESSION_INFO_END_KEY, 0);
        String sessionIDStr = sharedPreferences.getString(SESSION_ID_KEY, null);

        if (startTime == 0 || endTime == 0 || sessionIDStr == null) {
            return null;
        }

        SessionInfo sessionInfo = new SessionInfo(startTime, endTime);
        sessionInfo.interruptionCount = sharedPreferences.getInt(INTERRUPTION_COUNT_KEY, 0);
        sessionInfo.sourceApplicationInfo = SourceApplicationInfo.getStoredSourceApplicatioInfo();
        sessionInfo.diskRestoreTime = System.currentTimeMillis();
        sessionInfo.sessionId = UUID.fromString(sessionIDStr);
        return sessionInfo;
    }

    public static void clearSavedSessionFromDisk() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        FacebookSdk.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(LAST_SESSION_INFO_START_KEY);
        editor.remove(LAST_SESSION_INFO_END_KEY);
        editor.remove(INTERRUPTION_COUNT_KEY);
        editor.remove(SESSION_ID_KEY);
        editor.apply();

        SourceApplicationInfo.clearSavedSourceApplicationInfoFromDisk();
    }

    public Long getSessionStartTime() {
        return sessionStartTime;
    }

    public Long getSessionLastEventTime() {
        return sessionLastEventTime;
    }

    public void setSessionStartTime(Long sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public void setSessionLastEventTime(Long essionLastEventTime) {
        this.sessionLastEventTime = essionLastEventTime;
    }

    public int getInterruptionCount() {
        return interruptionCount;
    }

    public void incrementInterruptionCount() {
        interruptionCount++;
    }

    public long getDiskRestoreTime() {
        return diskRestoreTime == null ? 0 : diskRestoreTime;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public long getSessionLength() {
        if (sessionStartTime == null || sessionLastEventTime == null) {
            return 0;
        }

        return sessionLastEventTime - sessionStartTime;
    }

    public SourceApplicationInfo getSourceApplicationInfo() {
        return sourceApplicationInfo;
    }

    public void setSourceApplicationInfo(SourceApplicationInfo sourceApplicationInfo) {
        this.sourceApplicationInfo = sourceApplicationInfo;
    }

    /**
     * Performs disk IO. Do not call from main thread
     */
    public void writeSessionToDisk() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        FacebookSdk.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(LAST_SESSION_INFO_START_KEY, this.sessionStartTime);
        editor.putLong(LAST_SESSION_INFO_END_KEY, this.sessionLastEventTime);
        editor.putInt(INTERRUPTION_COUNT_KEY, this.interruptionCount);
        editor.putString(SESSION_ID_KEY, this.sessionId.toString());
        editor.apply();

        if (sourceApplicationInfo != null) {
            sourceApplicationInfo.writeSourceApplicationInfoToDisk();
        }
    }
}
