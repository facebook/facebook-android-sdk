package com.facebook.samples.switchuser;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import com.facebook.*;

public class MainActivity extends FacebookActivity {

    private static final String TAG = "MainActivity";
    private static final String SHOWING_SETTINGS_KEY = "Showing settings";

    private ProfileFragment profileFragment;
    private SettingsFragment settingsFragment;
    private boolean isShowingSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        restoreFragments(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.getBoolean(SHOWING_SETTINGS_KEY)) {
            showSettings();
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
    protected void onStart() {
        super.onStart();

        settingsFragment.setSlotChangedListener(new SettingsFragment.OnSlotChangedListener() {
            @Override
            public void OnSlotChanged(Slot newSlot) {
                handleSlotChange(newSlot);
            }
        });

        profileFragment.setOnOptionsItemSelectedListener(new ProfileFragment.OnOptionsItemSelectedListener() {
            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                return handleOptionsItemSelected(item);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        settingsFragment.setSlotChangedListener(null);
        profileFragment.setOnOptionsItemSelectedListener(null);
    }

    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
        if (state.isOpened()) {
            // Log in just happened.
            fetchUserInfo();
        } else if (state.isClosed()) {
            // Log out just happened. Update the UI.
            updateFragments(null);
        }
    }

    private void restoreFragments(Bundle savedInstanceState) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        if (savedInstanceState != null) {
            profileFragment = (ProfileFragment)manager.getFragment(savedInstanceState, ProfileFragment.TAG);
            settingsFragment = (SettingsFragment)manager.getFragment(savedInstanceState, SettingsFragment.TAG);
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

    private void fetchUserInfo() {
        Session currentSession = getSession();
        if (currentSession != null && currentSession.isOpened()) {
            Request request = Request.newMeRequest(currentSession, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    if (response.getRequest().getSession() == getSession()) {
                        GraphUser user = response.getGraphObjectAs(GraphUser.class);
                        updateFragments(user);
                    }
                }
            });
            Request.executeBatchAsync(request);
        }
    }

    private void handleSlotChange(Slot newSlot) {
        closeSession();

        if (newSlot != null) {
            Session newSession = newSlot.createSession(this);
            setSession(newSession);
            openSession(null, null, newSlot.getLoginBehavior(),
                    Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
        }
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

    private void updateFragments(GraphUser user) {
        settingsFragment.updateViewForUser(user);
        profileFragment.updateViewForUser(user);
    }
}
