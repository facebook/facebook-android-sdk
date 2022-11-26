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
