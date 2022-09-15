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
