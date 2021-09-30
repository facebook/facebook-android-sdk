/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
