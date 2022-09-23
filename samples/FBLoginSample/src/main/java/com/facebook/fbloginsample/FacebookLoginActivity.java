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
import androidx.annotation.NonNull;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import java.util.Arrays;

public class FacebookLoginActivity extends Activity {
  private static final String EMAIL = "email";
  private static final String USER_POSTS = "user_posts";
  private static final String AUTH_TYPE = "rerequest";

  private CallbackManager mCallbackManager;

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    mCallbackManager.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_facebook_login);
    mCallbackManager = CallbackManager.Factory.create();

    LoginButton mLoginButton = findViewById(R.id.login_button);

    // Set the initial permissions to request from the user while logging in
    mLoginButton.setPermissions(Arrays.asList(EMAIL, USER_POSTS));

    mLoginButton.setAuthType(AUTH_TYPE);

    // Register a callback to respond to the user
    mLoginButton.registerCallback(
        mCallbackManager,
        new FacebookCallback<LoginResult>() {
          @Override
          public void onSuccess(@NonNull LoginResult loginResult) {
            setResult(RESULT_OK);
            finish();
          }

          @Override
          public void onCancel() {
            setResult(RESULT_CANCELED);
            finish();
          }

          @Override
          public void onError(@NonNull FacebookException e) {
            // Handle exception
          }
        });
  }
}
