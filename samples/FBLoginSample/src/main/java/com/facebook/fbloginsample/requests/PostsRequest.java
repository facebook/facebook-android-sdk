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
