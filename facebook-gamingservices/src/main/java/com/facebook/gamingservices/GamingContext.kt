/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices

import com.facebook.FacebookSdk
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler
import com.facebook.gamingservices.cloudgaming.DaemonRequest
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum

data class GamingContext(val contextID: String) {

  companion object {
    private const val DEFAULT_TIMEOUT: Int = 5
    private var currentContext: GamingContext? = null

    @JvmStatic
    fun getCurrentGamingContext(): GamingContext? {
      if (CloudGameLoginHandler.isRunningInCloud()) {
        val response =
            DaemonRequest.executeAndWait(
                FacebookSdk.getApplicationContext(),
                null,
                SDKMessageEnum.CONTEXT_GET_ID,
                DEFAULT_TIMEOUT)
        val contextId = response?.getJSONObject()?.getString("id") ?: return null
        return GamingContext(contextId)
      }
      return currentContext
    }

    @JvmStatic
    fun setCurrentGamingContext(ctx: GamingContext) {
      if (CloudGameLoginHandler.isRunningInCloud()) {
        return
      }
      currentContext = ctx
    }
  }
}
