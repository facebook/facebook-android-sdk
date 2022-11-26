/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fbloginsample.callbacks

import com.facebook.GraphRequest
import com.facebook.fbloginsample.entities.Post
import java.util.ArrayList
import org.json.JSONException
import org.json.JSONObject

class GetPostsCallback(private val getPostsResponse: IGetPostsResponse) {
  interface IGetPostsResponse {
    fun onGetPostsCompleted(posts: List<Post>)
  }

  private val posts = mutableListOf<Post>()

  val callback =
      GraphRequest.Callback { response ->
        try {
          val postsObj = response.getJSONObject() ?: return@Callback
          val jsonPostsArray = postsObj.getJSONArray("data")
          for (i in 0 until jsonPostsArray.length()) {
            val jPost = jsonPostsArray.getJSONObject(i)
            val post = jsonToPost(jPost)
            posts.add(post)
          }
        } catch (e: JSONException) {
          // Handle exception ...
        }

        // Handled by PostFeedActivity
        getPostsResponse.onGetPostsCompleted(posts)
      }

  @Throws(JSONException::class)
  private fun jsonToPost(post: JSONObject): Post {
    val message = if (post.has("message")) post.getString("message") else null
    var picture = if (post.has("picture")) post.getString("picture") else null
    val createdTime = post.getString("created_time")
    val id = post.getString("id")
    val from = post.getJSONObject("from")
    val fromName = from.getString("name")
    val fromId = from.getString("id")
    return Post(message, createdTime, id, picture, fromName, fromId)
  }
}
