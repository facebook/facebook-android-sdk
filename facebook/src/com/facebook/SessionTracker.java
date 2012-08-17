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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * A Session tracker that tracks either the active Session or the
 * passed in Session object. This class is not thread safe.
 */
class SessionTracker {

    private Session session;
    private final Session.StatusCallback callback;
    private final BroadcastReceiver receiver;
    
    /**
     * Constructs a SessionTracker to track the active Session object.
     * 
     * @param callback the callback to use whenever the active Session's 
     *                 state changes
     */
    SessionTracker(Session.StatusCallback callback) {
        this(callback, null);
    }
    
    /**
     * Constructs a SessionTracker to track the Session object passed in.
     * If the Session is null, then it will track the active Session instead.
     * 
     * @param callback the callback to use whenever the session's state changes
     * @param session the Session object to track
     */
    SessionTracker(Session.StatusCallback callback, Session session) {
        this.callback = callback;
        this.session = session;
        this.receiver = new ActiveSessionBroadcastReceiver();
        
        if (this.session == null) {
            addBroadcastReceiver();
        }
        
        // if the session is not null, then add the callback to it right away
        if (getSession() != null) {
            getSession().addCallback(callback);
        }
    }

    /**
     * Returns the current Session that's being tracked.
     * 
     * @return the current Session associated with this tracker
     */
    Session getSession() {
        return (session == null) ? Session.getActiveSession() : session;
    }

    /**
     * Returns the current Session that's being tracked if it's open, 
     * otherwise returns null.
     * 
     * @return the current Session if it's open, otherwise returns null
     */
    Session getOpenSession() {
        Session openSession = getSession();
        if (openSession != null && openSession.getIsOpened()) {
            return openSession;
        }
        return null;
    }

    /**
     * Set the Session object to track.
     * 
     * @param newSession the new Session object to track
     */
    void setSession(Session newSession) {
        if (newSession == null) {
            if (session != null) {
                // We're current tracking a Session. Remove the callback
                // and start tracking the active Session.
                session.removeCallback(callback);
                session = null;
                addBroadcastReceiver();
                if (getSession() != null) {
                    getSession().addCallback(callback);
                }
            }
        } else {
            if (session == null) {
                // We're currently tracking the active Session, but will be
                // switching to tracking a different Session object.
                Session activeSession = Session.getActiveSession();
                if (activeSession != null) {
                    activeSession.removeCallback(callback);
                }
                Session.unregisterActiveSessionReceiver(receiver);  
            } else {
                // We're currently tracking a Session, but are now switching 
                // to a new Session, so we remove the callback from the old 
                // Session, and add it to the new one.
                session.removeCallback(callback);  
            }
            session = newSession;
            session.addCallback(callback);
        }
    }
    
    private void addBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Session.ACTION_ACTIVE_SESSION_SET);
        filter.addAction(Session.ACTION_ACTIVE_SESSION_UNSET);
        
        // Add a broadcast receiver to listen to when the active Session
        // is set or unset, and add/remove our callback as appropriate        
        Session.registerActiveSessionReceiver(receiver, filter);
    }

    /**
     * The BroadcastReceiver implementation that either adds or removes the callback
     * from the active Session object as it's SET or UNSET.
     */
    private class ActiveSessionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Session.ACTION_ACTIVE_SESSION_SET.equals(intent.getAction())) {
                Session.getActiveSession().addCallback(SessionTracker.this.callback);
            } else if (Session.ACTION_ACTIVE_SESSION_UNSET.equals(intent.getAction())) {
                Session.getActiveSession().removeCallback(SessionTracker.this.callback);
            }
        }
    }
        
}
