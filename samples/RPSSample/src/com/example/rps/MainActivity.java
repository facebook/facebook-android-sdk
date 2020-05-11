/*
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

package com.example.rps;

import static com.example.rps.RpsGameUtils.INVALID_CHOICE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.facebook.*;
import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.applinks.AppLinkData;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.widget.GameRequestDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  private static final String INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";

  static final int RPS = 0;
  static final int SETTINGS = 1;
  static final int CONTENT = 2;
  static final int AUTOAPPLINK = 3;
  static final int FRAGMENT_COUNT = 4;

  private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
  private MenuItem settings;
  private MenuItem challenge;
  private MenuItem message;
  private boolean isResumed = false;
  private boolean hasNativeLink = false;
  private CallbackManager callbackManager;
  private GameRequestDialog gameRequestDialog;

  private AccessTokenTracker accessTokenTracker;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
    FacebookSdk.setIsDebugEnabled(true);

    super.onCreate(savedInstanceState);
    accessTokenTracker =
        new AccessTokenTracker() {
          @Override
          protected void onCurrentAccessTokenChanged(
              AccessToken oldAccessToken, AccessToken currentAccessToken) {
            if (isResumed) {
              if (currentAccessToken == null) {
                showFragment(RPS, false);
              }
            }
          }
        };

    setContentView(R.layout.main);

    // Navigation bottom view
    BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
    bottomNav.setOnNavigationItemSelectedListener(
        new BottomNavigationView.OnNavigationItemSelectedListener() {
          @Override
          public boolean onNavigationItemSelected(MenuItem item) {

            if (item.getItemId() == R.id.nav_game) {
              showFragment(RPS, false);
            } else if (item.getItemId() == R.id.nav_auto_applink_debug_tool) {
              showFragment(AUTOAPPLINK, false);
            }

            return true;
          }
        });

    FragmentManager fm = getSupportFragmentManager();
    fragments[RPS] = fm.findFragmentById(R.id.rps_fragment);
    fragments[SETTINGS] = fm.findFragmentById(R.id.settings_fragment);
    fragments[CONTENT] = fm.findFragmentById(R.id.content_fragment);
    fragments[AUTOAPPLINK] = fm.findFragmentById(R.id.auto_applink_fragment);

    FragmentTransaction transaction = fm.beginTransaction();
    for (int i = 0; i < fragments.length; i++) {
      transaction.hide(fragments[i]);
    }
    transaction.commit();

    AppLinkData appLinkData = AppLinkData.createFromActivity(this);
    if (appLinkData != null && appLinkData.isAutoAppLink()) {
      String productId = appLinkData.getAppLinkData().optString("product_id", "");
      navigateToProductDetails(productId);
    }

    // We handle with other deep links
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
            Log.d(TAG, "Request id: " + result.getRequestId());
            Log.d(TAG, "Recipients:");
            for (String recipient : result.getRequestRecipients()) {
              Log.d(TAG, recipient);
            }
          }
        });
  }

  private void navigateToProductDetails(String productId) {
    Intent intent = new Intent(this, AutoApplinkDemoActivity.class);
    intent.putExtra("product_id", productId);
    startActivity(intent);
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
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    callbackManager.onActivityResult(requestCode, resultCode, data);
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RpsFragment.IN_APP_PURCHASE_RESULT) {
      String purchaseData = data.getStringExtra(INAPP_PURCHASE_DATA);

      if (resultCode == RESULT_OK) {
        RpsFragment fragment = (RpsFragment) fragments[RPS];
        try {
          JSONObject jo = new JSONObject(purchaseData);
          fragment.onInAppPurchaseSuccess(jo);
        } catch (JSONException e) {
          Log.e(TAG, "In app purchase invalid json.", e);
        }
      }
    }
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
        message = menu.add(R.string.send_with_messenger);
        challenge = menu.add(R.string.challenge_friends);
        settings = menu.add(R.string.check_settings);
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
      GameRequestContent newGameRequestContent =
          new GameRequestContent.Builder()
              .setTitle(getString(R.string.challenge_dialog_title))
              .setMessage(getString(R.string.challenge_dialog_message))
              .build();

      gameRequestDialog.show(this, newGameRequestContent);

      return true;
    } else if (item.equals(message)) {
      RpsFragment fragment = (RpsFragment) fragments[RPS];
      fragment.shareUsingMessengerDialog();
      return true;
    }
    return false;
  }

  private boolean handleNativeLink() {
    if (!AccessToken.isCurrentAccessTokenActive()) {
      AccessToken.createFromNativeLinkingIntent(
          getIntent(),
          FacebookSdk.getApplicationId(),
          new AccessToken.AccessTokenCreationCallback() {

            @Override
            public void onSuccess(AccessToken token) {
              AccessToken.setCurrentAccessToken(token);
            }

            @Override
            public void onError(FacebookException error) {}
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
    Uri targetURI = null;

    Bundle appLinkData = intent.getBundleExtra("al_applink_data");
    if (appLinkData != null) {
      String targetString = appLinkData.getString("target_url");
      if (targetString != null) {
        targetURI = Uri.parse(targetString);
      }
    }

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
