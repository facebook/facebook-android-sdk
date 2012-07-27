/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

public class SessionTests extends FacebookTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testFailNullArguments() {
        try {
            new Session(null, null, null, null);

            // Should not get here
            assertFalse(true);
        } catch (NullPointerException e) {
            // got expected exception
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testActiveSessionChangeRegistration() {
        final WaitForBroadcastReceiver receiver0 = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiver1 = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiver2 = new WaitForBroadcastReceiver();

        // Register these on the blocker thread so they will send notifications there as well.
        // The notifications need to be on a different thread than the current one so that
        // the Looper will process callbacks while we block the test progress.
        Runnable initialize0 = new Runnable() {
            @Override
            public void run() {
                Session.registerActiveSessionReceiver(receiver0, getActiveSessionAllFilter());

                Session.registerActiveSessionReceiver(receiver1,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_SET));
                Session.registerActiveSessionReceiver(receiver1,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                Session.registerActiveSessionReceiver(receiver1,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_CLOSED));

                Session.registerActiveSessionReceiver(receiver2,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                Session.registerActiveSessionReceiver(receiver2,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_CLOSED));
            }
        };
        runOnBlockerThread(initialize0, true);

        // Verify all actions show up where they are expected
        WaitForBroadcastReceiver.incrementExpectCounts(receiver0, receiver1, receiver2);
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_OPENED);
        WaitForBroadcastReceiver.waitForExpectedCalls(receiver0, receiver1, receiver2);

