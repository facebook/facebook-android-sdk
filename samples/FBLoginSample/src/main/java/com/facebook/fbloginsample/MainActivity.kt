/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.facebook.AccessToken
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.LoginStatusCallback
import com.facebook.login.LoginManager.Companion.getInstance
import com.google.android.material.snackbar.Snackbar
import java.lang.Exception

class MainActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_main)
    if (resources.getString(R.string.facebook_app_id) == DEFAULT_FB_APP_ID) {
      showAlertNoFacebookAppId()
      return
    }
    if (resources.getString(R.string.facebook_client_token) == DEFAULT_FB_CLIENT_COKEN) {
      showAlertNoFacebookClientToken()
      return
    }
    val view = findViewById<View>(R.id.activity_main)
    // User was previously logged in, can log them in directly here.
    // If this callback is called, a popup notification appears
    getInstance()
        .retrieveLoginStatus(
            this,
            object : LoginStatusCallback {
              override fun onCompleted(accessToken: AccessToken) {
                val snackbar = Snackbar.make(view, "User Logged in", 2)
                snackbar.show()
              }

              override fun onFailure() {
                // If MainActivity is reached without the user being logged in,
                // redirect to the Login Activity
                if (getCurrentAccessToken() == null) {
                  val loginIntent = Intent(this@MainActivity, FacebookLoginActivity::class.java)
                  startActivity(loginIntent)
                }
              }

              override fun onError(exception: Exception) {
                // Handle exception
              }
            })

    // Make a button which leads to profile information of the user
    val gotoProfileButton = findViewById<Button>(R.id.btn_profile)
    gotoProfileButton.setOnClickListener {
      if (getCurrentAccessToken() == null) {
        val profileIntent = Intent(this@MainActivity, FacebookLoginActivity::class.java)
        startActivityForResult(profileIntent, RESULT_PROFILE_ACTIVITY)
      } else {
        val profileIntent = Intent(this@MainActivity, ProfileActivity::class.java)
        startActivity(profileIntent)
      }
    }

    // Make a button which leads to posts made by the user
    val gotoPostsFeedButton = findViewById<Button>(R.id.btn_posts)
    gotoPostsFeedButton.setOnClickListener {
      if (getCurrentAccessToken() == null) {
        val loginIntent = Intent(this@MainActivity, FacebookLoginActivity::class.java)
        startActivityForResult(loginIntent, RESULT_POSTS_ACTIVITY)
      } else {
        val postsFeedIntent = Intent(this@MainActivity, PostFeedActivity::class.java)
        startActivity(postsFeedIntent)
      }
    }

    // Make a button which leads to request or revoke permissions
    val gotoPermissionsButton = findViewById<Button>(R.id.btn_permissions)
    gotoPermissionsButton.setOnClickListener {
      if (getCurrentAccessToken() == null) {
        val loginIntent = Intent(this@MainActivity, FacebookLoginActivity::class.java)
        startActivityForResult(loginIntent, RESULT_PERMISSIONS_ACTIVITY)
      } else {
        val permissionsIntent = Intent(this@MainActivity, PermissionsActivity::class.java)
        startActivity(permissionsIntent)
      }
    }

    // Make a logout button
    val fbLogoutButton = findViewById<Button>(R.id.btn_fb_logout)
    fbLogoutButton.setOnClickListener {
      getInstance().logOut()
      val loginIntent = Intent(this@MainActivity, FacebookLoginActivity::class.java)
      startActivity(loginIntent)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    when (requestCode) {
      RESULT_PROFILE_ACTIVITY ->
          if (resultCode == RESULT_OK) {
            val profileIntent = Intent(this@MainActivity, ProfileActivity::class.java)
            startActivity(profileIntent)
          }
      RESULT_POSTS_ACTIVITY ->
          if (resultCode == RESULT_OK) {
            val postFeedIntent = Intent(this@MainActivity, PostFeedActivity::class.java)
            startActivity(postFeedIntent)
          }
      RESULT_PERMISSIONS_ACTIVITY ->
          if (resultCode == RESULT_OK) {
            val permissionsIntent = Intent(this@MainActivity, PermissionsActivity::class.java)
            startActivity(permissionsIntent)
          }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun showAlertNoFacebookAppId() {
    val alert = AlertDialog.Builder(this@MainActivity).create()
    alert.setTitle("Use your facebook app id in strings.xml")
    alert.setMessage(
        "This sample app can not properly function without your app id. " +
            "Use your facebook app id in strings.xml. Check out https://developers.facebook.com/docs/android/getting-started/ for more info. " +
            "Restart the app after that")
    alert.show()
  }

  private fun showAlertNoFacebookClientToken() {
    val alert = AlertDialog.Builder(this@MainActivity).create()
    alert.setTitle("Use your facebook client token in strings.xml")
    alert.setMessage(
        "On and above Facebook SDK v13.0, all Graph API calls must have a valid client token." +
            "Add your client token to the facebook_client_token string in strings.xml. Check out https://developers.facebook.com/docs/android/getting-started#app_id for more info. " +
            "Restart the app after that")
    alert.show()
  }

  companion object {
    private const val RESULT_PROFILE_ACTIVITY = 1
    private const val RESULT_POSTS_ACTIVITY = 2
    private const val RESULT_PERMISSIONS_ACTIVITY = 3
    private const val DEFAULT_FB_APP_ID = "ENTER_YOUR_FB_APP_ID_HERE"
    private const val DEFAULT_FB_CLIENT_COKEN = "ENTER_YOUR_FB_CLIENT_TOKEN_HERE"
  }
}
