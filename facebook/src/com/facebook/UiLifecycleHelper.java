/**
 * Copyright 2010-present Facebook.
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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

/**
 * This class helps to create, automatically open (if applicable), save, and
 * restore the Active Session in a way that is similar to Android UI lifecycles.
 * <p>
 * When using this class, clients MUST call all the public methods from the
 * respective methods in either an Activity or Fragment. Failure to call all the
 * methods can result in improperly initialized or uninitialized Sessions.
 */
public class UiLifecycleHelper {

    private final static String ACTIVITY_NULL_MESSAGE = "activity cannot be null";

    private final Activity activity;
    private final Session.StatusCallback callback;
    private final BroadcastReceiver receiver;
    private final LocalBroadcastManager broadcastManager;

    /**
     * Creates a new UiLifecycleHelper.
     *
     * @param activity the Activity associated with the helper. If calling from a Fragment,
     *                 use {@link android.support.v4.app.Fragment#getActivity()}
     * @param callback the callback for Session status changes, can be null
     */
    public UiLifecycleHelper(Activity activity, Session.StatusCallback callback) {
        if (activity == null) {
            throw new IllegalArgumentException(ACTIVITY_NULL_MESSAGE);
        }
        this.activity = activity;
        this.callback = callback;
        this.receiver = new ActiveSessionBroadcastReceiver();
        this.broadcastManager = LocalBroadcastManager.getInstance(activity);
    }

    /**
     * To be called from an Activity or Fragment's onCreate method.
     *
     * @param savedInstanceState the previously saved state
     */
    public void onCreate(Bundle savedInstanceState) {
        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(activity, null, callback, savedInstanceState);
            }
            if (session == null) {
                session = new Session(activity);
            }
            Session.setActiveSession(session);
        }
    }

    /**
     * To be called from an Activity or Fragment's onResume method.
     */
    public void onResume() {
        Session session = Session.getActiveSession();
        if (session != null) {
            if (callback != null) {
                session.addCallback(callback);
            }
            if (SessionState.CREATED_TOKEN_LOADED.equals(session.getState())) {
                session.openForRead(null);
            }
        }

        // add the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Session.ACTION_ACTIVE_SESSION_SET);
        filter.addAction(Session.ACTION_ACTIVE_SESSION_UNSET);

        // Add a broadcast receiver to listen to when the active Session
        // is set or unset, and add/remove our callback as appropriate
        broadcastManager.registerReceiver(receiver, filter);
    }

    /**
     * To be called from an Activity or Fragment's onActivityResult method.
     *
     * @param requestCode the request code
     * @param resultCode the result code
     * @param data the result data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Session session = Session.getActiveSession();
        if (session != null) {
            session.onActivityResult(activity, requestCode, resultCode, data);
        }
    }

    /**
     * To be called from an Activity or Fragment's onSaveInstanceState method.
     *
     * @param outState the bundle to save state in
     */
    public void onSaveInstanceState(Bundle outState) {
        Session.saveSession(Session.getActiveSession(), outState);
    }

    /**
     * To be called from an Activity or Fragment's onPause method.
     */
    public void onPause() {
        // remove the broadcast receiver
        broadcastManager.unregisterReceiver(receiver);

        if (callback != null) {
            Session session = Session.getActiveSession();
            if (session != null) {
                session.removeCallback(callback);
            }
        }
    }

    /**
     * To be called from an Activity or Fragment's onDestroy method.
     */
    public void onDestroy() {
    }

    /**
     * The BroadcastReceiver implementation that either adds or removes the callback
     * from the active Session object as it's SET or UNSET.
     */
    private class ActiveSessionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Session.ACTION_ACTIVE_SESSION_SET.equals(intent.getAction())) {
                Session session = Session.getActiveSession();
                if (session != null && callback != null) {
                    session.addCallback(callback);
                }
            } else if (Session.ACTION_ACTIVE_SESSION_UNSET.equals(intent.getAction())) {
                Session session = Session.getActiveSession();
                if (session != null && callback != null) {
                    session.removeCallback(callback);
                }
            }
        }
    }
}
