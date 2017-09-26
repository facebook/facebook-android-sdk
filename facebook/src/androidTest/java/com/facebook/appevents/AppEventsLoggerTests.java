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

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.FacebookSdk;
import com.facebook.FacebookTestCase;
import com.facebook.WaitForBroadcastReceiver;

public class AppEventsLoggerTests extends FacebookTestCase {
    public void testSimpleCall() throws InterruptedException {
        AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY);

        AccessToken accessToken1 = getAccessTokenForSharedUser();
        AccessToken accessToken2 = getAccessTokenForSharedUser(SECOND_TEST_USER_TAG);

        AppEventsLogger logger1 = AppEventsLogger.newLogger(getActivity(), accessToken1);
        AppEventsLogger logger2 = AppEventsLogger.newLogger(getActivity(), accessToken2);

        final WaitForBroadcastReceiver waitForBroadcastReceiver = new WaitForBroadcastReceiver();
        waitForBroadcastReceiver.incrementExpectCount();

        final LocalBroadcastManager broadcastManager =
                LocalBroadcastManager.getInstance(getActivity());

        try {
            // Need to get notifications on another thread so we can wait for them.
            runOnBlockerThread(new Runnable() {
                @Override
                public void run() {
                    broadcastManager.registerReceiver(waitForBroadcastReceiver,
                            new IntentFilter(AppEventsLogger.ACTION_APP_EVENTS_FLUSHED));
                }
            }, true);

            logger1.logEvent("an_event");
            logger2.logEvent("another_event");

            // test illegal event name and event key, should not crash in non-debug environment.
            logger1.logEvent("$illegal_event_name");
            Bundle params = new Bundle();
            params.putString("illegal%key", "good_value");
            logger1.logEvent("legal_event_name", params);
            char[] val = {'b', 'a', 'd'};
            params.putCharArray("legal_key", val);
            logger1.logEvent("legal_event",params);

            logger1.flush();

            waitForBroadcastReceiver.waitForExpectedCalls();

            closeBlockerAndAssertSuccess();
        } finally {
            broadcastManager.unregisterReceiver(waitForBroadcastReceiver);
        }
    }

    public void testExplicitCallWithNoAppSettings() throws InterruptedException {
        AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY);

        AccessToken accessToken1 = getFakeAccessToken();
        FacebookSdk.setApplicationId("234");

        AppEventsLogger logger1 = AppEventsLogger.newLogger(getActivity(), accessToken1);

        final WaitForBroadcastReceiver waitForBroadcastReceiver = new WaitForBroadcastReceiver();
        waitForBroadcastReceiver.incrementExpectCount();

        final LocalBroadcastManager broadcastManager =
                LocalBroadcastManager.getInstance(getActivity());

        try {
            // Need to get notifications on another thread so we can wait for them.
            runOnBlockerThread(new Runnable() {
                @Override
                public void run() {
                    broadcastManager.registerReceiver(waitForBroadcastReceiver,
                            new IntentFilter(AppEventsLogger.ACTION_APP_EVENTS_FLUSHED));
                }
            }, true);

            logger1.logEvent("an_event");

            logger1.flush();

            waitForBroadcastReceiver.waitForExpectedCalls();

            closeBlockerAndAssertSuccess();
        } finally {
            broadcastManager.unregisterReceiver(waitForBroadcastReceiver);
        }
    }

    private AccessToken getFakeAccessToken() {
        return new AccessToken(
                "foobar",
                "234",
                "567",
                null,
                null,
                AccessTokenSource.TEST_USER,
                null,
                null);
    }
}
