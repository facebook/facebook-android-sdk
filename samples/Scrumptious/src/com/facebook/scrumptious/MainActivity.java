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

package com.facebook.scrumptious;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.facebook.FacebookActivity;
import com.facebook.widget.LoginFragment;
import com.facebook.Session;
import com.facebook.SessionState;

public class MainActivity extends FacebookActivity {

    private static final int SPLASH = 0;
    private static final int SELECTION = 1;
    private static final int SETTINGS = 2;
    private static final int FRAGMENT_COUNT = SETTINGS +1;
    private static final String FRAGMENT_PREFIX = "fragment";
    private static final String TAG = "Scrumplicious";

    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
    private MenuItem settings;
    private boolean restoredFragment = false;
    private boolean isResumed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        for(int i = 0; i < fragments.length; i++) {
            restoreFragment(savedInstanceState, i);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        Session session = Session.getActiveSession();
        if (session == null || session.getState().isClosed()) {
            session = new Session(this);
            Session.setActiveSession(session);
        }

        FragmentManager manager = getSupportFragmentManager();

        if (restoredFragment) {
            return;
        }

        // If we already have a valid token, then we can just open the session silently,
        // otherwise present the splash screen and ask the user to login.
        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            // no need to add any fragments here since it will be handled in onSessionStateChange
            session.openForRead(this);
        } else if (session.isOpened()) {
            // if the session is already open, try to show the selection fragment
            Fragment fragment = manager.findFragmentById(R.id.body_frame);
            if (!(fragment instanceof SelectionFragment)) {
                manager.beginTransaction().replace(R.id.body_frame, fragments[SELECTION]).commit();
            }
        } else {
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.body_frame, fragments[SPLASH]).commit();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment currentFragment = manager.findFragmentById(R.id.body_frame);
        // only add the menu when the selection fragment is showing
        if (currentFragment == fragments[SELECTION]) {
            if (menu.size() == 0) {
                settings = menu.add(R.string.settings);
            }
            return true;
        } else {
            menu.clear();
            settings = null;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.equals(settings)) {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(R.id.body_frame, fragments[SETTINGS]).addToBackStack(null).commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        FragmentManager manager = getSupportFragmentManager();
        // Since we're only adding one Fragment at a time, we can only save one.
        Fragment f = manager.findFragmentById(R.id.body_frame);
        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i] == f) {
                manager.putFragment(outState, getBundleKey(i), fragments[i]);
            }
        }
    }

    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
        if (isResumed) {
            FragmentManager manager = getSupportFragmentManager();
            int backStackSize = manager.getBackStackEntryCount();
            for (int i = 0; i < backStackSize; i++) {
                manager.popBackStack();
            }
            if (state.isOpened()) {
                if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
                    ((SelectionFragment) fragments[SELECTION]).tokenUpdated();
                } else {
                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.replace(R.id.body_frame, fragments[SELECTION]).commit();
                }
            } else if (state.isClosed()) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.body_frame, fragments[SPLASH]).commit();
            }
        }
    }

    /**
     * Returns the key to be used when saving a Fragment to a Bundle.
     * @param index the index of the Fragment in the fragments array
     * @return the key to be used
     */
    private String getBundleKey(int index) {
        return FRAGMENT_PREFIX + Integer.toString(index);
    }

    /**
     * Restore fragments from the bundle. If a necessary Fragment cannot be found in the bundle,
     * a new instance will be created.
     *
     * @param savedInstanceState
     * @param fragmentIndex
     */
    private void restoreFragment(Bundle savedInstanceState, int fragmentIndex) {
        Fragment fragment = null;
        if (savedInstanceState != null) {
            FragmentManager manager = getSupportFragmentManager();
            fragment = manager.getFragment(savedInstanceState, getBundleKey(fragmentIndex));
        }
        if (fragment != null) {
            fragments[fragmentIndex] = fragment;
            restoredFragment = true;
        } else {
            switch (fragmentIndex) {
                case SPLASH:
                    fragments[SPLASH] = new SplashFragment();
                    break;
                case SELECTION:
                    fragments[SELECTION] = new SelectionFragment();
                    break;
                case SETTINGS:
                    fragments[SETTINGS] = new LoginFragment();
                    break;
                default:
                    Log.w(TAG, "invalid fragment index");
                    break;
            }
        }
    }

}
