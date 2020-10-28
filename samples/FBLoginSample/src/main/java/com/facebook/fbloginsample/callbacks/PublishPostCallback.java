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

package com.facebook.fbloginsample.callbacks;

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.share.Sharer;

public class PublishPostCallback {

  public interface IPublishPostResponse {
    void onPublishCompleted();
  }

  private GraphRequest.Callback mCallback;

  private FacebookCallback<Sharer.Result> mShareCallback;

  public PublishPostCallback(final IPublishPostResponse caller) {

    mCallback =
        new GraphRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {
            // Handled by PostFeedActivity
            caller.onPublishCompleted();
          }
        };

    mShareCallback =
        new FacebookCallback<Sharer.Result>() {
          @Override
          public void onSuccess(Sharer.Result result) {
            // Handled by PostFeedActivity
            caller.onPublishCompleted();
          }

          @Override
          public void onCancel() {
            // Handle user cancel ...
          }

          @Override
          public void onError(FacebookException error) {
            // Handle exception ...
          }
        };
  }

  public GraphRequest.Callback getGraphRequestCallback() {
    return mCallback;
  }

  public FacebookCallback<Sharer.Result> getShareCallback() {
    return mShareCallback;
  }
}
