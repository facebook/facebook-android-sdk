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

import com.facebook.AccessToken;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.Utility;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * This class extends the AppEventsLogger to be able to
 *   1. expose creating an app events logger without passing
 *      all required parameters when using AppEventsLogger.newLogger
 *   2. log implicit events
 */

class InternalAppEventsLogger extends AppEventsLogger {
    InternalAppEventsLogger(Context context) {
        this(Utility.getActivityName(context), null, null);
    }

    InternalAppEventsLogger(
            String activityName,
            String applicationId,
            AccessToken accessToken) {
        super(activityName, applicationId, accessToken);
    }

    @Override
    protected void logPurchaseImplicitlyInternal(
            BigDecimal purchaseAmount, Currency currency, Bundle parameters) {
        super.logPurchaseImplicitlyInternal(
                purchaseAmount,
                currency,
                parameters
        );
    }

    @Override
    protected void logEventImplicitly(String eventName,
                                      BigDecimal purchaseAmount,
                                      Currency currency,
                                      Bundle parameters) {
        super.logEventImplicitly(
                eventName,
                purchaseAmount,
                currency,
                parameters);
    }
}
