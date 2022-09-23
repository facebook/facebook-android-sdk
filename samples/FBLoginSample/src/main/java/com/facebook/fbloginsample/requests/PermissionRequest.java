/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.requests;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;

public class PermissionRequest {
  private static final String PERMISSIONS_ENDPOINT = "/me/permissions";
  private static final String APP = "app";

  public static void makeRevokePermRequest(String permission, GraphRequest.Callback callback) {
    String graphPath;
    if (permission.equals(APP)) {
      graphPath = PERMISSIONS_ENDPOINT;
    } else {
      graphPath = PERMISSIONS_ENDPOINT + "/" + permission;
    }

    GraphRequest request =
        GraphRequest.newGraphPathRequest(AccessToken.getCurrentAccessToken(), graphPath, callback);
    request.setHttpMethod(HttpMethod.DELETE);
    request.executeAsync();
  }
}
