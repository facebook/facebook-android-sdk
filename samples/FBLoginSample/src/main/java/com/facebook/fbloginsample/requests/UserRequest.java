/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.requests;

import android.os.Bundle;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;

public class UserRequest {
  private static final String ME_ENDPOINT = "/me";

  public static void makeUserRequest(GraphRequest.Callback callback) {
    Bundle params = new Bundle();
    params.putString("fields", "picture,name,id,email,permissions");

    GraphRequest request =
        new GraphRequest(
            AccessToken.getCurrentAccessToken(), ME_ENDPOINT, params, HttpMethod.GET, callback);
    request.executeAsync();
  }
}
