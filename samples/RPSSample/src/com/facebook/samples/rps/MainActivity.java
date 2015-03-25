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

package com.facebook.samples.rps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import bolts.AppLinks;
import com.facebook.*;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.widget.GameRequestDialog;

import static com.facebook.samples.rps.RpsGameUtils.INVALID_CHOICE;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";

    static final int RPS = 0;
    static final int SETTINGS = 1;
    static final int CONTENT = 2;
    static final int FRAGMENT_COUNT = CONTENT +1;

    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
    private MenuItem settings;
    private MenuItem challenge;
    private MenuItem share;
    private MenuItem message;
    private MenuItem invite;
    private boolean isResumed = false;
    private boolean hasNativeLink = false;
    private CallbackManager callbackManager;
    private GameRequestDialog gameRequestDialog;

    private AccessTokenTracker accessTokenTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(this.getApplicationContext());

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {
                if (isResumed) {
                    if (currentAccessToken == null) {
                        showFragment(RPS, false);
                    }
                }
            }
        };

        setContentView(R.layout.main);

        FragmentManager fm = getSupportFragmentManager();
        fragments[RPS] = fm.findFragmentById(R.id.rps_fragment);
        fragments[SETTINGS] = fm.findFragmentById(R.id.settings_fragment);
        fragments[CONTENT] = fm.findFragmentById(R.id.content_fragment);

        FragmentTransaction transaction = fm.beginTransaction();
        for(int i = 0; i < fragments.length; i++) {
            transaction.hide(fragments[i]);
        }
        transaction.commit();

        hasNativeLink = handleNativeLink();

        gameRequestDialog = new GameRequestDialog(this);
        callbackManager = CallbackManager.Factory.create();
        gameRequestDialog.registerCallback(
                callbackManager,
                new FacebookCallback<GameRequestDialog.Result>() {
                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Canceled");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, String.format("Error: %s", error.toString()));
                    }

                    @Override
                    public void onSuccess(GameRequestDialog.Result result) {
                        Log.d(TAG, "Success!");
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;

        // Call the 'deactivateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onPause methods of the primary Activities that an app may be launched into.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        if (hasNativeLink) {
            showFragment(CONTENT, false);
            hasNativeLink = false;
        } else {
            showFragment(RPS, false);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // only add the menu when the selection fragment is showing
        if (fragments[RPS].isVisible()) {
            if (menu.size() == 0) {
                share = menu.add(R.string.share_on_facebook);
                message = menu.add(R.string.send_with_messenger);
                challenge = menu.add(R.string.challenge_friends);
                settings = menu.add(R.string.check_settings);
                invite = menu.add(R.string.invite_friends);
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
            showFragment(SETTINGS, true);
            return true;
        } else if (item.equals(challenge)) {
            GameRequestContent newGameRequestContent = new GameRequestContent.Builder()
                    .setTitle(getString(R.string.challenge_dialog_title))
                    .setMessage(getString(R.string.challenge_dialog_message))
                    .build();

            gameRequestDialog.show(this, newGameRequestContent);

            return true;
        } else if (item.equals(share)) {
            RpsFragment fragment = (RpsFragment) fragments[RPS];
            fragment.shareUsingNativeDialog();
            return true;
        } else if (item.equals(message)) {
            RpsFragment fragment = (RpsFragment) fragments[RPS];
            fragment.shareUsingMessengerDialog();
            return true;
        } else if (item.equals(invite)) {
            RpsFragment fragment = (RpsFragment) fragments[RPS];
            fragment.presentAppInviteDialog();
        }
        return false;
    }

    private boolean handleNativeLink() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) {
            AccessToken.createFromNativeLinkingIntent(getIntent(),
                    FacebookSdk.getApplicationId(), new AccessToken.AccessTokenCreationCallback(){

                        @Override
                        public void onSuccess(AccessToken token) {
                            AccessToken.setCurrentAccessToken(token);
                        }

                        @Override
                        public void onError(FacebookException error) {

                        }
                    });
        }
        // See if we have a deep link in addition.
        int appLinkGesture = getAppLinkGesture(getIntent());
        if (appLinkGesture != INVALID_CHOICE) {
            ContentFragment fragment = (ContentFragment) fragments[CONTENT];
            fragment.setContentIndex(appLinkGesture);
            return true;
        }
        return false;
    }

    private int getAppLinkGesture(Intent intent) {
      Uri targetURI = AppLinks.getTargetUrlFromInboundIntent(this, intent);
      if (targetURI == null) {
        return INVALID_CHOICE;
      }
      String gesture = targetURI.getQueryParameter("gesture");
      if (gesture != null) {
          if (gesture.equalsIgnoreCase(getString(R.string.rock))) {
              return RpsGameUtils.ROCK;
          } else if (gesture.equalsIgnoreCase(getString(R.string.paper))) {
              return RpsGameUtils.PAPER;
          } else if (gesture.equalsIgnoreCase(getString(R.string.scissors))) {
              return RpsGameUtils.SCISSORS;
          }
      }
      return INVALID_CHOICE;
    }

    void showFragment(int fragmentIndex, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (addToBackStack) {
            transaction.addToBackStack(null);
        } else {
            int backStackSize = fm.getBackStackEntryCount();
            for (int i = 0; i < backStackSize; i++) {
                fm.popBackStack();
            }
        }
        for (int i = 0; i < fragments.length; i++) {
            if (i == fragmentIndex) {
                transaction.show(fragments[i]);
            } else {
                transaction.hide(fragments[i]);
            }
        }
        transaction.commit();
    }
}
