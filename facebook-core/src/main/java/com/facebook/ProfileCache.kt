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
