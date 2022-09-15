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
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONException
import org.json.JSONObject

internal class AccessTokenCache(
    private val sharedPreferences: SharedPreferences,
    private val tokenCachingStrategyFactory: SharedPreferencesTokenCachingStrategyFactory
) {
  private var tokenCachingStrategyField: LegacyTokenHelper? = null
  private val tokenCachingStrategy: LegacyTokenHelper
    @AutoHandleExceptions
    get() {
      if (tokenCachingStrategyField == null) {
        synchronized(this) {
          if (tokenCachingStrategyField == null) {
            tokenCachingStrategyField = tokenCachingStrategyFactory.create()
          }
        }
      }
      return checkNotNull(tokenCachingStrategyField)
    }

  constructor() :
      this(
          FacebookSdk.getApplicationContext()
              .getSharedPreferences(
                  AccessTokenManager.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE),
          SharedPreferencesTokenCachingStrategyFactory())

  fun load(): AccessToken? {
    var accessToken: AccessToken? = null
    if (hasCachedAccessToken()) {
      // If we have something cached, we try to use it; even if it is invalid, do not fall
      // back to a legacy caching strategy.
      accessToken = cachedAccessToken
    } else if (shouldCheckLegacyToken()) {
      accessToken = legacyAccessToken
      if (accessToken != null) {
        save(accessToken)
        tokenCachingStrategy.clear()
      }
    }
    return accessToken
  }

  fun save(accessToken: AccessToken) {
    try {
      val jsonObject = accessToken.toJSONObject()
      sharedPreferences.edit().putString(CACHED_ACCESS_TOKEN_KEY, jsonObject.toString()).apply()
    } catch (e: JSONException) {
      // Can't recover
    }
  }

  fun clear() {
    sharedPreferences.edit().remove(CACHED_ACCESS_TOKEN_KEY).apply()
    if (shouldCheckLegacyToken()) {
      tokenCachingStrategy.clear()
    }
  }

  private fun hasCachedAccessToken(): Boolean {
    return sharedPreferences.contains(CACHED_ACCESS_TOKEN_KEY)
  }

  private val cachedAccessToken: AccessToken?
    get() {
      val jsonString = sharedPreferences.getString(CACHED_ACCESS_TOKEN_KEY, null)
      return if (jsonString != null) {
        try {
          val jsonObject = JSONObject(jsonString)
          AccessToken.createFromJSONObject(jsonObject)
        } catch (e: JSONException) {
          null
        }
      } else null
    }

  private fun shouldCheckLegacyToken(): Boolean {
    return FacebookSdk.isLegacyTokenUpgradeSupported()
  }

  private val legacyAccessToken: AccessToken?
    get() {
      var accessToken: AccessToken? = null
      val bundle = tokenCachingStrategy.load()
      if (bundle != null && LegacyTokenHelper.hasTokenInformation(bundle)) {
        accessToken = AccessToken.createFromLegacyCache(bundle)
      }
      return accessToken
    }

  internal class SharedPreferencesTokenCachingStrategyFactory {
    fun create(): LegacyTokenHelper {
      return LegacyTokenHelper(FacebookSdk.getApplicationContext())
    }
  }

  companion object {
    const val CACHED_ACCESS_TOKEN_KEY = "com.facebook.AccessTokenManager.CachedAccessToken"
  }
}
