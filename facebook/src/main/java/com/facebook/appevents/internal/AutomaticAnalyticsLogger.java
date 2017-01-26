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

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Validate;

/**
 * com.facebook.appevents.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class AutomaticAnalyticsLogger {
    // Constants
    private static final String TAG = AutomaticAnalyticsLogger.class.getCanonicalName();

    public static void logActivateAppEvent() {
        final Context context = FacebookSdk.getApplicationContext();
        final String appId = FacebookSdk.getApplicationId();
        final boolean autoLogAppEvents = FacebookSdk.getAutoLogAppEventsEnabled();
        Validate.notNull(context, "context");
        if (autoLogAppEvents) {
            if (context instanceof Application) {
                AppEventsLogger.activateApp((Application) context, appId);
            } else { // Context is probably originated from ContentProvider or Mocked
                Log.w(
                    TAG,
                    "Automatic logging of basic events will not happen, because " +
                      "FacebookSdk.getApplicationContext() returns object that is not " +
                      "instance of android.app.Application. Make sure you call " +
                      "FacebookSdk.sdkInitialize() from Application class and pass " +
                      "application context.");
            }
        }
    }

    public static void logActivityTimeSpentEvent(String activityName, long timeSpentInSeconds) {
        final Context context = FacebookSdk.getApplicationContext();
        final String appId = FacebookSdk.getApplicationId();
        Validate.notNull(context, "context");
        final FetchedAppSettings settings = FetchedAppSettingsManager.queryAppSettings(
                appId, false);
        if (settings != null && settings.getAutomaticLoggingEnabled() && timeSpentInSeconds > 0) {
            AppEventsLogger appEventsLogger = AppEventsLogger.newLogger(context);
            Bundle params = new Bundle(1);
            params.putCharSequence(Constants.AA_TIME_SPENT_SCREEN_PARAMETER_NAME, activityName);
            appEventsLogger.logEvent(
                Constants.AA_TIME_SPENT_EVENT_NAME, timeSpentInSeconds, params);
        }
    }
}
