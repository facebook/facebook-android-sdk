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
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;

/**
 * <p>Basic implementation of an Activity that uses a Session to perform 
 * Single Sign On (SSO).</p>
 * 
 * <p>The method {@link android.app.Activity#onActivityResult} is used to 
 * manage the session information, so if you override it in a subclass, 
 * be sure to call {@code super.onActivityResult}.</p>
 * 
 * <p>The methods in this class are not thread-safe</p>
 */
public class FacebookActivity extends Activity {

    private Session session;
    private SessionStatusCallback callback = new DefaultSessionStatusCallback();
    
    /**
     * Called when the activity that was launched exits. This method manages session
     * information when a session is opened. If this method is overridden in subclasses,
     * be sure to call {@code super.onActivityResult(...)} first.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        session.onActivityResult(this, requestCode, resultCode, data);
    }

    // METHOD TO BE OVERRIDDEN
    
    /**
     * Called when the session state changes. Override this method to take action
     * on session state changes.
     * 
     * @param state the new state
     * @param exception any exceptions that occurred during the state change
     */
    protected void onSessionStateChange(SessionState state, Exception exception) {
    }

    // ACCESSORS (CANNOT BE OVERRIDDEN)

    /**
     * Determines whether the current session is open.
     * 
     * @return true if the current session is open
     */
    protected final boolean isSessionOpen() {
        return (session == null) ? false : session.getIsOpened();
    }
    
    /**
     * Gets the current state of the session or null if no session has been created.
     * 
     * @return the current state of the session
     */
    protected final SessionState getSessionState() {
        return (session == null) ? null : session.getState();
    }
    
    /**
     * Gets the access token associated with the current session or null if no 
     * session has been created.
     * 
     * @return the access token
     */
    protected final String getAccessToken() {
        return (session == null) ? null : session.getAccessToken();
    }

    /**
     * Gets the date at which the current session will expire or null if no session 
     * has been created.
     * 
     * @return the date at which the current session will expire
     */
    protected final Date getExpirationDate() {
        return (session == null) ? null : session.getExpirationDate();
    }
    
    /**
     * Closes the current session.
     */
    protected final void closeSession() {
        if (session != null) {
            session.close();
        }
    }
    
    /**
     * Closes the current session as well as clearing the token cache.
     */
    protected final void closeSessionAndClearTokenInformation() {
        if (session != null) {
            session.closeAndClearTokenInformation();
        }
    }
    
    /**
     * Gets the permissions associated with the current session or null if no session 
     * has been created.
     * 
     * @return the permissions associated with the current session
     */
    protected final List<String> getSessionPermissions() {
        return (session == null) ? null : session.getPermissions();
    }

    /**
     * Opens a new session. This method will use the application id from
     * the associated meta-data value and an empty list of permissions.
     */
    protected final void openSession() {
        openSession(null, null);
    }
    
    /**
     * Opens a new session. If either applicationID or permissions is null, 
     * this method will default to using the values from the associated 
     * meta-data value and an empty list respectively.
     * 
     * @param applicationId the applicationID, can be null
     * @param permissions the permissions list, can be null
     */
    protected final void openSession(String applicationId, List<String> permissions) {
        session = Session.sessionOpen(this, applicationId, permissions, callback);
    }
    
    /**
     * Opens a new session. If either applicationID or permissions is null,
     * this method will default to using the values from the associated 
     * meta-data value and an empty list respectively.
     * 
     * @param applicationId the applicationID, can be null
     * @param permissions the permissions list, can be null
     * @param behavior the login behavior to use with the session
     * @param activityCode the activity code to use for the SSO activity
     */
    protected final void openSession(String applicationId, List<String> permissions, 
            SessionLoginBehavior behavior, int activityCode) {
        session = Session.sessionOpen(this, applicationId, permissions, callback, behavior, activityCode);
    }

    /**
     * The default callback implementation for the session.
     */
    private class DefaultSessionStatusCallback implements SessionStatusCallback {

        @Override
        public void call(Session session, 
                         SessionState state,
                         Exception exception) {
            FacebookActivity.this.onSessionStateChange(state, exception);
        }
        
    }
}