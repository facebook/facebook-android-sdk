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

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.Utility
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.util.common.AuthenticationTokenTestUtil
import com.facebook.util.common.mockLocalBroadcastManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.security.PublicKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FacebookSdk::class,
    AuthenticationTokenCache::class,
    AuthenticationTokenManager::class,
    Utility::class,
    OidcSecurityUtil::class)
class AuthenticationTokenManagerTest : FacebookPowerMockTestCase() {
  private lateinit var localBroadcastManager: LocalBroadcastManager
  private lateinit var authenticationTokenCache: AuthenticationTokenCache

  private fun createAuthenticationTokenManager(): AuthenticationTokenManager {
    return AuthenticationTokenManager(localBroadcastManager, authenticationTokenCache)
  }

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    PowerMockito.`when`(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    PowerMockito.`when`(OidcSecurityUtil.getPublicKeyFromString(any()))
        .thenReturn(PowerMockito.mock(PublicKey::class.java))
    PowerMockito.`when`(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    authenticationTokenCache = mock()
    localBroadcastManager = mockLocalBroadcastManager(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testDefaultsToNoCurrentAuthenticationToken() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    assertThat(authenticationTokenManager.currentAuthenticationToken).isNull()
  }

  @Test
  fun testCanSetCurrentAuthenticationToken() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    authenticationTokenManager.currentAuthenticationToken = authenticationToken
    assertThat(authenticationTokenManager.currentAuthenticationToken).isEqualTo(authenticationToken)
  }

  @Test
  fun testLoadReturnsFalseIfNoCachedToken() {
    val accessTokenManager = createAuthenticationTokenManager()
    val result = accessTokenManager.loadCurrentAuthenticationToken()
    assertThat(result).isFalse
  }

  @Test
  fun testLoadReturnsTrueIfCachedToken() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    whenever(authenticationTokenCache.load()).thenReturn(authenticationToken)
    val authenticationTokenManager = createAuthenticationTokenManager()
    val result = authenticationTokenManager.loadCurrentAuthenticationToken()
    assertThat(result).isTrue
  }

  @Test
  fun testLoadSetsCurrentTokenIfCached() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    whenever(authenticationTokenCache.load()).thenReturn(authenticationToken)
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.loadCurrentAuthenticationToken()
    assertThat(authenticationTokenManager.currentAuthenticationToken).isEqualTo(authenticationToken)
  }
}
