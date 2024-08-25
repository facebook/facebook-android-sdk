/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.facebook.FacebookSdk

class AppLinkManager private constructor() {

  private val preferences: SharedPreferences by lazy {
    FacebookSdk.getApplicationContext().getSharedPreferences(APPLINK_INFO, Context.MODE_PRIVATE)
  }

  companion object {
    const val APPLINK_INFO = "com.facebook.sdk.APPLINK_INFO"

    @Volatile
    private var instance: AppLinkManager? = null

    fun getInstance(): AppLinkManager? =
      instance ?: synchronized(this) {
        if (!FacebookSdk.isInitialized()) {
          return null
        }
        instance ?: AppLinkManager().also { instance = it }
      }
  }

  fun handleURL(activity: Activity) {}

  fun getInfo(key: String): String? {
    return preferences.getString(key, null)
  }
}
