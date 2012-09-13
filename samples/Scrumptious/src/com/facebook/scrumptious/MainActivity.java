package com.facebook.scrumptious;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;
import com.facebook.FacebookActivity;
import com.facebook.LoginFragment;
import com.facebook.Session;
import com.facebook.SessionState;

import java.util.Arrays;

public class MainActivity extends FacebookActivity {

    private static final int SPLASH = 0;
    private static final int SELECTION = 1;
    private static final int SETTINGS = 2;
    private static final int FRAGMENT_COUNT = SETTINGS +1;
    private static final String FRAGMENT_PREFIX = "fragment";
    private static final String TAG = "Scrumplicious";

    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
    private MenuItem settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        for(int i = 0; i < fragments.length; i++) {
            restoreFragment(savedInstanceState, i);
        }

        String[] permissions = getResources().getStringArray(R.array.permissions);
        Session session = new Session(this, null, Arrays.asList(permissions), null);
        Session.setActiveSession(session);

        // If we already have a valid token, then we can just open the session silently,
        // otherwise present the splash screen and ask the user to login.
        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            // no need to add any fragments here since it will be handled in onSessionStateChange
            session.open(this, null);
        } else {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.body_frame, fragments[SPLASH]).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        settings = menu.add(R.string.settings);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.equals(settings)) {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.body_frame, fragments[SETTINGS]).addToBackStack(null).commit();
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
        if (state.getIsOpened()) {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.body_frame, fragments[SELECTION]).commit();
        } else if (state.getIsClosed()) {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.body_frame, fragments[SPLASH]).commit();
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
