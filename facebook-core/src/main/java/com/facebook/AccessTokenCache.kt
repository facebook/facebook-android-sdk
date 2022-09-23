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
