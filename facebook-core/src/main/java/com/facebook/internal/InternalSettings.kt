/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

object InternalSettings {
  /**
   * This value is used by the unity sdk to set the custom version. The user agent format is
   * sdk_version/custom_user_agent For example FBAndroidSDK.4.2.0/Unity.7.0.1
   */
  private const val UNITY_PREFIX = "Unity."

  @Volatile private var customUserAgent: String? = null

  @JvmStatic fun getCustomUserAgent(): String? = customUserAgent
  @JvmStatic
  fun setCustomUserAgent(value: String) {
    customUserAgent = value
  }

  @JvmStatic
  val isUnityApp: Boolean
    get() = customUserAgent?.startsWith(UNITY_PREFIX) == true
}
