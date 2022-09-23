/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless.internal

import android.util.Log

object UnityReflection {
  private val TAG = UnityReflection::class.java.canonicalName
  private const val UNITY_PLAYER_CLASS = "com.unity3d.player.UnityPlayer"
  private const val UNITY_SEND_MESSAGE_METHOD = "UnitySendMessage"
  private const val FB_UNITY_GAME_OBJECT = "UnityFacebookSDKPlugin"
  private const val CAPTURE_VIEW_HIERARCHY_METHOD = "CaptureViewHierarchy"
  private const val EVENT_MAPPING_METHOD = "OnReceiveMapping"
  private lateinit var unityPlayer: Class<*>

  private fun getUnityPlayerClass(): Class<*> = Class.forName(UNITY_PLAYER_CLASS)

  @JvmStatic
  fun sendMessage(unityObject: String?, unityMethod: String?, message: String?) {
    try {
      if (!::unityPlayer.isInitialized) {
        unityPlayer = getUnityPlayerClass()
      }
      val method =
          unityPlayer.getMethod(
              UNITY_SEND_MESSAGE_METHOD, String::class.java, String::class.java, String::class.java)
      method.invoke(unityPlayer, unityObject, unityMethod, message)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to send message to Unity", e)
    }
  }

  @JvmStatic
  fun captureViewHierarchy() {
    sendMessage(FB_UNITY_GAME_OBJECT, CAPTURE_VIEW_HIERARCHY_METHOD, "")
  }

  @JvmStatic
  fun sendEventMapping(eventMapping: String?) {
    sendMessage(FB_UNITY_GAME_OBJECT, EVENT_MAPPING_METHOD, eventMapping)
  }
}
