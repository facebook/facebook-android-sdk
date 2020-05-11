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
