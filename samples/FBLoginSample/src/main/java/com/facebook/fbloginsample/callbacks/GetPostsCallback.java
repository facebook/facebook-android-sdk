/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.callbacks;

import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.fbloginsample.entities.Post;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GetPostsCallback {

  public interface IGetPostsResponse {
    void onGetPostsCompleted(List<Post> posts);
  }

  private IGetPostsResponse mGetPostsResponse;
  private GraphRequest.Callback mCallback;
  private ArrayList<Post> mPosts = new ArrayList<>();

  public GetPostsCallback(final IGetPostsResponse getPostsResponse) {

    mGetPostsResponse = getPostsResponse;
    mCallback =
        new GraphRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {
            try {
              JSONObject postsObj = response.getJSONObject();
              if (postsObj == null) {
                return;
              }
              JSONArray posts = postsObj.getJSONArray("data");
              for (int i = 0; i < posts.length(); i++) {
                JSONObject jPost = posts.getJSONObject(i);
                Post post = jsonToPost(jPost);
                if (post != null) {
                  mPosts.add(post);
                }
              }

            } catch (JSONException e) {
              // Handle exception ...
            }

            // Handled by PostFeedActivity
            mGetPostsResponse.onGetPostsCompleted(mPosts);
          }
        };
  }

  private Post jsonToPost(JSONObject post) throws JSONException {
    String message = null;
    if (post.has("message")) {
      message = post.getString("message");
    }
    String picture = null;
    if (post.has("picture")) {
      picture = post.getString("picture");
    }
    String created_time = post.getString("created_time");
    String id = post.getString("id");

    JSONObject from = post.getJSONObject("from");
    String from_name = from.getString("name");
    String from_id = from.getString("id");

    return new Post(message, created_time, id, picture, from_name, from_id);
  }

  public GraphRequest.Callback getCallback() {
    return mCallback;
  }
}
