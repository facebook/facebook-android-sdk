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

import java.util.Collections;

import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

public class SessionTrackerTests extends SessionTestsBase {

    private static final String TOKEN_STR = "A token of thanks";

    @SmallTest
    @MediumTest
    @LargeTest
    // Tests the SessionDelegate while tracking the active session
    public void testDelegateWithActiveSession() throws Exception {
        Session.setActiveSession(null);
        final SessionStatusCallbackRecorder statusRecorder = 
                new SessionStatusCallbackRecorder();
        final MockTokenCache cache = new MockTokenCache(null, 0);
        final ScriptedSession session = 
                createScriptedSessionOnBlockerThread(cache);

        final AccessToken openToken = AccessToken.createFromString(TOKEN_STR, 
                Collections.<String>emptyList());
        session.addAuthorizeResult(openToken);

        final SessionTracker tracker = new SessionTracker(getActivity(), statusRecorder);
        Session.setActiveSession(session); 
        
        session.open(getActivity(), null);
        
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);
        assertNotNull("Session should be open", tracker.getOpenSession());
        assertEquals("Access Token check", TOKEN_STR, tracker.getOpenSession().getAccessToken());
        tracker.getOpenSession().close();
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);
        
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        tracker.stopTracking();
        statusRecorder.close();
    }
    
    @SmallTest
    @MediumTest
    @LargeTest
    // Tests the SessionDelegate while tracking a passed in session from the constructor
    public void testDelegateWithSessionInConstructor() throws Exception {
        final SessionStatusCallbackRecorder statusRecorder = 
                new SessionStatusCallbackRecorder();
        final MockTokenCache cache = new MockTokenCache(null, 0);
        final ScriptedSession session = 
                createScriptedSessionOnBlockerThread(cache);

        final AccessToken openToken = AccessToken.createFromString(TOKEN_STR, 
                Collections.<String>emptyList());
        session.addAuthorizeResult(openToken);

        SessionTracker tracker = new SessionTracker(getActivity(), statusRecorder, session);
        
        session.open(getActivity(), null);
        
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);
        assertNotNull("Session should be open", tracker.getOpenSession());
        assertEquals("Access Token check", TOKEN_STR, tracker.getOpenSession().getAccessToken());
        tracker.getOpenSession().close();
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);
        
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        tracker.stopTracking();
        statusRecorder.close();
    }
    
    @SmallTest
    @MediumTest
    @LargeTest
    // Tests the SessionDelegate while tracking the active session and then a new session
    public void testDelegateWithActiveSessionThenNewSession() throws Exception {
        Session.setActiveSession(null);
        final SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        final MockTokenCache cache = new MockTokenCache(null, 0);
        final ScriptedSession session = createScriptedSessionOnBlockerThread(cache);

        final AccessToken openToken = AccessToken.createFromString(TOKEN_STR, 
                Collections.<String>emptyList());
        session.addAuthorizeResult(openToken);

        SessionTracker tracker = new SessionTracker(getActivity(), statusRecorder);
        Session.setActiveSession(session); 
        
        session.open(getActivity(), null);
        
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);
        assertNotNull("Session should be open", tracker.getOpenSession());
        assertEquals("Access Token check", TOKEN_STR, tracker.getOpenSession().getAccessToken());
        tracker.getOpenSession().close();
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);
        
        final ScriptedSession newSession = createScriptedSessionOnBlockerThread(cache);
        newSession.addAuthorizeResult(openToken);
        
        tracker.setSession(newSession);
        assertNull("Session should not be open", tracker.getOpenSession());
        newSession.open(getActivity(), null);
        
        statusRecorder.waitForCall(newSession, SessionState.OPENING, null);
        statusRecorder.waitForCall(newSession, SessionState.OPENED, null);
        assertNotNull("Session should be open", tracker.getOpenSession());
        assertEquals("Access Token check", TOKEN_STR, tracker.getOpenSession().getAccessToken());
        tracker.getOpenSession().close();
        statusRecorder.waitForCall(newSession, SessionState.CLOSED, null);
        
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        tracker.stopTracking();
        statusRecorder.close();
    }
    
    @SmallTest
    @MediumTest
    @LargeTest
    // Tests the SessionDelegate while tracking a new session and then an active session
    public void testDelegateWithSessionThenActiveSession() throws Exception {
        Session.setActiveSession(null);
        final SessionStatusCallbackRecorder statusRecorder = new SessionStatusCallbackRecorder();
        final MockTokenCache cache = new MockTokenCache(null, 0);
        final ScriptedSession session = createScriptedSessionOnBlockerThread(cache);

        final AccessToken openToken = AccessToken.createFromString(TOKEN_STR, 
                Collections.<String>emptyList());
        session.addAuthorizeResult(openToken);

        final SessionTracker tracker = new SessionTracker(getActivity(), statusRecorder, session);
        
        session.open(getActivity(), null);
        
        statusRecorder.waitForCall(session, SessionState.OPENING, null);
        statusRecorder.waitForCall(session, SessionState.OPENED, null);
        assertNotNull("Session should be open", tracker.getOpenSession());
        assertEquals("Access Token check", TOKEN_STR, tracker.getOpenSession().getAccessToken());
        tracker.getOpenSession().close();
        statusRecorder.waitForCall(session, SessionState.CLOSED, null);
        
        final ScriptedSession newSession = createScriptedSessionOnBlockerThread(cache);
        newSession.addAuthorizeResult(openToken);
        
        // need to run on the blocker thread so that when we register the 
        // BroadcastReceivers, the handler gets run on the right thread
        runOnBlockerThread(new Runnable() {
            public void run() {
                tracker.setSession(null);
                Session.setActiveSession(newSession);
            }
        }, true);
        
        assertNull("Session should not be open", tracker.getOpenSession());
        newSession.open(getActivity(), null);
        
        statusRecorder.waitForCall(newSession, SessionState.OPENING, null);
        statusRecorder.waitForCall(newSession, SessionState.OPENED, null);
        assertNotNull("Session should be open", tracker.getOpenSession());
        assertEquals("Access Token check", TOKEN_STR, tracker.getOpenSession().getAccessToken());
        tracker.getOpenSession().close();
        statusRecorder.waitForCall(newSession, SessionState.CLOSED, null);
        
        stall(STRAY_CALLBACK_WAIT_MILLISECONDS);
        tracker.stopTracking();
        statusRecorder.close();
    }
}
