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
import android.os.Bundle
import com.facebook.internal.Utility
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RuntimeEnvironment

@PrepareForTest(FacebookSdk::class, LegacyTokenHelper::class, Utility::class)
class AccessTokenCacheTest : FacebookPowerMockTestCase() {
  companion object {
    private const val TOKEN_STRING = "A token of my esteem"
    private const val USER_ID = "1000"
    private val PERMISSIONS = listOf("walk", "chew gum")
    private val EXPIRES = Date(2_025, 5, 3)
    private val LAST_REFRESH = Date(2_023, 8, 15)
    private const val APP_ID = "1234"
  }
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var cachingStrategy: LegacyTokenHelper
  private lateinit var cachingStrategyFactory:
      AccessTokenCache.SharedPreferencesTokenCachingStrategyFactory

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    sharedPreferences =
        RuntimeEnvironment.application.getSharedPreferences(
            AccessTokenManager.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPreferences.edit().clear().commit()
    cachingStrategy = mock()
    cachingStrategyFactory = mock()
    whenever(cachingStrategyFactory.create()).thenReturn(cachingStrategy)
    MemberModifier.stub<Any>(
            PowerMockito.method(
                Utility::class.java, "awaitGetGraphMeRequestWithCache", String::class.java))
        .toReturn(JSONObject().put("id", "1000"))
  }

  @Test
  fun `test load returns false if no cached token`() {
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    val accessToken = cache.load()
    Assert.assertNull(accessToken)
    verifyZeroInteractions(cachingStrategyFactory)
  }

  @Test
  fun `test load returns false if no cached or legacy token`() {
    whenever(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true)
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    val accessToken = cache.load()
    Assert.assertNull(accessToken)
  }

  @Test
  fun `test load returns false if empty cached token and does not check legacy`() {
    val jsonObject = JSONObject()
    sharedPreferences
        .edit()
        .putString(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY, jsonObject.toString())
        .commit()
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    val accessToken = cache.load()
    Assert.assertNull(accessToken)
    verifyZeroInteractions(cachingStrategy)
  }

  @Test
  fun `test load returns false if no cached token and empty legacy token`() {
    whenever(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true)
    whenever(cachingStrategy.load()).thenReturn(Bundle())
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    val accessToken = cache.load()
    Assert.assertNull(accessToken)
  }

  @Test
  fun `test load valid cached token`() {
    val accessToken = createAccessToken()
    val jsonObject = accessToken.toJSONObject()
    sharedPreferences
        .edit()
        .putString(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY, jsonObject.toString())
        .commit()
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    val loadedAccessToken = cache.load()
    Assert.assertNotNull(loadedAccessToken)
    Assert.assertEquals(accessToken, loadedAccessToken)
  }

  @Test
  fun `test load sets current token if no cached token but valid legacy token`() {
    whenever(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true)
    val accessToken = createAccessToken()
    whenever(cachingStrategy.load())
        .thenReturn(AccessTokenTestHelper.toLegacyCacheBundle(accessToken))
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    val loadedAccessToken = cache.load()
    Assert.assertNotNull(loadedAccessToken)
    Assert.assertEquals(accessToken, loadedAccessToken)
  }

  @Test
  fun `test load saves token when upgrading from legacy token`() {
    whenever(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true)
    val accessToken = createAccessToken()
    whenever(cachingStrategy.load())
        .thenReturn(AccessTokenTestHelper.toLegacyCacheBundle(accessToken))
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    cache.load()
    assertThat(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY)).isTrue
    val savedAccessToken =
        AccessToken.createFromJSONObject(
            JSONObject(
                checkNotNull(
                    sharedPreferences.getString(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY, null))))
    Assert.assertEquals(accessToken, savedAccessToken)
  }

  @Test
  fun `test load clears legacy cache when upgrading from legacy token`() {
    whenever(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true)
    val accessToken = createAccessToken()
    whenever(cachingStrategy.load())
        .thenReturn(AccessTokenTestHelper.toLegacyCacheBundle(accessToken))
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    cache.load()
    verify(cachingStrategy, times(1)).clear()
  }

  @Test
  fun `test save writes to cache if token is valid`() {
    val accessToken = createAccessToken()
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    cache.save(accessToken)
    verify(cachingStrategy, never()).save(any())
    assertThat(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY)).isTrue
    val savedAccessToken =
        AccessToken.createFromJSONObject(
            JSONObject(
                checkNotNull(
                    sharedPreferences.getString(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY, null))))
    Assert.assertEquals(accessToken, savedAccessToken)
  }

  @Test
  fun `test clear cache does clear cache`() {
    val accessToken = createAccessToken()
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    cache.save(accessToken)
    cache.clear()
    assertThat(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY)).isFalse
    verify(cachingStrategy, never()).clear()
  }

  @Test
  fun `test clear cache clears legacy cache`() {
    whenever(FacebookSdk.isLegacyTokenUpgradeSupported()).thenReturn(true)
    val accessToken = createAccessToken()
    val cache = AccessTokenCache(sharedPreferences, cachingStrategyFactory)
    cache.save(accessToken)
    cache.clear()
    assertThat(sharedPreferences.contains(AccessTokenCache.CACHED_ACCESS_TOKEN_KEY)).isFalse
    verify(cachingStrategy, times(1)).clear()
  }

  private fun createAccessToken(
      tokenString: String = TOKEN_STRING,
      userId: String = USER_ID
  ): AccessToken {
    return AccessToken(
        tokenString,
        APP_ID,
        userId,
        PERMISSIONS,
        null,
        null,
        AccessTokenSource.WEB_VIEW,
        EXPIRES,
        LAST_REFRESH,
        null)
  }

  @Test
  fun `test the token caching strategy factory will create a cache from the application context`() {
    val mockContext = mock<Context>()
    whenever(mockContext.applicationContext).thenReturn(mockContext)
    whenever(mockContext.getSharedPreferences(any<String>(), any())).thenReturn(mock())
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)

    val factory = AccessTokenCache.SharedPreferencesTokenCachingStrategyFactory()
    factory.create()
    verify(mockContext)
        .getSharedPreferences(eq(LegacyTokenHelper.DEFAULT_CACHE_KEY), eq(Context.MODE_PRIVATE))
  }
}
