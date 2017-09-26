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

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;

import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.Logger;

import java.util.Locale;

class SessionLogger {
    private static final String TAG = SessionLogger.class.getCanonicalName();

    private static final long[] INACTIVE_SECONDS_QUANTA =
        new long[] {
            5 * DateUtils.MINUTE_IN_MILLIS,
            15 * DateUtils.MINUTE_IN_MILLIS,
            30 * DateUtils.MINUTE_IN_MILLIS,
            1 * DateUtils.HOUR_IN_MILLIS,
            6 * DateUtils.HOUR_IN_MILLIS,
            12 * DateUtils.HOUR_IN_MILLIS,
            1 * DateUtils.DAY_IN_MILLIS,
            2 * DateUtils.DAY_IN_MILLIS,
            3 * DateUtils.DAY_IN_MILLIS,
            7 * DateUtils.DAY_IN_MILLIS,
            14 * DateUtils.DAY_IN_MILLIS,
            21 * DateUtils.DAY_IN_MILLIS,
            28 * DateUtils.DAY_IN_MILLIS,
            60 * DateUtils.DAY_IN_MILLIS,
            90 * DateUtils.DAY_IN_MILLIS,
            120 * DateUtils.DAY_IN_MILLIS,
            150 * DateUtils.DAY_IN_MILLIS,
            180 * DateUtils.DAY_IN_MILLIS,
            365 * DateUtils.DAY_IN_MILLIS,
        };

    public static void logActivateApp(
            Context applicationContext,
            String activityName,
            SourceApplicationInfo sourceApplicationInfo,
            String appId
    ) {
        String sourAppInfoStr = sourceApplicationInfo != null
                ? sourceApplicationInfo.toString()
                : "Unclassified";

        Bundle eventParams = new Bundle();
        eventParams.putString(
                AppEventsConstants.EVENT_PARAM_SOURCE_APPLICATION,
                sourAppInfoStr);
        AppEventsLogger logger = new InternalAppEventsLogger(
                activityName,
                appId,
                null);
        logger.logEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP, eventParams);
        if (logger.getFlushBehavior() != AppEventsLogger.FlushBehavior.EXPLICIT_ONLY) {
          logger.flush();
        }
    }

    public static void logDeactivateApp(
            Context applicationContext,
            String activityName,
            SessionInfo sessionInfo,
            String appId) {

        Long interruptionDurationMillis =
                sessionInfo.getDiskRestoreTime() - sessionInfo.getSessionLastEventTime();
        if (interruptionDurationMillis < 0) {
            interruptionDurationMillis = 0l;
            logClockSkewEvent();
        }

        Long sessionLength = sessionInfo.getSessionLength();
        if (sessionLength < 0) {
            logClockSkewEvent();
            sessionLength = 0l;
        }

        Bundle eventParams = new Bundle();
        eventParams.putInt(
                AppEventsConstants.EVENT_NAME_SESSION_INTERRUPTIONS,
                sessionInfo.getInterruptionCount());
        eventParams.putString(
                AppEventsConstants.EVENT_NAME_TIME_BETWEEN_SESSIONS,
                String.format(
                        Locale.ROOT,
                        "session_quanta_%d",
                        getQuantaIndex(interruptionDurationMillis)));
        SourceApplicationInfo sourceApplicationInfo = sessionInfo.getSourceApplicationInfo();
        String sourAppInfoStr = sourceApplicationInfo != null
                ? sourceApplicationInfo.toString()
                : "Unclassified";
        eventParams.putString(
                AppEventsConstants.EVENT_PARAM_SOURCE_APPLICATION,
                sourAppInfoStr);

        eventParams.putLong(Constants.LOG_TIME_APP_EVENT_KEY,
                sessionInfo.getSessionLastEventTime() / 1000);

        new InternalAppEventsLogger(
                activityName,
                appId,
                null)
                .logEvent(
                    AppEventsConstants.EVENT_NAME_DEACTIVATED_APP,
                    (sessionLength / DateUtils.SECOND_IN_MILLIS),
                    eventParams);
    }

    private static void logClockSkewEvent() {
        Logger.log(LoggingBehavior.APP_EVENTS, TAG, "Clock skew detected");
    }

    private static int getQuantaIndex(long timeBetweenSessions) {
        int quantaIndex = 0;

        while (
                quantaIndex < INACTIVE_SECONDS_QUANTA.length &&
                        INACTIVE_SECONDS_QUANTA[quantaIndex] < timeBetweenSessions
                ) {
            ++quantaIndex;
        }

        return quantaIndex;
    }
}
