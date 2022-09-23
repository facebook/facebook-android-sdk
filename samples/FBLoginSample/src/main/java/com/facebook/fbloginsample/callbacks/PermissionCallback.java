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

public class PermissionCallback {

  public interface IPermissionResponse {
    void onCompleted(GraphResponse response);
  }

  private GraphRequest.Callback mCallback;

  public PermissionCallback(final IPermissionResponse caller) {

    mCallback =
        new GraphRequest.Callback() {

          // Handled by PermissionsActivity
          @Override
          public void onCompleted(GraphResponse response) {
            caller.onCompleted(response);
          }
        };
  }

  public GraphRequest.Callback getCallback() {
    return mCallback;
  }
}
