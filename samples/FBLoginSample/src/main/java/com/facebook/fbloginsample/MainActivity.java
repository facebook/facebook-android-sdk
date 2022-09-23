/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.facebook.AccessToken;
import com.facebook.LoginStatusCallback;
import com.facebook.login.LoginManager;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends Activity {

  private static final int RESULT_PROFILE_ACTIVITY = 1;
  private static final int RESULT_POSTS_ACTIVITY = 2;
  private static final int RESULT_PERMISSIONS_ACTIVITY = 3;

  private static final String DEFAULT_FB_APP_ID = "ENTER_YOUR_FB_APP_ID_HERE";
  private static final String DEFAULT_FB_CLIENT_COKEN = "ENTER_YOUR_FB_CLIENT_TOKEN_HERE";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (getResources().getString(R.string.facebook_app_id).equals(DEFAULT_FB_APP_ID)) {
      showAlertNoFacebookAppId();
      return;
    }
    if (getResources().getString(R.string.facebook_client_token).equals(DEFAULT_FB_CLIENT_COKEN)) {
      showAlertNoFacebookClientToken();
      return;
    }
    final View view = findViewById(R.id.activity_main);
    // User was previously logged in, can log them in directly here.
    // If this callback is called, a popup notification appears
    LoginManager.getInstance()
        .retrieveLoginStatus(
            this,
            new LoginStatusCallback() {
              @Override
              public void onCompleted(AccessToken accessToken) {
                Snackbar snackbar = Snackbar.make(view, "User Logged in", 2);
                snackbar.show();
              }

              @Override
              public void onFailure() {
                // If MainActivity is reached without the user being logged in,
                // redirect to the Login Activity
                if (AccessToken.getCurrentAccessToken() == null) {
                  Intent loginIntent = new Intent(MainActivity.this, FacebookLoginActivity.class);
                  startActivity(loginIntent);
                }
              }

              @Override
              public void onError(Exception exception) {
                // Handle exception
              }
            });

    // Make a button which leads to profile information of the user
    Button gotoProfileButton = findViewById(R.id.btn_profile);
    gotoProfileButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (AccessToken.getCurrentAccessToken() == null) {
              Intent profileIntent = new Intent(MainActivity.this, FacebookLoginActivity.class);
              startActivityForResult(profileIntent, RESULT_PROFILE_ACTIVITY);
            } else {
              Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
              startActivity(profileIntent);
            }
          }
        });

    // Make a button which leads to posts made by the user
    Button gotoPostsFeedButton = findViewById(R.id.btn_posts);
    gotoPostsFeedButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (AccessToken.getCurrentAccessToken() == null) {
              Intent loginIntent = new Intent(MainActivity.this, FacebookLoginActivity.class);
              startActivityForResult(loginIntent, RESULT_POSTS_ACTIVITY);
            } else {
              Intent postsFeedIntent = new Intent(MainActivity.this, PostFeedActivity.class);
              startActivity(postsFeedIntent);
            }
          }
        });

    // Make a button which leads to request or revoke permissions
    Button gotoPermissionsButton = findViewById(R.id.btn_permissions);
    gotoPermissionsButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (AccessToken.getCurrentAccessToken() == null) {
              Intent loginIntent = new Intent(MainActivity.this, FacebookLoginActivity.class);
              startActivityForResult(loginIntent, RESULT_PERMISSIONS_ACTIVITY);
            } else {
              Intent permissionsIntent = new Intent(MainActivity.this, PermissionsActivity.class);
              startActivity(permissionsIntent);
            }
          }
        });

    // Make a logout button
    Button fbLogoutButton = findViewById(R.id.btn_fb_logout);
    fbLogoutButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            LoginManager.getInstance().logOut();
            Intent loginIntent = new Intent(MainActivity.this, FacebookLoginActivity.class);
            startActivity(loginIntent);
          }
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case RESULT_PROFILE_ACTIVITY:
        if (resultCode == RESULT_OK) {
          Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
          startActivity(profileIntent);
        }
        break;
      case RESULT_POSTS_ACTIVITY:
        if (resultCode == RESULT_OK) {
          Intent postFeedIntent = new Intent(MainActivity.this, PostFeedActivity.class);
          startActivity(postFeedIntent);
        }
        break;
      case RESULT_PERMISSIONS_ACTIVITY:
        if (resultCode == RESULT_OK) {
          Intent permissionsIntent = new Intent(MainActivity.this, PermissionsActivity.class);
          startActivity(permissionsIntent);
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void showAlertNoFacebookAppId() {
    AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
    alert.setTitle("Use your facebook app id in strings.xml");
    alert.setMessage(
        "This sample app can not properly function without your app id. "
            + "Use your facebook app id in strings.xml. Check out https://developers.facebook.com/docs/android/getting-started/ for more info. "
            + "Restart the app after that");
    alert.show();
  }

  private void showAlertNoFacebookClientToken() {
    AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
    alert.setTitle("Use your facebook client token in strings.xml");
    alert.setMessage(
        "On and above Facebook SDK v13.0, all Graph API calls must have a valid client token."
            + "Add your client token to the facebook_client_token string in strings.xml. Check out https://developers.facebook.com/docs/android/getting-started#app_id for more info. "
            + "Restart the app after that");
    alert.show();
  }
}
