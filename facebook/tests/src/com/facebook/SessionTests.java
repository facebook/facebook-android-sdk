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

import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SessionTests extends SessionTestsBase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testFailNullArguments() {
        try {
            new Session(null);

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
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());

        try {
            // Register these on the blocker thread so they will send
            // notifications there as well. The notifications need to be on a
            // different thread than the progress.
            Runnable initialize0 = new Runnable() {
                @Override
                public void run() {
                    broadcastManager.registerReceiver(receiver0, getActiveSessionAllFilter());

                    broadcastManager.registerReceiver(receiver1,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_SET));
                    broadcastManager.registerReceiver(receiver1,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                    broadcastManager.registerReceiver(receiver1,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_CLOSED));

                    broadcastManager.registerReceiver(receiver2,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                    broadcastManager.registerReceiver(receiver2,
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
            broadcastManager.unregisterReceiver(receiver1);

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
            broadcastManager.unregisterReceiver(receiver0);

            Runnable initialize1 = new Runnable() {
                @Override
                public void run() {
                    broadcastManager.registerReceiver(receiver1,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                    broadcastManager.registerReceiver(receiver1,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                    broadcastManager.registerReceiver(receiver1,
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
            broadcastManager.unregisterReceiver(receiver0);
            broadcastManager.unregisterReceiver(receiver1);
            broadcastManager.unregisterReceiver(receiver2);
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
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());

        try {
            Runnable initializeOnBlockerThread = new Runnable() {
                @Override
                public void run() {
                    broadcastManager.registerReceiver(receiverOpened,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_OPENED));
                    broadcastManager.registerReceiver(receiverClosed,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_CLOSED));
                    broadcastManager.registerReceiver(receiverSet,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_SET));
                    broadcastManager.registerReceiver(receiverUnset,
                            getActiveSessionFilter(Session.ACTION_ACTIVE_SESSION_UNSET));
                }
            };
            runOnBlockerThread(initializeOnBlockerThread, true);

            // null -> null should not fire events
            assertEquals(null, Session.getActiveSession());
            Session.setActiveSession(null);
            assertEquals(null, Session.getActiveSession());

            Session session0 = new Session.Builder(getActivity()).
                    setApplicationId("FakeAppId").
                    setTokenCache(new MockTokenCache()).
                    build();
            assertEquals(SessionState.CREATED_TOKEN_LOADED, session0.getState());

            // For unopened session, we should only see the Set event.
            receiverSet.incrementExpectCount();
            Session.setActiveSession(session0);
            assertEquals(session0, Session.getActiveSession());
            receiverSet.waitForExpectedCalls();

            // When we open it, then we should see the Opened event.
            receiverOpened.incrementExpectCount();
            session0.open();
            receiverOpened.waitForExpectedCalls();

            // Setting to itself should not fire events
            Session.setActiveSession(session0);
            assertEquals(session0, Session.getActiveSession());

            // Setting from one opened session to another should deliver a full
            // cycle of events
            WaitForBroadcastReceiver.incrementExpectCounts(receiverClosed, receiverUnset, receiverSet, receiverOpened);
            Session session1 = new Session.Builder(getActivity()).
                    setApplicationId("FakeAppId").
                    setTokenCache(new MockTokenCache()).
                    build();
            assertEquals(SessionState.CREATED_TOKEN_LOADED, session1.getState());
            session1.open();
            assertEquals(SessionState.OPENED, session1.getState());
            Session.setActiveSession(session1);
            WaitForBroadcastReceiver.waitForExpectedCalls(receiverClosed, receiverUnset, receiverSet, receiverOpened);
            assertEquals(SessionState.CLOSED, session0.getState());
            assertEquals(session1, Session.getActiveSession());

            closeBlockerAndAssertSuccess();
        } finally {
            broadcastManager.unregisterReceiver(receiverOpened);
            broadcastManager.unregisterReceiver(receiverClosed);
            broadcastManager.unregisterReceiver(receiverSet);
            broadcastManager.unregisterReceiver(receiverUnset);
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
        AccessToken openToken = AccessToken
                .createFromString("A token of thanks", permissions, AccessTokenSource.TEST_USER);

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        session.addAuthorizeResult(openToken);
        session.openForRead(new Session.OpenRequest(getActivity()).setCallback(statusRecorder));
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
    public void testOpenForPublishSuccess() {
        ArrayList<String> permissions = new ArrayList<String>();
        MockTokenCache cache = new MockTokenCache(null, 0);
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);
        AccessToken openToken = AccessToken
                .createFromString("A token of thanks", permissions, AccessTokenSource.TEST_USER);

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        session.addAuthorizeResult(openToken);
        session.openForPublish(new Session.OpenRequest(getActivity()).setCallback(statusRecorder).
                setPermissions(Arrays.asList(new String[]{
                        "publish_something",
                        "manage_something",
                        "ads_management",
                        "create_event",
                        "rsvp_event"
                })));
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
    public void testOpenForPublishSuccessWithReadPermissions() {
        ArrayList<String> permissions = new ArrayList<String>();
        MockTokenCache cache = new MockTokenCache(null, 0);
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);
        AccessToken openToken = AccessToken
                .createFromString("A token of thanks", permissions, AccessTokenSource.TEST_USER);

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        session.addAuthorizeResult(openToken);
        session.openForPublish(new Session.OpenRequest(getActivity()).setCallback(statusRecorder).
                setPermissions(Arrays.asList(new String[]{
                        "publish_something",
                        "manage_something",
                        "ads_management",
                        "create_event",
                        "rsvp_event",
                        "read_something"
                })));
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
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        String token = "A token less unique than most";
        MockTokenCache cache = new MockTokenCache(token, DEFAULT_TIMEOUT_MILLISECONDS);
        ScriptedSession session = createScriptedSessionOnBlockerThread("app-id", cache);

        // Verify state when we have a token in cache.
        assertEquals(SessionState.CREATED_TOKEN_LOADED, session.getState());

        session.openForRead(new Session.OpenRequest(getActivity()).setCallback(statusRecorder));

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
    public void testOpenActiveFromEmptyTokenCache() {
        assertNull(Session.openActiveSession(getActivity(), false));
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
        session.openForRead(new Session.OpenRequest(getActivity()).setCallback(statusRecorder));
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
    public void testOpenForReadFailure() {
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        MockTokenCache cache = new MockTokenCache(null, 0);
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);

        try {
            session.openForRead(new Session.OpenRequest(getActivity()).setCallback(statusRecorder).
                    setPermissions(Arrays.asList(new String[]{"publish_something"})));
            fail("should not reach here without an exception");
        } catch (FacebookException e) {
            assertTrue(e.getMessage().contains("Cannot pass a publish permission"));
        } finally {
            stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
            statusRecorder.close();
        }
    }


    @SmallTest
    @MediumTest
    @LargeTest
    public void testReauthorizeSuccess() {
        ArrayList<String> permissions = new ArrayList<String>();
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        MockTokenCache cache = new MockTokenCache(null, 0);
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);

        // Session.open
        final AccessToken openToken = AccessToken
                .createFromString("Allows playing outside", permissions, AccessTokenSource.TEST_USER);
        permissions.add("play_outside");

        session.addAuthorizeResult(openToken);
        session.openForRead(new Session.OpenRequest(getActivity()).setCallback(statusRecorder));
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        verifySessionHasToken(session, openToken);
        assertTrue(cache.getSavedState() != null);
        assertEquals(openToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Successful Session.reauthorize with new permissions
        final AccessToken reauthorizeToken = AccessToken.createFromString(
                "Allows playing outside and eating ice cream", permissions, AccessTokenSource.TEST_USER);
        permissions.add("eat_ice_cream");

        session.addAuthorizeResult(reauthorizeToken);
        session.reauthorizeForRead(new Session.ReauthorizeRequest(getActivity(), permissions));
        statusRecorder.waitForCall(session, SessionState.OPENED_TOKEN_UPDATED, null);

        verifySessionHasToken(session, reauthorizeToken);
        assertTrue(cache.getSavedState() != null);
        assertEquals(reauthorizeToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Failing reauthorization with new permissions
        final Exception reauthorizeException = new Exception("Don't run with scissors");
        permissions.add("run_with_scissors");

        session.addAuthorizeResult(reauthorizeException);
        session.reauthorizeForRead(new Session.ReauthorizeRequest(getActivity(), permissions));
        statusRecorder.waitForCall(session, SessionState.OPENED_TOKEN_UPDATED, reauthorizeException);

        // Verify we do not overwrite cache if reauthorize fails
        assertTrue(cache.getSavedState() != null);
        assertEquals(reauthorizeToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorders.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testReauthorizeForPublishSuccess() {
        ArrayList<String> permissions = new ArrayList<String>();
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        MockTokenCache cache = new MockTokenCache(null, 0);
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);

        // Session.open
        final AccessToken openToken = AccessToken
                .createFromString("Allows playing outside", permissions, AccessTokenSource.TEST_USER);
        permissions.add("play_outside");

        session.addAuthorizeResult(openToken);
        session.openForRead(new Session.OpenRequest(getActivity()).setCallback(statusRecorder));
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        verifySessionHasToken(session, openToken);
        assertTrue(cache.getSavedState() != null);
        assertEquals(openToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Successful Session.reauthorize with new permissions
        final AccessToken reauthorizeToken = AccessToken.createFromString(
                "Allows playing outside and publish eating ice cream", permissions, AccessTokenSource.TEST_USER);
        permissions.add("publish_eat_ice_cream");

        session.addAuthorizeResult(reauthorizeToken);
        session.reauthorizeForPublish(new Session.ReauthorizeRequest(getActivity(), permissions));
        statusRecorder.waitForCall(session, SessionState.OPENED_TOKEN_UPDATED, null);

        verifySessionHasToken(session, reauthorizeToken);
        assertTrue(cache.getSavedState() != null);
        assertEquals(reauthorizeToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Failing reauthorization with publish permissions on a read request
        permissions.add("publish_run_with_scissors");

        try {
            session.reauthorizeForRead(new Session.ReauthorizeRequest(getActivity(), permissions));
            fail("Should not reach here without an exception");
        } catch (FacebookException e) {
            assertTrue(e.getMessage().contains("Cannot pass a publish permission"));
        } finally {
            stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
            statusRecorder.close();
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testOpenWithImportedAccessToken() {
        String token = "This is a fake token.";
        Date expirationDate = new Date(new Date().getTime() + 3600 * 1000);
        Date lastRefreshDate = new Date();
        List<String> permissions = Arrays.asList(new String[]{"email", "publish_stream"});

        MockTokenCache cache = new MockTokenCache(null, 0);
        SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        ScriptedSession session = createScriptedSessionOnBlockerThread(cache);

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        session.openWithImportedAccessToken(token, expirationDate, lastRefreshDate,
                AccessTokenSource.FACEBOOK_APPLICATION, permissions, statusRecorder);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        AccessToken expectedToken = new AccessToken(token, expirationDate, permissions,
                AccessTokenSource.FACEBOOK_APPLICATION, lastRefreshDate);
        verifySessionHasToken(session, expectedToken);

        // Verify we get a close callback.
        session.close();
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);

        // Verify we saved the token to cache.
        assertTrue(cache.getSavedState() != null);
        assertEquals(expectedToken.getToken(), TokenCache.getToken(cache.getSavedState()));

        // Verify token information is cleared.
        session.closeAndClearTokenInformation();
        assertTrue(cache.getSavedState() == null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();

    }

    @MediumTest
    @LargeTest
    public void testSessionWillExtendTokenIfNeeded() {
        TestSession session = openTestSessionWithSharedUser();
        session.forceExtendAccessToken(true);

        Request request = Request.newMeRequest(session, null);
        request.executeAndWait();

        assertTrue(session.getWasAskedToExtendAccessToken());
    }

    @MediumTest
    @LargeTest
    public void testSessionWillNotExtendTokenIfCurrentlyAttempting() {
        TestSession session = openTestSessionWithSharedUser();
        session.forceExtendAccessToken(true);
        session.fakeTokenRefreshAttempt();

        Request request = Request.newMeRequest(session, null);
        request.executeAndWait();
        assertFalse(session.getWasAskedToExtendAccessToken());
    }


    @LargeTest
    public void testBasicSerialization() throws IOException, ClassNotFoundException {
        // Try to test the happy path, that there are no unserializable fields
        // in the session.
        Session session0 = new Session.Builder(getActivity()).setApplicationId("fakeID").
                setShouldAutoPublishInstall(false).build();
        Session session1 = TestUtils.serializeAndUnserialize(session0);

        // do some basic assertions
        assertNotNull(session0.getAccessToken());
        assertEquals(session0, session1);

        Session.AuthorizationRequest authRequest0 =
                new Session.OpenRequest(getActivity()).
                        setRequestCode(123).
                        setLoginBehavior(SessionLoginBehavior.SSO_ONLY);
        Session.AuthorizationRequest authRequest1 = TestUtils.serializeAndUnserialize(authRequest0);

        assertEquals(authRequest0.getLoginBehavior(), authRequest1.getLoginBehavior());
        assertEquals(authRequest0.getRequestCode(), authRequest1.getRequestCode());
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
}
