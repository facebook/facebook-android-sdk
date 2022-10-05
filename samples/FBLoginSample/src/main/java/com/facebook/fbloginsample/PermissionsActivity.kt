/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.AccessToken.AccessTokenRefreshCallback
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory.create
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphResponse
import com.facebook.fbloginsample.callbacks.PermissionCallback
import com.facebook.fbloginsample.callbacks.PermissionCallback.IPermissionResponse
import com.facebook.fbloginsample.requests.PermissionRequest.makeRevokePermRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

class PermissionsActivity :
    Activity(), IPermissionResponse, AccessTokenRefreshCallback, FacebookCallback<LoginResult> {
  private lateinit var emailSwitch: Switch
  private lateinit var userPostsSwitch: Switch
  private lateinit var publishPostSwitch: Switch
  private lateinit var entireAppSwitch: Switch
  private lateinit var callbackManager: CallbackManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_permissions)
    callbackManager = create()
    LoginManager.getInstance().registerCallback(callbackManager, this)

    // Make a switch for the email permission
    emailSwitch = findViewById(R.id.switch_email_permission)
    emailSwitch.setOnCheckedChangeListener { _, isChecked ->
      val permissions = AccessToken.getCurrentAccessToken()?.permissions
      if (isChecked && permissions?.contains(EMAIL) == false) {
        // Make request to user to grant email permission
        LoginManager.getInstance().logInWithReadPermissions(this@PermissionsActivity, listOf(EMAIL))
      } else if (!isChecked && permissions?.contains(EMAIL) == true) {
        // Make revoke email permission request
        makeRevokePermRequest(EMAIL, PermissionCallback(this@PermissionsActivity).callback)
      }
    }

    // Make a switch for the user posts permission
    userPostsSwitch = findViewById(R.id.switch_user_posts_permission)
    userPostsSwitch.setOnCheckedChangeListener { _, isChecked ->
      val permissions = AccessToken.getCurrentAccessToken()?.permissions
      if (isChecked && permissions?.contains(USER_POSTS) == false) {
        // Make request to user to grant user_posts permission
        LoginManager.getInstance()
            .logInWithReadPermissions(this@PermissionsActivity, listOf(USER_POSTS))
      } else if (!isChecked && permissions?.contains(USER_POSTS) == true) {
        // Make revoke user_posts permission request
        makeRevokePermRequest(USER_POSTS, PermissionCallback(this@PermissionsActivity).callback)
      }
    }

    // Make a switch for the publish posts permission
    publishPostSwitch = findViewById(R.id.switch_publish_post_permission)
    publishPostSwitch.setOnCheckedChangeListener { _, isChecked ->
      val permissions = AccessToken.getCurrentAccessToken()?.permissions
      if (isChecked && permissions?.contains(PUBLISH_ACTIONS) == false) {
        // Make request to user to grant publish_actions permission
        LoginManager.getInstance()
            .logInWithPublishPermissions(this@PermissionsActivity, listOf(PUBLISH_ACTIONS))
      } else if (!isChecked && permissions?.contains(PUBLISH_ACTIONS) == true) {
        // Make revoke publish_actions permission request
        makeRevokePermRequest(
            PUBLISH_ACTIONS, PermissionCallback(this@PermissionsActivity).callback)
      }
    }

    // Make a switch for the app login permission
    entireAppSwitch = findViewById(R.id.switch_app_permission)
    entireAppSwitch.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked && AccessToken.getCurrentAccessToken() == null) {
        // Make request to user to login
        LoginManager.getInstance()
            .logInWithReadPermissions(this@PermissionsActivity, listOf(PUBLIC_PROFILE))
      } else if (!isChecked && AccessToken.getCurrentAccessToken() != null) {
        makeRevokePermRequest(APP, PermissionCallback(this@PermissionsActivity).callback)
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    callbackManager.onActivityResult(requestCode, resultCode, data)
  }

  override fun onResume() {
    super.onResume()
    setPermissionsSwitches()
  }

  // PermissionCallback.IPermissionResponse Callbacks
  override fun onCompleted(response: GraphResponse?) {
    response?.error?.let {
      Toast.makeText(this, "Error with permissions request: " + it.errorMessage, Toast.LENGTH_LONG)
          .show()
    }
    AccessToken.refreshCurrentAccessTokenAsync(this@PermissionsActivity)
  }

  // Facebook Login Callbacks
  override fun onSuccess(result: LoginResult) {
    // Refresh token cached on device after login succeeds
    AccessToken.refreshCurrentAccessTokenAsync(this)
  }

  override fun onCancel() {
    // Handle user cancel ...
  }

  override fun onError(error: FacebookException) {
    // Handle exception ...
  }

  // Access Token Refresh Callbacks
  override fun OnTokenRefreshed(accessToken: AccessToken?) {
    if (accessToken == null) {
      LoginManager.getInstance().logOut()
      val loginIntent = Intent(this@PermissionsActivity, FacebookLoginActivity::class.java)
      startActivity(loginIntent)
    } else {
      setPermissionsSwitches()
    }
  }

  override fun OnTokenRefreshFailed(exception: FacebookException?) {
    // Handle exception ...
  }

  // Set switch on/off according to current user-granted permissions
  private fun setPermissionsSwitches() {
    val permissions = AccessToken.getCurrentAccessToken()?.permissions
    emailSwitch.isChecked = permissions?.contains(EMAIL) == true
    userPostsSwitch.isChecked = permissions?.contains(USER_POSTS) == true
    publishPostSwitch.isChecked = permissions?.contains(PUBLISH_ACTIONS) == true
    entireAppSwitch.isChecked = AccessToken.getCurrentAccessToken() != null
  }

  companion object {
    private const val EMAIL = "email"
    private const val USER_POSTS = "user_posts"
    private const val PUBLISH_ACTIONS = "publish_actions"
    private const val PUBLIC_PROFILE = "public_profile"
    private const val APP = "app"
  }
}
