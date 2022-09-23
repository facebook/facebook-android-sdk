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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.fbloginsample.adapters.PostAdapter;
import com.facebook.fbloginsample.callbacks.GetPostsCallback;
import com.facebook.fbloginsample.callbacks.PublishPostCallback;
import com.facebook.fbloginsample.entities.Post;
import com.facebook.fbloginsample.requests.PostsRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostFeedActivity extends Activity
    implements GetPostsCallback.IGetPostsResponse,
        PublishPostCallback.IPublishPostResponse,
        AccessToken.AccessTokenRefreshCallback,
        FacebookCallback<LoginResult> {

  private static final int PICK_IMAGE = 1;
  private static final String PUBLISH_ACTIONS = "publish_actions";
  private static final String SELECT_PICTURE = "Select Picture";
  private static final String TYPE_IMAGE = "image/*";

  private ListView mPostsListView;
  private TextView mComposeText;
  private Uri mAttachmentUri;
  private CallbackManager mCallbackManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_post_feed);
    mCallbackManager = CallbackManager.Factory.create();
    LoginManager.getInstance().registerCallback(mCallbackManager, this);

    PostAdapter postApapter = new PostAdapter(this, new ArrayList<Post>());
    mPostsListView = findViewById(R.id.post_list_view);
    mPostsListView.setAdapter(postApapter);

    // Attach image
    ImageButton attachImgBtn = findViewById(R.id.btn_attach);
    attachImgBtn.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent intent = new Intent();
            intent.setType(TYPE_IMAGE);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, SELECT_PICTURE), PICK_IMAGE);
          }
        });

    // Compose post
    mComposeText = findViewById(R.id.compose_text);
    Button composePostBtn = findViewById(R.id.btn_send);
    composePostBtn.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (AccessToken.getCurrentAccessToken().getPermissions().contains(PUBLISH_ACTIONS)) {
              makePost();
            } else {
              // Get Publish Permissions
              LoginManager.getInstance()
                  .logInWithPublishPermissions(
                      PostFeedActivity.this, Arrays.asList(PUBLISH_ACTIONS));
            }
          }
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    getPosts();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    mCallbackManager.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_IMAGE && data != null) {
      mAttachmentUri = data.getData();
    }
  }

  // From GetPostsCallback
  @Override
  public void onGetPostsCompleted(List<Post> posts) {
    if (posts != null && !posts.isEmpty()) {
      mPostsListView.setAdapter(new PostAdapter(PostFeedActivity.this, posts));
    }
  }

  // From PublishPostCallback
  @Override
  public void onPublishCompleted() {
    getPosts();
  }

  // From AccessTokenRefreshCallback
  @Override
  public void OnTokenRefreshed(AccessToken accessToken) {
    makePost();
  }

  @Override
  public void OnTokenRefreshFailed(FacebookException e) {
    // Handle exception ...
  }

  // From FacebookLogin
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
  public void onError(@NonNull FacebookException e) {
    // Handle exception ...
  }

  // Private methods
  private void getPosts() {
    PostsRequest.makeGetPostsRequest(new GetPostsCallback(this).getCallback());
  }

  private void makePost() {
    if (mAttachmentUri == null) {
      PostsRequest.makePublishPostRequest(
          mComposeText.getText().toString(),
          new PublishPostCallback(this).getGraphRequestCallback());
    } else {
      PostsRequest.makePublishPostRequest(
          mAttachmentUri, new PublishPostCallback(this).getShareCallback());
    }
    mComposeText.setText("");
    mAttachmentUri = null;
  }
}
