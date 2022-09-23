/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
