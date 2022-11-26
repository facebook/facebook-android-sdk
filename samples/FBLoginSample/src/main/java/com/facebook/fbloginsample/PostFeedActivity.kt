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
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.fbloginsample.adapters.PostAdapter
import com.facebook.fbloginsample.callbacks.GetPostsCallback
import com.facebook.fbloginsample.callbacks.PublishPostCallback
import com.facebook.fbloginsample.entities.Post
import com.facebook.fbloginsample.requests.PostsRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import java.util.ArrayList

class PostFeedActivity :
    Activity(),
    GetPostsCallback.IGetPostsResponse,
    PublishPostCallback.IPublishPostResponse,
    AccessToken.AccessTokenRefreshCallback,
    FacebookCallback<LoginResult> {

  private lateinit var postsListView: ListView
  private lateinit var composeTextView: TextView
  private lateinit var callbackManager: CallbackManager
  private var attachmentUri: Uri? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_post_feed)
    callbackManager = CallbackManager.Factory.create()
    LoginManager.getInstance().registerCallback(callbackManager, this)

    val postAdapter = PostAdapter(this, mutableListOf())
    postsListView = findViewById(R.id.post_list_view)
    postsListView.adapter = postAdapter

    // Attach image
    val attachImgBtn = findViewById<ImageButton>(R.id.btn_attach)
    attachImgBtn.setOnClickListener {
      val intent = Intent()
      intent.type = TYPE_IMAGE
      intent.action = Intent.ACTION_GET_CONTENT
      startActivityForResult(Intent.createChooser(intent, SELECT_PICTURE), PICK_IMAGE)
    }

    // Compose post
    composeTextView = findViewById(R.id.compose_text)
    val composePostBtn = findViewById<Button>(R.id.btn_send)
    composePostBtn.setOnClickListener {
      if (AccessToken.getCurrentAccessToken()?.permissions?.contains(PUBLISH_ACTIONS) == true) {
        makePost()
      } else {
        // Get Publish Permissions
        LoginManager.getInstance()
            .logInWithPublishPermissions(this@PostFeedActivity, listOf(PUBLISH_ACTIONS))
      }
    }
  }

  override fun onResume() {
    super.onResume()
    posts
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)

    callbackManager.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PICK_IMAGE) {
      attachmentUri = data.data
    }
  }

  // From GetPostsCallback
  override fun onGetPostsCompleted(posts: List<Post>) {
    if (posts.isNotEmpty()) {
      postsListView.adapter = PostAdapter(this@PostFeedActivity, posts)
    }
  }

  // From PublishPostCallback
  override fun onPublishCompleted() {
    posts
  }

  // From AccessTokenRefreshCallback
  override fun OnTokenRefreshed(accessToken: AccessToken?) {
    makePost()
  }

  override fun OnTokenRefreshFailed(exception: FacebookException?) {
    // Handle exception ...
  }

  // From FacebookLogin
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

  // Private methods
  private val posts: Unit
    get() {
      PostsRequest.makeGetPostsRequest(GetPostsCallback(this).callback)
    }

  private fun makePost() {
    if (attachmentUri == null) {
      PostsRequest.makePublishPostRequest(
          composeTextView.text.toString(), PublishPostCallback(this).graphRequestCallback)
    } else {
      PostsRequest.makePublishPostRequest(attachmentUri, PublishPostCallback(this).shareCallback)
    }
    composeTextView.text = ""
    attachmentUri = null
  }

  companion object {
    private const val PICK_IMAGE = 1
    private const val PUBLISH_ACTIONS = "publish_actions"
    private const val SELECT_PICTURE = "Select Picture"
    private const val TYPE_IMAGE = "image/*"
  }
}
