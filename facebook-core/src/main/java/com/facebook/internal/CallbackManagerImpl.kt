/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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

  fun interface Callback {
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
    Referral(11),
    GamingContextCreate(12),
    GamingContextSwitch(13),
    GamingContextChoose(14),
    TournamentShareDialog(15),
    TournamentJoinDialog(16);

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
