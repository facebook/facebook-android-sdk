package com.facebook;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginButtonTest extends SessionTestsBase {

    static final int STRAY_CALLBACK_WAIT_MILLISECONDS = 50;

    @SmallTest
    @MediumTest
    @LargeTest
    public void testLoginButton() {
        SessionTestsBase.MockTokenCache cache = new SessionTestsBase.MockTokenCache(null, 0);
        ScriptedSession session = new ScriptedSession(getActivity(), "SomeId", cache);
        SessionTestsBase.SessionStatusCallbackRecorder statusRecorder = new SessionTestsBase.SessionStatusCallbackRecorder();
        AccessToken openToken = AccessToken.createFromString("A token of thanks", new ArrayList<String>());

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        final LoginButton button = new LoginButton(getActivity());
        button.setSession(session);
        session.addAuthorizeResult(openToken);
        session.addCallback(statusRecorder);

        button.performClick();

        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        // Verify token information is cleared.
        session.closeAndClearTokenInformation();
        assertTrue(cache.getSavedState() == null);
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testLoginFail() {
        SessionTestsBase.MockTokenCache cache = new SessionTestsBase.MockTokenCache(null, 0);
        ScriptedSession session = new ScriptedSession(getActivity(), "SomeId", cache);
        final Exception openException = new Exception("Open failed!");
        final AtomicBoolean clicked = new AtomicBoolean(false);

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        final LoginButton button = new LoginButton(getActivity());
        LoginButton.OnErrorListener listener = new LoginButton.OnErrorListener() {
            @Override
            public void onError(FacebookException exception) {
                synchronized (this) {
                    assertEquals(exception.getCause().getMessage(), openException.getMessage());
                    clicked.set(true);
                    this.notifyAll();
                }
            }
        };
        button.setOnErrorListener(listener);
        button.setSession(session);
        session.addAuthorizeResult(openException);

        button.onAttachedToWindow();
        button.performClick();

        try {
            synchronized (listener) {
                listener.wait(DEFAULT_TIMEOUT_MILLISECONDS);
            }
        } catch (InterruptedException e) {
            fail("Interrupted during open");
        }

        if (!clicked.get()) {
            fail("Did not get exception");
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCanAddReadPermissions() {
        SessionTestsBase.MockTokenCache cache = new SessionTestsBase.MockTokenCache(null, 0);
        ScriptedSession session = new ScriptedSession(getActivity(), "SomeId", cache);
        SessionTestsBase.SessionStatusCallbackRecorder statusRecorder = new SessionTestsBase.SessionStatusCallbackRecorder();
        AccessToken openToken = AccessToken.createFromString("A token of thanks", new ArrayList<String>());

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        final LoginButton button = new LoginButton(getActivity());
        button.setSession(session);
        button.setReadPermissions(Arrays.asList(new String[] {"read_permission", "read_another"}));
        session.addAuthorizeResult(openToken);
        session.addCallback(statusRecorder);

        button.performClick();

        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        // Verify token information is cleared.
        session.closeAndClearTokenInformation();
        assertTrue(cache.getSavedState() == null);
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCanAddPublishPermissions() {
        SessionTestsBase.MockTokenCache cache = new SessionTestsBase.MockTokenCache(null, 0);
        ScriptedSession session = new ScriptedSession(getActivity(), "SomeId", cache);
        SessionTestsBase.SessionStatusCallbackRecorder statusRecorder =
                new SessionTestsBase.SessionStatusCallbackRecorder();
        AccessToken openToken = AccessToken.createFromString("A token of thanks", new ArrayList<String>());

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        final LoginButton button = new LoginButton(getActivity());
        button.setSession(session);
        button.setPublishPermissions(Arrays.asList(new String[] {"publish_permission", "publish_another"}));
        session.addAuthorizeResult(openToken);
        session.addCallback(statusRecorder);

        button.performClick();

        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        // Verify token information is cleared.
        session.closeAndClearTokenInformation();
        assertTrue(cache.getSavedState() == null);
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCantAddReadThenPublishPermissions() {
        final LoginButton button = new LoginButton(getActivity());
        button.setReadPermissions(Arrays.asList(new String[] {"read_permission", "read_another"}));
        try {
            button.setPublishPermissions(Arrays.asList(new String[] {"read_permission", "read_a_third"}));
            fail("Should not be able to reach here");
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCantAddPublishThenReadPermissions() {
        final LoginButton button = new LoginButton(getActivity());
        button.setPublishPermissions(Arrays.asList(new String[] {"publish_permission", "publish_another"}));
        try {
            button.setReadPermissions(Arrays.asList(new String[] {"publish_permission", "publish_a_third"}));
            fail("Should not be able to reach here");
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCanAddReadThenPublishPermissionsWithClear() {
        final LoginButton button = new LoginButton(getActivity());
        button.setReadPermissions(Arrays.asList(new String[] {"read_permission", "read_another"}));
        button.clearPermissions();
        button.setPublishPermissions(Arrays.asList(new String[] {"publish_permission", "publish_another"}));
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCantAddMorePermissionsToOpenSession() {
        SessionTestsBase.MockTokenCache cache = new SessionTestsBase.MockTokenCache(null, 0);
        ScriptedSession session = new ScriptedSession(getActivity(), "SomeId", cache);
        SessionTestsBase.SessionStatusCallbackRecorder statusRecorder =
                new SessionTestsBase.SessionStatusCallbackRecorder();
        AccessToken openToken = AccessToken.createFromString("A token of thanks",
                Arrays.asList(new String[] {"read_permission", "read_another"}));

        // Verify state with no token in cache
        assertEquals(SessionState.CREATED, session.getState());

        final LoginButton button = new LoginButton(getActivity());
        button.setSession(session);        session.addAuthorizeResult(openToken);
        session.addCallback(statusRecorder);

        button.performClick();

        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);

        // this should be fine
        button.setReadPermissions(Arrays.asList(new String[] {"read_permission", "read_another"}));

        button.setReadPermissions(Arrays.asList(new String[] {"read_permission", "read_a_third"}));
        List<String> permissions = button.getPermissions();
        assertTrue(permissions.contains("read_permission"));
        assertTrue(permissions.contains("read_another"));
        assertFalse(permissions.contains("read_a_third"));

        // Verify token information is cleared.
        session.closeAndClearTokenInformation();
        assertTrue(cache.getSavedState() == null);
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);

        // Wait a bit so we can fail if any unexpected calls arrive on the
        // recorder.
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        statusRecorder.close();
    }

}
