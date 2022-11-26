/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.requests

import android.net.Uri
import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.FacebookCallback
import com.facebook.GraphRequest
import com.facebook.HttpMethod.*
import com.facebook.share.ShareApi
import com.facebook.share.Sharer
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent

object PostsRequest {
  private const val FEED_ENDPOINT = "/me/feed"

  fun makeGetPostsRequest(callback: GraphRequest.Callback) {
    val params = Bundle()
    params.putString("fields", "message,created_time,id,picture,story,from")
    val request =
        GraphRequest(AccessToken.getCurrentAccessToken(), FEED_ENDPOINT, params, GET, callback)
    request.executeAsync()
  }

  fun makePublishPostRequest(message: String, callback: GraphRequest.Callback) {
    val params = Bundle()
    params.putString("message", message)
    val request =
        GraphRequest(AccessToken.getCurrentAccessToken(), FEED_ENDPOINT, params, POST, callback)
    request.executeAsync()
  }

  fun makePublishPostRequest(attachmentUri: Uri?, callback: FacebookCallback<Sharer.Result>) {
    val photo = SharePhoto.Builder().setImageUrl(attachmentUri).build()
    val content = SharePhotoContent.Builder().addPhoto(photo).build()
    ShareApi.share(content, callback)
  }
}
