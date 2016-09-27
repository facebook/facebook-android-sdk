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

import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.Utility;
import com.facebook.internal.Utility.FetchedAppSettings;

class AutomaticAnalyticsLogger {

    public static void logActivityTimeSpentEvent(
            Context context,
            String appId,
            String activityName,
            long timeSpentInSeconds) {
        AppEventsLogger l = AppEventsLogger.newLogger(context);
        final FetchedAppSettings settings = Utility.queryAppSettings(appId, false);
        if (settings.getAutomaticLoggingEnabled() && timeSpentInSeconds > 0) {
            Bundle params = new Bundle(1);
            params.putCharSequence(Constants.AA_TIME_SPENT_SCREEN_PARAMETER_NAME, activityName);
            l.logEvent(Constants.AA_TIME_SPENT_EVENT_NAME, timeSpentInSeconds, params);
        }
    }

}
