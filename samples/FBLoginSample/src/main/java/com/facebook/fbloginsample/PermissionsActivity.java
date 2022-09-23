/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphResponse;
import com.facebook.fbloginsample.callbacks.PermissionCallback;
import com.facebook.fbloginsample.requests.PermissionRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import java.util.Arrays;
import java.util.Set;

public class PermissionsActivity extends Activity
    implements PermissionCallback.IPermissionResponse,
        AccessToken.AccessTokenRefreshCallback,
        FacebookCallback<LoginResult> {
  private static final String EMAIL = "email";
  private static final String USER_POSTS = "user_posts";
  private static final String PUBLISH_ACTIONS = "publish_actions";
  private static final String PUBLIC_PROFILE = "public_profile";
  private static final String APP = "app";

  private Switch mEmailPermSwitch;
  private Switch mUserPostsPermSwitch;
  private Switch mPublishPostPermSwitch;
  private Switch mAppPermSwitch;
  private CallbackManager mCallbackManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_permissions);
    mCallbackManager = CallbackManager.Factory.create();
    LoginManager.getInstance().registerCallback(mCallbackManager, this);

    // Make a switch for the email permission
    mEmailPermSwitch = findViewById(R.id.switch_email_permission);
    mEmailPermSwitch.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            Set<String> permissions = AccessToken.getCurrentAccessToken().getPermissions();
            if (isChecked && !permissions.contains(EMAIL)) {
              // Make request to user to grant email permission
              LoginManager.getInstance()
                  .logInWithReadPermissions(PermissionsActivity.this, Arrays.asList(EMAIL));
            } else if (!isChecked && permissions.contains(EMAIL)) {
              // Make revoke email permission request
              PermissionRequest.makeRevokePermRequest(
                  EMAIL, new PermissionCallback(PermissionsActivity.this).getCallback());
            }
          }
        });

    // Make a switch for the user posts permission
    mUserPostsPermSwitch = findViewById(R.id.switch_user_posts_permission);
    mUserPostsPermSwitch.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            Set<String> permissions = AccessToken.getCurrentAccessToken().getPermissions();
            if (isChecked && !permissions.contains(USER_POSTS)) {
              // Make request to user to grant user_posts permission
              LoginManager.getInstance()
                  .logInWithReadPermissions(PermissionsActivity.this, Arrays.asList(USER_POSTS));
            } else if (!isChecked && permissions.contains(USER_POSTS)) {
              // Make revoke user_posts permission request
              PermissionRequest.makeRevokePermRequest(
                  USER_POSTS, new PermissionCallback(PermissionsActivity.this).getCallback());
            }
          }
        });

    // Make a switch for the publish posts permission
    mPublishPostPermSwitch = findViewById(R.id.switch_publish_post_permission);
    mPublishPostPermSwitch.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            Set<String> permissions = AccessToken.getCurrentAccessToken().getPermissions();
            if (isChecked && !permissions.contains(PUBLISH_ACTIONS)) {
              // Make request to user to grant publish_actions permission
              LoginManager.getInstance()
                  .logInWithPublishPermissions(
                      PermissionsActivity.this, Arrays.asList(PUBLISH_ACTIONS));
            } else if (!isChecked && permissions.contains(PUBLISH_ACTIONS)) {
              // Make revoke publish_actions permission request
              PermissionRequest.makeRevokePermRequest(
                  PUBLISH_ACTIONS, new PermissionCallback(PermissionsActivity.this).getCallback());
            }
          }
        });

    // Make a switch for the app login permission
    mAppPermSwitch = findViewById(R.id.switch_app_permission);
    mAppPermSwitch.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked && AccessToken.getCurrentAccessToken() == null) {
              // Make request to user to login
              LoginManager.getInstance()
                  .logInWithReadPermissions(
                      PermissionsActivity.this, Arrays.asList(PUBLIC_PROFILE));
            } else if (!isChecked && AccessToken.getCurrentAccessToken() != null) {
              PermissionRequest.makeRevokePermRequest(
                  APP, new PermissionCallback(PermissionsActivity.this).getCallback());
            }
          }
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    mCallbackManager.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onResume() {
    super.onResume();
    setPermissionsSwitches();
  }

  // PermissionCallback.IPermissionResponse Callbacks
  @Override
  public void onCompleted(GraphResponse response) {
    if (response.getError() != null) {
      Toast.makeText(
              this,
              "Error with permissions request: " + response.getError().getErrorMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
    AccessToken.refreshCurrentAccessTokenAsync(PermissionsActivity.this);
  }

  // Facebook Login Callbacks
  @Override
  public void onSuccess(@NonNull LoginResult loginResult) {
    // Refresh token cached on device after login succeeds
    AccessToken.refreshCurrentAccessTokenAsync(this);
  }

  @Override
  public void onCancel() {
    // Handle user cancel ...
  }

  @Override
  public void onError(@NonNull FacebookException error) {
    // Handle exception ...
  }

  // Access Token Refresh Callbacks
  @Override
  public void OnTokenRefreshed(AccessToken accessToken) {
    if (accessToken == null) {
      LoginManager.getInstance().logOut();
      Intent loginIntent = new Intent(PermissionsActivity.this, FacebookLoginActivity.class);
      startActivity(loginIntent);
    } else {
      setPermissionsSwitches();
    }
  }

  @Override
  public void OnTokenRefreshFailed(FacebookException exception) {
    // Handle exception ...
  }

  // Set switch on/off according to current user-granted permissions
  private void setPermissionsSwitches() {
    Set<String> permissions = AccessToken.getCurrentAccessToken().getPermissions();

    if (permissions.contains(EMAIL)) {
      mEmailPermSwitch.setChecked(true);
    } else {
      mEmailPermSwitch.setChecked(false);
    }

    if (permissions.contains(USER_POSTS)) {
      mUserPostsPermSwitch.setChecked(true);
    } else {
      mUserPostsPermSwitch.setChecked(false);
    }

    if (permissions.contains(PUBLISH_ACTIONS)) {
      mPublishPostPermSwitch.setChecked(true);
    } else {
      mPublishPostPermSwitch.setChecked(false);
    }

    if (AccessToken.getCurrentAccessToken() != null) {
      mAppPermSwitch.setChecked(true);
    } else {
      mAppPermSwitch.setChecked(false);
    }
  }
}
