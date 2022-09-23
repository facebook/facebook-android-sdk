/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.requests;

import android.net.Uri;
import android.os.Bundle;
import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;

public class PostsRequest {
  private static final String FEED_ENDPOINT = "/me/feed";

  public static void makeGetPostsRequest(GraphRequest.Callback callback) {

    Bundle params = new Bundle();
    params.putString("fields", "message,created_time,id,picture,story,from");

    GraphRequest request =
        new GraphRequest(
            AccessToken.getCurrentAccessToken(), FEED_ENDPOINT, params, HttpMethod.GET, callback);
    request.executeAsync();
  }

  public static void makePublishPostRequest(String message, GraphRequest.Callback callback) {
    Bundle params = new Bundle();
    params.putString("message", message);
    GraphRequest request =
        new GraphRequest(
            AccessToken.getCurrentAccessToken(), FEED_ENDPOINT, params, HttpMethod.POST, callback);
    request.executeAsync();
  }

  public static void makePublishPostRequest(
      Uri attachmentUri, FacebookCallback<Sharer.Result> callback) {
    SharePhoto photo = new SharePhoto.Builder().setImageUrl(attachmentUri).build();
    SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();

    ShareApi.share(content, callback);
  }
}
