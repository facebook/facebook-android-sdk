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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

public class SessionTests extends FacebookTestCase {
    private static final int DEFAULT_TIMEOUT_MILLISECONDS = 10 * 1000;
    private static final int SIMULATED_WORKING_MILLISECONDS = 20;
    private static final int STRAY_CALLBACK_WAIT_MILLISECONDS = 50;

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

        try {
            // Register these on the blocker thread so they will send
            // notifications there as well. The notifications need to be on a
            // different thread than the progress.
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

            // Remove receiver1 and verify actions continue to show up where
            // expected
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

            // Remove receiver0 and register receiver1 multiple times for one
            // action
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
        } finally {
            Session.unregisterActiveSessionReceiver(receiver0);
            Session.unregisterActiveSessionReceiver(receiver1);
            Session.unregisterActiveSessionReceiver(receiver2);
            Session.setActiveSession(null);
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testSetActiveSession() {
        Session.setActiveSession(null);

        final WaitForBroadcastReceiver receiverOpened = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiverClosed = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiverSet = new WaitForBroadcastReceiver();
        final WaitForBroadcastReceiver receiverUnset = new WaitForBroadcastReceiver();

        try {
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

            Session session0 = new Session(getStartedActivity(), "FakeAppId", null, new MockTokenCache());
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

            // Setting from one opened session to another should deliver a full
            // cycle of events
            WaitForBroadcastReceiver.incrementExpectCounts(receiverClosed, receiverUnset, receiverSet, receiverOpened);
            Session session1 = new Session(getStartedActivity(), "FakeAppId", null, new MockTokenCache());
            assertEquals(SessionState.CREATED_TOKEN_LOADED, session1.getState());
            session1.open(null, null);
            assertEquals(SessionState.OPENED, session1.getState());
            Session.setActiveSession(session1);
            WaitForBroadcastReceiver.waitForExpectedCalls(receiverClosed, receiverUnset, receiverSet, receiverOpened);
            assertEquals(SessionState.CLOSED, session0.getState());
            assertEquals(session1, Session.getActiveSession());

            closeBlockerAndAssertSuccess();
        } finally {
            Session.unregisterActiveSessionReceiver(receiverOpened);
            Session.unregisterActiveSessionReceiver(receiverClosed);
            Session.unregisterActiveSessionReceiver(receiverSet);
            Session.unregisterActiveSessionReceiver(receiverUnset);
            Session.setActiveSession(null);
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testOpenSuccess() {
        ArrayList<String> permissions = new ArrayList<String>();
        MockTokenCache cache = new MockTokenCache(null, 0);
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);
        AccessToken openToken = AccessToken.createFromString("A token of thanks", permissions);

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        session.addAuthorizeResult(openToken);
        session.open(getStartedActivity(), statusRecorder);
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        verifySessionHasToken(session, openToken);

        // Verify we get a close callback.
        session.close();
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);

        // Verify we saved the token to cache.
        assertTrue(cache.getSavedState() != null);
        assertEquals(openToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Verify token information is cleared.
        session.closeAndClearTokenInformation();
        assertTrue(cache.getSavedState() == null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testOpenFromTokenCache() {
        ArrayList<String> permissions = new ArrayList<String>();
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        String token = "A token less unique than most";
        MockTokenCache cache = new MockTokenCache(token, DEFAULT_TIMEOUT_MILLISECONDS);
        ScriptedSession session = createScriptedSessionOnBlockerThread("app-id", permissions, cache);

        // Verify state when we have a token in cache.
        assertEquals(SessionState.CREATED_TOKEN_LOADED, session.getState());

        session.open(getStartedActivity(), statusRecorder);

        // Verify we open with no authorize call.
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        // Verify no token information is saved.
        assertTrue(cache.getSavedState() == null);

        // Verify we get a close callback.
        session.close();
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testOpenFailure() {
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        MockTokenCache cache = new MockTokenCache(null, 0);
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);
        Exception openException = new Exception();

        session.addAuthorizeResult(openException);
        session.open(getStartedActivity(), statusRecorder);
        statusRecorder.waitForCall(session, SessionState.OPENING, null);

        // Verify we get the expected exception and no saved state.
        statusRecorder.waitForCall(session, SessionState.CLOSED_LOGIN_FAILED, openException);
        assertTrue(cache.getSavedState() == null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testReauthorizeSuccess() {
        ArrayList<String> permissions = new ArrayList<String>();
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        SessionReauthorizeCallbackRecorder reauthorizeRecorder = new SessionReauthorizeCallbackRecorder();
        MockTokenCache cache = new MockTokenCache(null, 0);
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);

        // Session.open
        final AccessToken openToken = AccessToken.createFromString("Allows playing outside", permissions);
        permissions.add("play_outside");

        session.addAuthorizeResult(openToken);
        session.open(getStartedActivity(), statusRecorder);
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        verifySessionHasToken(session, openToken);
        assertTrue(cache.getSavedState() != null);
        assertEquals(openToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Successful Session.reauthorize with new permissions
        final AccessToken reauthorizeToken = AccessToken.createFromString(
                "Allows playing outside and eating ice cream", permissions);
        permissions.add("eat_ice_cream");

        session.addAuthorizeResult(reauthorizeToken);
        session.reauthorize(getStartedActivity(), reauthorizeRecorder, SessionLoginBehavior.SSO_WITH_FALLBACK,
                permissions, Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
        reauthorizeRecorder.waitForCall(session, null);
        statusRecorder.waitForCall(session, SessionState.OPENED_TOKEN_UPDATED, null);

        verifySessionHasToken(session, reauthorizeToken);
        assertTrue(cache.getSavedState() != null);
        assertEquals(reauthorizeToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Failing reauthorization with new permissions
        final Exception reauthorizeException = new Exception("Don't run with scissors");
        permissions.add("run_with_scissors");

        session.addAuthorizeResult(reauthorizeException);
        session.reauthorize(getStartedActivity(), reauthorizeRecorder, SessionLoginBehavior.SSO_WITH_FALLBACK,
                permissions, Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
        reauthorizeRecorder.waitForCall(session, reauthorizeException);
        statusRecorder.waitForCall(session, SessionState.CLOSED_LOGIN_FAILED, reauthorizeException);

        // Verify we do not overwrite cache if reauthorize fails
        assertTrue(cache.getSavedState() != null);
        assertEquals(reauthorizeToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorders.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
        reauthorizeRecorder.close();
    }

    @MediumTest
    @LargeTest
    public void testSessionWillExtendTokenIfNeeded() {
        TestSession session = openTestSessionWithSharedUser();
        session.forceExtendAccessToken(true);

        Request request = Request.newMeRequest(session, null);
        request.execute();

        assertTrue(session.getWasAskedToExtendAccessToken());
    }

    @MediumTest
    @LargeTest
    public void testSessionWillNotExtendTokenIfCurrentlyAttempting() {
        TestSession session = openTestSessionWithSharedUser();
        session.forceExtendAccessToken(true);
        session.fakeTokenRefreshAttempt();

        Request request = Request.newMeRequest(session, null);
        request.execute();
        assertFalse(session.getWasAskedToExtendAccessToken());
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

    private void verifySessionHasToken(Session session, AccessToken token) {
        assertEquals(token.getToken(), session.getAccessToken());
        assertEquals(token.getExpires(), session.getExpirationDate());
        assertEquals(token.getPermissions(), session.getPermissions());
    }

    private static void stall(int stallMsec) {
        try {
            Thread.sleep(stallMsec);
        } catch (InterruptedException e) {
            fail("InterruptedException while stalling");
        }
    }

    private ScriptedSession createScriptedSessionOnBlockerThread(TokenCache cache) {
        ArrayList<String> permissions = new ArrayList<String>();
        return createScriptedSessionOnBlockerThread("SomeApplicationId", permissions, cache);
    }

    private ScriptedSession createScriptedSessionOnBlockerThread(final String applicationId,
            final List<String> permissions, final TokenCache cache) {
        class MutableState {
            ScriptedSession session;
        }
        ;
        final MutableState mutable = new MutableState();

        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                mutable.session = new ScriptedSession(getStartedActivity(), applicationId, permissions, cache, null);
            }
        }, true);

        return mutable.session;
    }

    private static class ScriptedSession extends Session {
        private final LinkedList<AuthorizeResult> pendingAuthorizations = new LinkedList<AuthorizeResult>();

        ScriptedSession(Context currentContext, String applicationId, List<String> permissions, TokenCache tokenCache,
                Handler handler) {
            super(currentContext, applicationId, permissions, tokenCache, handler);
        }

        public void addAuthorizeResult(AccessToken token) {
            pendingAuthorizations.add(new AuthorizeResult(token));
        }

        public void addAuthorizeResult(Exception exception) {
            pendingAuthorizations.add(new AuthorizeResult(exception));
        }

        // Overrides authorize to return the next AuthorizeResult we added.
        @Override
        void authorize(final Activity currentActivity, final AuthRequest request) {
            SdkRuntime.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    stall(SIMULATED_WORKING_MILLISECONDS);
                    AuthorizeResult result = pendingAuthorizations.poll();

                    if (result == null) {
                        fail("Missing call to addScriptedAuthorization");
                    }

                    finishAuth(currentActivity, result.token, result.exception);
                }
            });
        }

        private static class AuthorizeResult {
            final AccessToken token;
            final Exception exception;

            private AuthorizeResult(AccessToken token, Exception exception) {
                this.token = token;
                this.exception = exception;
            }

            AuthorizeResult(AccessToken token) {
                this(token, null);
            }

            AuthorizeResult(Exception exception) {
                this(null, exception);
            }
        }
    }

    private static class SessionReauthorizeCallbackRecorder implements SessionReauthorizeCallback {
        private final BlockingQueue<Call> calls = new LinkedBlockingQueue<Call>();
        volatile boolean isClosed = false;

        void waitForCall(Session session, Exception exception) {
            Call call = null;

            try {
                call = calls.poll(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
                if (call == null) {
                    fail("Did not get a reauthorize callback within timeout.");
                }
            } catch (InterruptedException e) {
                fail("InterruptedException while waiting for reauthorize: " + e);
            }

            assertEquals(session, call.session);
            assertEquals(exception, call.exception);
        }

        void close() {
            isClosed = true;
            assertEquals(0, calls.size());
        }

        @Override
        public void call(Session session, Exception exception) {
            Call call = new Call(session, exception);
            if (!calls.offer(call)) {
                fail("Test Error: Blocking queue ran out of capacity");
            }
            if (isClosed) {
                fail("Reauthorize callback called after closed");
            }
        }

        private static class Call {
            final Session session;
            final Exception exception;

            Call(Session session, Exception exception) {
                this.session = session;
                this.exception = exception;
            }
        }
    }

    private static class SessionStatusCallbackRecorder implements SessionStatusCallback {
        private final BlockingQueue<Call> calls = new LinkedBlockingQueue<Call>();
        volatile boolean isClosed = false;

        void waitForCall(Session session, SessionState state, Exception exception) {
            Call call = null;

            try {
                call = calls.poll(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
                if (call == null) {
                    fail("Did not get a status callback within timeout.");
                }
            } catch (InterruptedException e) {
                fail("InterruptedException while waiting for status callback: " + e);
            }

            assertEquals(session, call.session);
            assertEquals(state, call.state);
            assertEquals(exception, call.exception);
        }

        void close() {
            isClosed = true;
            assertEquals(0, calls.size());
        }

        @Override
        public void call(Session session, SessionState state, Exception exception) {
            Call call = new Call(session, state, exception);
            if (!calls.offer(call)) {
                fail("Test Error: Blocking queue ran out of capacity");
            }
            if (isClosed) {
                fail("Reauthorize callback called after closed");
            }
        }

        private static class Call {
            final Session session;
            final SessionState state;
            final Exception exception;

            Call(Session session, SessionState state, Exception exception) {
                this.session = session;
                this.state = state;
                this.exception = exception;
            }
        }

    }

    static class MockTokenCache extends TokenCache {
        private final String token;
        private final long expires_in;
        private Bundle saved;

        MockTokenCache() {
            this("FakeToken", DEFAULT_TIMEOUT_MILLISECONDS);
        }

        MockTokenCache(String token, long expires_in) {
            this.token = token;
            this.expires_in = expires_in;
            this.saved = null;
        }

        Bundle getSavedState() {
            return saved;
        }

        @Override
        public Bundle load() {
            Bundle bundle = null;

            if (token != null) {
                bundle = new Bundle();

                TokenCache.putToken(bundle, token);
                TokenCache.putExpirationMilliseconds(bundle, System.currentTimeMillis() + expires_in);
            }

            return bundle;
        }

        @Override
        public void save(Bundle bundle) {
            this.saved = bundle;
        }

        @Override
        public void clear() {
            this.saved = null;
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
            if (!condition.block(DEFAULT_TIMEOUT_MILLISECONDS)) {
                assertTrue(false);
            }
        }

        public static void incrementExpectCounts(WaitForBroadcastReceiver... receivers) {
            for (WaitForBroadcastReceiver receiver : receivers) {
                receiver.incrementExpectCount();
            }
        }

        public static void waitForExpectedCalls(WaitForBroadcastReceiver... receivers) {
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
