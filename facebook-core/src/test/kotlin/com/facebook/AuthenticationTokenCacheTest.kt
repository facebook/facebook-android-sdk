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
import com.facebook.internal.Utility
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.util.common.AuthenticationTokenTestUtil
import java.security.PublicKey
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RuntimeEnvironment

@PrepareForTest(FacebookSdk::class, OidcSecurityUtil::class)
internal class AuthenticationTokenCacheTest : FacebookPowerMockTestCase() {
  private lateinit var sharedPreferences: SharedPreferences

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.getApplicationId())
        .thenReturn(AuthenticationTokenTestUtil.APP_ID)

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    PowerMockito.`when`(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    PowerMockito.`when`(OidcSecurityUtil.getPublicKeyFromString(any()))
        .thenReturn(PowerMockito.mock(PublicKey::class.java))
    PowerMockito.`when`(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    sharedPreferences =
        RuntimeEnvironment.application.getSharedPreferences(
            AuthenticationTokenManager.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPreferences.edit().clear().apply()
    MemberModifier.stub<Any>(
            PowerMockito.method(
                Utility::class.java, "awaitGetGraphMeRequestWithCache", String::class.java))
        .toReturn(JSONObject().put("id", "1000"))
  }

  @Test
  fun `test load returns null if no cached authentication token`() {
    val cache = AuthenticationTokenCache(sharedPreferences)
    val authenticationToken = cache.load()
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun `test load returns null if empty authentication token`() {
    sharedPreferences
        .edit()
        .putString(AuthenticationTokenCache.CACHED_AUTHENTICATION_TOKEN_KEY, "")
        .apply()
    val cache = AuthenticationTokenCache(sharedPreferences)
    val authenticationToken = cache.load()
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun `test load valid cached token`() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    sharedPreferences
        .edit()
        .putString(
            AuthenticationTokenCache.CACHED_AUTHENTICATION_TOKEN_KEY,
            authenticationToken.toJSONObject().toString())
        .apply()
    val cache = AuthenticationTokenCache(sharedPreferences)
    val loadedAuthenticationToken = cache.load()
    assertThat(loadedAuthenticationToken).isNotNull
    assertThat(authenticationToken).isEqualTo(loadedAuthenticationToken)
  }

  @Test
  fun `test AuthenticationToken save and clear`() {
    val cache = AuthenticationTokenCache(sharedPreferences)
    sharedPreferences
        .edit()
        .remove(AuthenticationTokenCache.CACHED_AUTHENTICATION_TOKEN_KEY)
        .apply()
    assertThat(cache.load()).isNull()

    // make sure save correctly
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    cache.save(authenticationToken)
    assertThat(authenticationToken.token).isNotEmpty
    assertThat(
            sharedPreferences.getString(
                AuthenticationTokenCache.CACHED_AUTHENTICATION_TOKEN_KEY, ""))
        .isEqualTo(authenticationToken.toJSONObject().toString())

    // clear
    cache.clear()
    assertThat(sharedPreferences.contains(AuthenticationTokenCache.CACHED_AUTHENTICATION_TOKEN_KEY))
        .isFalse
  }
}
