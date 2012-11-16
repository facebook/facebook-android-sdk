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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.facebook.internal.SessionAuthorizationType;
import com.facebook.internal.SessionTracker;

import java.util.Date;
import java.util.List;

/**
 * <p>Basic implementation of an Activity that uses a Session to perform 
 * Single Sign On (SSO).</p>
 * 
 * <p>Numerous Activity lifecycle methods are overridden in this class
 * to manage session information. If you override Activity lifecycle methods,
 * be sure to call the appropriate {@code super} method.
 * 
 * <p>The methods in this class are not thread-safe</p>
 */
public class FacebookActivity extends FragmentActivity {

    private static final String SESSION_IS_ACTIVE_KEY = "com.facebook.sdk.FacebookActivity.sessionIsActiveKey";

    private SessionTracker sessionTracker;

    /**
     * Initializes the state in FacebookActivity. This method will try to restore the Session
     * if one was saved. If the restored Session object was the active Session, it will also set
     * the restored Session as the active Session (unless there's currently an active Session
     * already set).
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session.StatusCallback callback = new DefaultSessionStatusCallback();
        sessionTracker = new SessionTracker(this, callback);
        if (savedInstanceState != null) {
            Session session = Session.restoreSession(this, null, callback, savedInstanceState);
            if (session != null) {
                if (savedInstanceState.getBoolean(SESSION_IS_ACTIVE_KEY)) {
                    if (Session.getActiveSession() == null) {
                        Session.setActiveSession(session);
                    }
                } else {
                    sessionTracker.setSession(session);
                }
            }
        }
    }

    /**
     * Called when the activity that was launched exits. This method manages session
     * information when a session is opened. If this method is overridden in subclasses,
     * be sure to call {@code super.onActivityResult(...)} first.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Session session = getSession();
        if (session != null) {
            session.onActivityResult(this, requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sessionTracker.stopTracking();
    }

    /**
     * This method will save the session state so that it can be restored during
     * onCreate.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session currentSession = sessionTracker.getSession();
        Session.saveSession(currentSession, outState);
        outState.putBoolean(SESSION_IS_ACTIVE_KEY, sessionTracker.isTrackingActiveSession());
    }

    // METHOD TO BE OVERRIDDEN
    
    /**
     * Called when the session state changes. Override this method to take action
     * on session state changes.
     * 
     * @param state the new Session state
     * @param exception any exceptions that occurred during the state change
     */
    protected void onSessionStateChange(SessionState state, Exception exception) {
    }

    /** 
     * Use the supplied Session object instead of the active Session.
     * 
     * @param newSession the Session object to use
     */
    protected void setSession(Session newSession) {
        sessionTracker.setSession(newSession);
    }

    // ACCESSORS (CANNOT BE OVERRIDDEN)

    /**
     * Gets the current session for this Activity
     *
     * @return the current session, or null if one has not been set.
     */
    protected final Session getSession() {
        return sessionTracker.getSession();
    }

    /**
     * Determines whether the current session is open.
     * 
     * @return true if the current session is open
     */
    protected final boolean isSessionOpen() {
        return sessionTracker.getOpenSession() != null;
    }
    
    /**
     * Gets the current state of the session or null if no session has been created.
     * 
     * @return the current state of the session
     */
    protected final SessionState getSessionState() {
        Session currentSession = sessionTracker.getSession();
        return (currentSession != null) ? currentSession.getState() : null;
    }
    
    /**
     * Gets the access token associated with the current session or null if no 
     * session has been created.
     * 
     * @return the access token
     */
    protected final String getAccessToken() {
        Session currentSession = sessionTracker.getOpenSession();
        return (currentSession != null) ? currentSession.getAccessToken() : null;
    }

    /**
     * Gets the date at which the current session will expire or null if no session 
     * has been created.
     * 
     * @return the date at which the current session will expire
     */
    protected final Date getExpirationDate() {
        Session currentSession = sessionTracker.getOpenSession();
        return (currentSession != null) ? currentSession.getExpirationDate() : null;
    }
    
    /**
     * Closes the current session.
     */
    protected final void closeSession() {
        Session currentSession = sessionTracker.getOpenSession();
        if (currentSession != null) {
            currentSession.close();
        }
    }
    
    /**
     * Closes the current session as well as clearing the token cache.
     */
    protected final void closeSessionAndClearTokenInformation() {
        Session currentSession = sessionTracker.getOpenSession();
        if (currentSession != null) {
            currentSession.closeAndClearTokenInformation();
        }
    }
    
    /**
     * Gets the permissions associated with the current session or null if no session 
     * has been created.
     * 
     * @return the permissions associated with the current session
     */
    protected final List<String> getSessionPermissions() {
        Session currentSession = sessionTracker.getSession();
        return (currentSession != null) ? currentSession.getPermissions() : null;
    }

