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

import android.net.Uri;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.fbloginsample.entities.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GetUserCallback {

  public interface IGetUserResponse {
    void onCompleted(User user);
  }

  private IGetUserResponse mGetUserResponse;
  private GraphRequest.Callback mCallback;

  public GetUserCallback(final IGetUserResponse getUserResponse) {

    mGetUserResponse = getUserResponse;
    mCallback =
        new GraphRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {
            User user = null;
            try {
              JSONObject userObj = response.getJSONObject();
              if (userObj == null) {
                return;
              }
              user = jsonToUser(userObj);

            } catch (JSONException e) {
              // Handle exception ...
            }

            // Handled by ProfileActivity
            mGetUserResponse.onCompleted(user);
          }
        };
  }

  private User jsonToUser(JSONObject user) throws JSONException {
    Uri picture = Uri.parse(user.getJSONObject("picture").getJSONObject("data").getString("url"));
    String name = user.getString("name");
    String id = user.getString("id");
    String email = null;
    if (user.has("email")) {
      email = user.getString("email");
    }

    // Build permissions display string
    StringBuilder builder = new StringBuilder();
    JSONArray perms = user.getJSONObject("permissions").getJSONArray("data");
    builder.append("Permissions:\n");
    for (int i = 0; i < perms.length(); i++) {
      builder
          .append(perms.getJSONObject(i).get("permission"))
          .append(": ")
          .append(perms.getJSONObject(i).get("status"))
          .append("\n");
    }
    String permissions = builder.toString();

    return new User(picture, name, id, email, permissions);
  }

  public GraphRequest.Callback getCallback() {
    return mCallback;
  }
}
