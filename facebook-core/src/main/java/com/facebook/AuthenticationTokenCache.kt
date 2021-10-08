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

/**
 * This class is an wrapper class which handles load/save/clear of the Authentication Token cache
 *
 * WARNING: This feature is currently in development and not intended for external usage.
 */
class AuthenticationTokenCache(private val sharedPreferences: SharedPreferences) {
  constructor() :
      this(
          FacebookSdk.getApplicationContext()
              .getSharedPreferences(
                  AuthenticationTokenManager.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE))

  fun load(): AuthenticationToken? {
    var authenticationToken: AuthenticationToken? = null
    if (hasCachedAuthenticationToken()) {
      // If we have something cached, we try to use it; even if it is invalid, do not fall
      authenticationToken = cachedAuthenticationToken
    }
    return authenticationToken
  }

  fun save(authenticationToken: AuthenticationToken) {
    try {
      val jsonObject = authenticationToken.toJSONObject()
      sharedPreferences
          .edit()
          .putString(CACHED_AUTHENTICATION_TOKEN_KEY, jsonObject.toString())
          .apply()
    } catch (e: JSONException) {
      // can't recover
    }
  }

  fun clear() {
    sharedPreferences.edit().remove(CACHED_AUTHENTICATION_TOKEN_KEY).apply()
  }

  private fun hasCachedAuthenticationToken(): Boolean {
    return sharedPreferences.contains(CACHED_AUTHENTICATION_TOKEN_KEY)
  }

  private val cachedAuthenticationToken: AuthenticationToken?
    get() {
      val jsonString = sharedPreferences.getString(CACHED_AUTHENTICATION_TOKEN_KEY, null)
      return if (jsonString != null) {
        try {
          val jsonObject = JSONObject(jsonString)
          AuthenticationToken(jsonObject)
        } catch (e: JSONException) {
          // in case if exception happens on reconstructing the id token
          null
        }
      } else null
    }

  companion object {
    const val CACHED_AUTHENTICATION_TOKEN_KEY =
        "com.facebook.AuthenticationManager.CachedAuthenticationToken"
  }
}