    /**
     * Opens a new session. This method will use the application id from
     * the associated meta-data value and an empty list of permissions.
     * <p>
     * If no session exists for this Activity, or if the current session has been closed,
     * this will create a new Session object and set it as the active session. If a session
     * exists for this Activity but is not yet open, this will try to open the session.
     * If a session is already open for this Activity, this does nothing.
     * </p>
     */
    protected final void openSession() {
        openSessionForRead(null, null);
    }
    
    /**
     * Opens a new session with read permissions. If either applicationID or permissions
     * is null, this method will default to using the values from the associated
     * meta-data value and an empty list respectively.
     * <p>
     * If no session exists for this Activity, or if the current session has been closed,
     * this will create a new Session object and set it as the active session. If a session
     * exists for this Activity but is not yet open, this will try to open the session.
     * If a session is already open for this Activity, this does nothing.
     * </p>
     *
     * @param applicationId the applicationID, can be null
     * @param permissions the permissions list, can be null
     */
    protected final void openSessionForRead(String applicationId, List<String> permissions) {
        openSessionForRead(applicationId, permissions, SessionLoginBehavior.SSO_WITH_FALLBACK,
                Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
    }
    
    /**
     * Opens a new session with read permissions. If either applicationID or permissions
     * is null, this method will default to using the values from the associated
     * meta-data value and an empty list respectively.
     * <p>
     * If no session exists for this Activity, or if the current session has been closed,
     * this will create a new Session object and set it as the active session. If a session
     * exists for this Activity but is not yet open, this will try to open the session.
     * If a session is already open for this Activity, this does nothing.
     * </p>
     *
     * @param applicationId the applicationID, can be null
     * @param permissions the permissions list, can be null
     * @param behavior the login behavior to use with the session
     * @param activityCode the activity code to use for the SSO activity
     */
    protected final void openSessionForRead(String applicationId, List<String> permissions,
            SessionLoginBehavior behavior, int activityCode) {
        openSession(applicationId, permissions, behavior, activityCode, SessionAuthorizationType.READ);
    }

    /**
     * Opens a new session with publish permissions. If the applicationID is null,
     * this method will default to using the value from the associated
     * meta-data value. The permissions list cannot be null.
     * <p>
     * If no session exists for this Activity, or if the current session has been closed,
     * this will create a new Session object and set it as the active session. If a session
     * exists for this Activity but is not yet open, this will try to open the session.
     * If a session is already open for this Activity, this does nothing.
     * </p>
     *
     * @param applicationId the applicationID, can be null
     * @param permissions the permissions list, cannot be null
     */
    protected final void openSessionForPublish(String applicationId, List<String> permissions) {
        openSessionForPublish(applicationId, permissions, SessionLoginBehavior.SSO_WITH_FALLBACK,
                Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
    }

    /**
     * Opens a new session with publish permissions. If the applicationID is null,
     * this method will default to using the value from the associated
     * meta-data value. The permissions list cannot be null.
     * <p>
     * If no session exists for this Activity, or if the current session has been closed,
     * this will create a new Session object and set it as the active session. If a session
     * exists for this Activity but is not yet open, this will try to open the session.
     * If a session is already open for this Activity, this does nothing.
     * </p>
     *
     * @param applicationId the applicationID, can be null
     * @param permissions the permissions list, cannot be null
     * @param behavior the login behavior to use with the session
     * @param activityCode the activity code to use for the SSO activity
     */
    protected final void openSessionForPublish(String applicationId, List<String> permissions,
            SessionLoginBehavior behavior, int activityCode) {
        openSession(applicationId, permissions, behavior, activityCode, SessionAuthorizationType.PUBLISH);
    }

    private void openSession(String applicationId, List<String> permissions,
            SessionLoginBehavior behavior, int activityCode, SessionAuthorizationType authType) {
        Session currentSession = sessionTracker.getSession();
        if (currentSession == null || currentSession.getState().isClosed()) {
            Session session = new Session.Builder(this).setApplicationId(applicationId).build();
            Session.setActiveSession(session);
            currentSession = session;
        }
        if (!currentSession.isOpened()) {
            Session.OpenRequest openRequest = new Session.OpenRequest(this).
                    setPermissions(permissions).
                    setLoginBehavior(behavior).
                    setRequestCode(activityCode);
            if (SessionAuthorizationType.PUBLISH.equals(authType)) {
                currentSession.openForPublish(openRequest);
            } else {
                currentSession.openForRead(openRequest);
            }
        }
    }

    /**
     * The default callback implementation for the session.
     */
    private class DefaultSessionStatusCallback implements Session.StatusCallback {

        @Override
        public void call(Session session, 
                         SessionState state,
                         Exception exception) {
            FacebookActivity.this.onSessionStateChange(state, exception);
        }
        
    }
}
