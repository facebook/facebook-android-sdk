/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
  public void onSuccess(LoginResult loginResult) {
    // Refresh token cached on device after login succeeds
    AccessToken.refreshCurrentAccessTokenAsync(this);
  }

  @Override
  public void onCancel() {
    // Handle user cancel ...
  }

  @Override
  public void onError(FacebookException e) {
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