        WaitForBroadcastReceiver.incrementExpectCounts(receiver0, receiver1, receiver2);
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_CLOSED);
        WaitForBroadcastReceiver.waitForExpectedCalls(receiver0, receiver1, receiver2);

        WaitForBroadcastReceiver.incrementExpectCounts(receiver0, receiver1);
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_SET);
        WaitForBroadcastReceiver.waitForExpectedCalls(receiver0, receiver1);

        receiver0.incrementExpectCount();
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_UNSET);
        receiver0.waitForExpectedCalls();

        // Remove receiver1 and verify actions continue to show up where expected
        Session.unregisterActiveSessionReceiver(receiver1);

        WaitForBroadcastReceiver.incrementExpectCounts(receiver0, receiver2);
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_OPENED);
        WaitForBroadcastReceiver.waitForExpectedCalls(receiver0, receiver2);

        WaitForBroadcastReceiver.incrementExpectCounts(receiver0, receiver2);
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_CLOSED);
        WaitForBroadcastReceiver.waitForExpectedCalls(receiver0, receiver2);

        receiver0.incrementExpectCount();
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_SET);
        receiver0.waitForExpectedCalls();

        receiver0.incrementExpectCount();
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_UNSET);
        receiver0.waitForExpectedCalls();

        // Remove receiver0 and register receiver1 multiple times for one action
        Session.unregisterActiveSessionReceiver(receiver0);

        Runnable initialize1 = new Runnable() {
            @Override
            public void run() {
                Session.registerActiveSessionReceiver(receiver1,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                Session.registerActiveSessionReceiver(receiver1,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                Session.registerActiveSessionReceiver(receiver1,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
            }
        };
        runOnBlockerThread(initialize1, true);

        receiver1.incrementExpectCount(3);
        receiver2.incrementExpectCount();
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_OPENED);
        receiver1.waitForExpectedCalls();
        receiver2.waitForExpectedCalls();

        receiver2.incrementExpectCount();
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_CLOSED);
        receiver2.waitForExpectedCalls();

        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_SET);
        Session.postActiveSessionAction(Session.ACTION_ACTIVE_SESSION_UNSET);

        closeBlockerAndAssertSuccess();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testSetActiveSession() {
        final WaitForBroadcastReceiver receiverOpened = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiverClosed = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiverSet = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiverUnset = new WaitForBroadcastReceiver();

        Runnable initializeOnBlockerThread = new Runnable() {
            @Override
            public void run() {
                Session.registerActiveSessionReceiver(receiverOpened,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                Session.registerActiveSessionReceiver(receiverClosed,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_CLOSED));
                Session.registerActiveSessionReceiver(receiverSet,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_SET));
                Session.registerActiveSessionReceiver(receiverUnset,
                        getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_UNSET));
            }
        };
        runOnBlockerThread(initializeOnBlockerThread, true);

        // null -> null should not fire events
        assertEquals(null, Session.getActiveSession());
        Session.setActiveSession(null);
        assertEquals(null, Session.getActiveSession());

        Session session0 = new Session(getStartedActivity(), "FakeAppId", null, new FakeTokenCache());
        assertEquals(SessionState.CREATED_TOKEN_LOADED, session0.getState());

        // For unopened session, we should only see the Set event.
        receiverSet.incrementExpectCount();
        Session.setActiveSession(session0);
        assertEquals(session0, Session.getActiveSession());
        receiverSet.waitForExpectedCalls();

        // When we open it, then we should see the Opened event.
        receiverOpened.incrementExpectCount();
        session0.open(null, null);
        receiverOpened.waitForExpectedCalls();

        // Setting to itself should not fire events
        Session.setActiveSession(session0);
        assertEquals(session0, Session.getActiveSession());

        // Setting from one opened session to another should deliver a full cycle of events
        WaitForBroadcastReceiver.incrementExpectCounts(receiverClosed, receiverUnset, receiverSet, receiverOpened);
        Session session1 = new Session(getStartedActivity(), "FakeAppId", null, new FakeTokenCache());
        assertEquals(SessionState.CREATED_TOKEN_LOADED, session1.getState());
        session1.open(null, null);
        assertEquals(SessionState.OPENED, session1.getState());
        Session.setActiveSession(session1);
        WaitForBroadcastReceiver.waitForExpectedCalls(receiverClosed, receiverUnset, receiverSet, receiverOpened);
        assertEquals(SessionState.CLOSED, session0.getState());
        assertEquals(session1, Session.getActiveSession());

        closeBlockerAndAssertSuccess();
    }

    static IntentFilter getActiveSessionFilter(String... actions) {
        IntentFilter filter = new IntentFilter();

        for (String action : actions) {
            filter.addAction(action);
        }

        return filter;
    }

    static IntentFilter getActiveSessionAllFilter() {
        return getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_CLOSED, Session.ACTION_ACTIVE_SESSION_OPENED,
                Session.ACTION_ACTIVE_SESSION_SET, Session.ACTION_ACTIVE_SESSION_UNSET);
    }

    static class FakeTokenCache extends TokenCache {

        @Override
        public Bundle load() {
            Bundle bundle = new Bundle();

            TokenCache.putToken(bundle, "FakeToken");
            TokenCache.putExpirationMilliseconds(bundle, System.currentTimeMillis() + 10 * 1000);

            return bundle;
        }

        @Override
        public void save(Bundle bundle) {
            // This space intentionally left blank.
        }

        @Override
        public void clear() {
            // This space intentionally left blank.
        }
        
    }

    static class WaitForBroadcastReceiver extends BroadcastReceiver {
        static int idGenerator = 0;
        final int id = idGenerator++;

        ConditionVariable condition = new ConditionVariable(true);
        int expectCount;
        int actualCount;

        public void incrementExpectCount() {
            incrementExpectCount(1);
        }

        public void incrementExpectCount(int n) {
            expectCount += n;
            if (actualCount < expectCount) {
                condition.close();
            }
        }

        public void waitForExpectedCalls() {
            if (!condition.block(5 * 1000)) {
                assertTrue(false);
            }
        }

        public static void incrementExpectCounts(WaitForBroadcastReceiver...receivers) {
            for (WaitForBroadcastReceiver receiver : receivers) {
                receiver.incrementExpectCount();
            }
        }

        public static void waitForExpectedCalls(WaitForBroadcastReceiver...receivers) {
            for (WaitForBroadcastReceiver receiver : receivers) {
                receiver.waitForExpectedCalls();
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (++actualCount == expectCount) {
                condition.open();
            }
            assertTrue(actualCount <= expectCount);
        }
    }
}
