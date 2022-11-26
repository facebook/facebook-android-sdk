/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** A callback class for getting the Login Status of a user. */
interface LoginStatusCallback {
  /**
   * Called when an access token is able to be retrieved successfully.
   *
   * @param accessToken The access token retrieved for the user
   */
  fun onCompleted(accessToken: AccessToken)

  /** Called when an access token could not be retrieved. */
  fun onFailure()

  /**
   * Called when there was an error getting the login status of a user.
   *
   * @param exception The error that occurred
   */
  fun onError(exception: Exception)
}
