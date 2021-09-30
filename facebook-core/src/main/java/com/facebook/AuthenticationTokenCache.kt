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
import java.lang.IllegalArgumentException

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
                  AccessTokenManager.SHARED_PREFERENCES_NAME,
                  Context.MODE_PRIVATE)) // TODO replace with AuthenticationTokenManager

  fun load(): AuthenticationToken? {
    var authenticationToken: AuthenticationToken? = null
    if (hasCachedAuthenticationToken()) {
      // If we have something cached, we try to use it; even if it is invalid, do not fall
      authenticationToken = cachedAuthenticationToken
    }
    return authenticationToken
  }

  fun save(authenticationToken: AuthenticationToken) {
    sharedPreferences
        .edit()
        .putString(
            CACHED_AUTHENTICATION_TOKEN_KEY,
            authenticationToken.token) // TODO: save the jsonString instead of actual token
        .putString(CACHED_AUTHENTICATION_TOKEN_NONCE_KEY, authenticationToken.expectedNonce)
        .apply()
  }

  fun clear() {
    sharedPreferences.edit().remove(CACHED_AUTHENTICATION_TOKEN_KEY).apply()
    sharedPreferences.edit().remove(CACHED_AUTHENTICATION_TOKEN_NONCE_KEY).apply()
  }

  private fun hasCachedAuthenticationToken(): Boolean {
    return sharedPreferences.contains(CACHED_AUTHENTICATION_TOKEN_KEY)
  }

  private val cachedAuthenticationToken: AuthenticationToken?
    get() {
      val idTokenString = sharedPreferences.getString(CACHED_AUTHENTICATION_TOKEN_KEY, null)
      val idTokenExpectedNonce =
          sharedPreferences.getString(CACHED_AUTHENTICATION_TOKEN_NONCE_KEY, null)
      return if (!idTokenString.isNullOrEmpty() && !idTokenExpectedNonce.isNullOrEmpty()) {
        try {
          AuthenticationToken(idTokenString, idTokenExpectedNonce)
        } catch (e: IllegalArgumentException) {
          // in case if exception happens on reconstructing the id token
          null
        }
      } else null
    }

  companion object {
    const val CACHED_AUTHENTICATION_TOKEN_KEY =
        "com.facebook.AuthenticationManager.CachedAuthenticationToken"
    const val CACHED_AUTHENTICATION_TOKEN_NONCE_KEY =
        "com.facebook.AuthenticationManager.CachedAuthenticationTokenNonce"
  }
}
