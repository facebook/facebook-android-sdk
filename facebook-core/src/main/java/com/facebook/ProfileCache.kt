/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONException
import org.json.JSONObject

class ProfileCache {
  private val sharedPreferences: SharedPreferences
  fun load(): Profile? {
    val jsonString = sharedPreferences.getString(CACHED_PROFILE_KEY, null)
    if (jsonString != null) {
      try {
        val jsonObject = JSONObject(jsonString)
        return Profile(jsonObject)
      } catch (e: JSONException) {
        // Can't recover
      }
    }
    return null
  }

  fun save(profile: Profile) {
    val jsonObject = profile.toJSONObject()
    if (jsonObject != null) {
      sharedPreferences.edit().putString(CACHED_PROFILE_KEY, jsonObject.toString()).apply()
    }
  }

  fun clear() {
    sharedPreferences.edit().remove(CACHED_PROFILE_KEY).apply()
  }

  companion object {
    const val CACHED_PROFILE_KEY = "com.facebook.ProfileManager.CachedProfile"
    const val SHARED_PREFERENCES_NAME = "com.facebook.AccessTokenManager.SharedPreferences"
  }

  init {
    sharedPreferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
  }
}
