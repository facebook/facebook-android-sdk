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
package com.facebook.internal

import android.content.Intent
import com.facebook.CallbackManager
import com.facebook.FacebookSdk
import java.util.HashMap

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class CallbackManagerImpl : CallbackManager {
  private val callbacks: MutableMap<Int, Callback> = HashMap()
  fun registerCallback(requestCode: Int, callback: Callback) {
    callbacks[requestCode] = callback
  }

  fun unregisterCallback(requestCode: Int) {
    callbacks.remove(requestCode)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    val callback = callbacks[requestCode]
    return callback?.onActivityResult(resultCode, data)
        ?: runStaticCallback(requestCode, resultCode, data)
  }

  interface Callback {
    fun onActivityResult(resultCode: Int, data: Intent?): Boolean
  }

  enum class RequestCodeOffset(private val offset: Int) {
    Login(0),
    Share(1),
    Message(2),
    Like(3),
    GameRequest(4),
    AppGroupCreate(5),
    AppGroupJoin(6),
    AppInvite(7),
    DeviceShare(8),
    GamingFriendFinder(9),
    GamingGroupIntegration(10),
    Referral(11);

    fun toRequestCode(): Int {
      return FacebookSdk.getCallbackRequestCodeOffset() + offset
    }
  }

  companion object {
    private val staticCallbacks: MutableMap<Int, Callback> = HashMap()

    /**
     * If there is no explicit callback, but we still need to call the Facebook component, because
     * it's going to update some state, e.g., login, like. Then we should register a static callback
     * that can still handle the response.
     *
     * @param requestCode The request code.
     * @param callback The callback for the feature.
     */
    @Synchronized
    @JvmStatic
    fun registerStaticCallback(requestCode: Int, callback: Callback) {
      if (staticCallbacks.containsKey(requestCode)) {
        return
      }
      staticCallbacks[requestCode] = callback
    }

    @Synchronized
    @JvmStatic
    private fun getStaticCallback(requestCode: Int): Callback? {
      return staticCallbacks[requestCode]
    }

    @JvmStatic
    private fun runStaticCallback(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
      val callback = getStaticCallback(requestCode)
      return callback?.onActivityResult(resultCode, data) ?: false
    }
  }
}
