/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices

import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.gamingservices.model.CustomUpdateContent

object CustomUpdate {
  /**
   * Returns a request to perform a CustomUpdate
   *
   * This informs facebook that the player has taken an action and will notify other players in the
   * same GamingContext. Please check CustomUpdateContent.Builder for details on all the fields that
   * allow customizing the update.
   *
   * @returns A GraphRequest that is ready to execute
   */
  @JvmStatic
  fun newCustomUpdateRequest(
      content: CustomUpdateContent,
      callback: GraphRequest.Callback?
  ): GraphRequest {
    return GraphRequest.newPostRequest(
        AccessToken.getCurrentAccessToken(),
        "me/custom_update",
        content.toGraphRequestContent(),
        callback)
  }
}
