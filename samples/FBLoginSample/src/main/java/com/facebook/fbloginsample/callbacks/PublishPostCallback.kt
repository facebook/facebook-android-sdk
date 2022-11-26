/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.callbacks

import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.share.Sharer

class PublishPostCallback(caller: IPublishPostResponse) {
  interface IPublishPostResponse {
    fun onPublishCompleted()
  }

  val graphRequestCallback =
      GraphRequest.Callback { // Handled by PostFeedActivity
        caller.onPublishCompleted()
      }

  val shareCallback =
      object : FacebookCallback<Sharer.Result> {
        override fun onSuccess(result: Sharer.Result) {
          // Handled by PostFeedActivity
          caller.onPublishCompleted()
        }

        override fun onCancel() {
          // Handle user cancel ...
        }

        override fun onError(error: FacebookException) {
          // Handle exception ...
        }
      }
}
