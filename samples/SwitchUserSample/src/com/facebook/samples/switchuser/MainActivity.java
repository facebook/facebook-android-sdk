/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.switchuser;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.facebook.*;
import com.facebook.appevents.AppEventsLogger;

public class MainActivity extends ActionBarActivity {

    private static final String SHOWING_SETTINGS_KEY = "Showing settings";

    private ProfileFragment profileFragment;
    private SettingsFragment settingsFragment;
    private boolean isShowingSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.main);

        restoreFragments(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(SHOWING_SETTINGS_KEY)) {
                showSettings();
            } else {
                showProfile();
            }
        } else {
            showProfile();
        }
    }

    @Override
    public void onBackPressed() {
        if (isShowingSettings()) {
            // This back is from the settings fragment
            showProfile();
        } else {
            // Allow the user to back out of the app as well.
            super.onBackPressed();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOWING_SETTINGS_KEY, isShowingSettings());

        FragmentManager manager = getSupportFragmentManager();
        manager.putFragment(outState, SettingsFragment.TAG, settingsFragment);
        manager.putFragment(outState, ProfileFragment.TAG, profileFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();

        profileFragment.setOnOptionsItemSelectedListener(new ProfileFragment.OnOptionsItemSelectedListener() {
            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                return handleOptionsItemSelected(item);
            }
        });

        // Call the 'activateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onResume methods of the primary Activities that an app may be
        // launched into.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        profileFragment.setOnOptionsItemSelectedListener(null);

        // Call the 'deactivateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onPause methods of the primary Activities that an app may be
        // launched into.
        AppEventsLogger.deactivateApp(this);
    }

    private void restoreFragments(Bundle savedInstanceState) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        if (savedInstanceState != null) {
            profileFragment = (ProfileFragment) manager.getFragment(savedInstanceState,
                    ProfileFragment.TAG);
            settingsFragment = (SettingsFragment) manager.getFragment(savedInstanceState,
                    SettingsFragment.TAG);
        }

        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
            transaction.add(R.id.fragmentContainer, profileFragment, ProfileFragment.TAG);
        }

        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
            transaction.add(R.id.fragmentContainer, settingsFragment, SettingsFragment.TAG);
        }

        transaction.commit();
    }

    private void showSettings() {
        isShowingSettings = true;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(profileFragment)
                .show(settingsFragment)
                .commit();
    }

    private boolean isShowingSettings() {
        return isShowingSettings;
    }

    private void showProfile() {
        isShowingSettings = false;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(settingsFragment)
                .show(profileFragment)
                .commit();
    }

    private boolean handleOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_switch:
                showSettings();
                return true;
            default:
                return false;
        }
    }
}
