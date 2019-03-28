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

package com.facebook.appevents;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.RestrictTo;

import com.facebook.AccessToken;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.Executor;

import com.facebook.appevents.AppEventsLogger.FlushBehavior;

/**
 * com.facebook.appevents.InternalAppEventsLogger is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InternalAppEventsLogger {
    private AppEventsLoggerImpl loggerImpl;

    public InternalAppEventsLogger(Context context) {
        loggerImpl = new AppEventsLoggerImpl(context, null, null);
    }

    public InternalAppEventsLogger(
            String activityName,
            String applicationId,
            AccessToken accessToken) {
        loggerImpl = new AppEventsLoggerImpl(activityName, applicationId, accessToken);
    }

    public void logPurchase(
            BigDecimal purchaseAmount, Currency currency, Bundle parameters) {
        loggerImpl.logPurchaseImplicitly(
                purchaseAmount,
                currency,
                parameters
        );
    }

    public void logEvent(String eventName,
                         BigDecimal purchaseAmount,
                         Currency currency,
                         Bundle parameters) {
        loggerImpl.logEventImplicitly(
                eventName,
                purchaseAmount,
                currency,
                parameters);
    }

    public void logEvent(String eventName, Double valueToSum, Bundle parameters) {
        loggerImpl.logEventImplicitly(eventName, valueToSum, parameters);
    }

    public void logEvent(String eventName, Bundle parameters) {
        loggerImpl.logEventImplicitly(eventName, null, parameters);
    }

    public static FlushBehavior getFlushBehavior() {
        return AppEventsLoggerImpl.getFlushBehavior();
    }

    public void flush() {
        loggerImpl.flush();
    }

    static String getSourceApplication() {
        return AppEventsLoggerImpl.getSourceApplication();
    }

    static Executor getAnalyticsExecutor() {
        return AppEventsLoggerImpl.getAnalyticsExecutor();
    }

    static String getPushNotificationsRegistrationId() {
        return AppEventsLoggerImpl.getPushNotificationsRegistrationId();
    }
}
