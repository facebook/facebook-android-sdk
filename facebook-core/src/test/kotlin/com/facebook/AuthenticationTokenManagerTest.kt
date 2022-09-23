/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.Utility
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.util.common.AuthenticationTokenTestUtil
import com.facebook.util.common.mockLocalBroadcastManager
import java.security.PublicKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FacebookSdk::class, AuthenticationTokenCache::class, Utility::class, OidcSecurityUtil::class)
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
    val authenticationTokenManager = createAuthenticationTokenManager()
    val result = authenticationTokenManager.loadCurrentAuthenticationToken()
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

  @Test
  fun testSaveWritesToCacheIfToken() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.currentAuthenticationToken = authenticationToken
    verify(authenticationTokenCache, times(1)).save(any())
  }

  @Test
  fun testSetEmptyTokenClearsCache() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.currentAuthenticationToken = null
    verify(authenticationTokenCache, times(1)).clear()
  }

  @Test
  fun testLoadDoesNotSave() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    whenever(authenticationTokenCache.load()).thenReturn(authenticationToken)
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.loadCurrentAuthenticationToken()
    verify(authenticationTokenCache, never()).save(any())
  }

  @Test
  fun testChangingAuthenticationTokenSendsBroadcast() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    authenticationTokenManager.currentAuthenticationToken = authenticationToken
    val intents = arrayOfNulls<Intent>(1)
    val broadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
            intents[0] = intent
          }
        }
    localBroadcastManager.registerReceiver(
        broadcastReceiver,
        IntentFilter(AuthenticationTokenManager.ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED))
    val anotherAuthenticationToken =
        AuthenticationTokenTestUtil.getAuthenticationTokenEmptyOptionalClaimsForTest()
    authenticationTokenManager.currentAuthenticationToken = anotherAuthenticationToken
    localBroadcastManager.unregisterReceiver(broadcastReceiver)
    val intent = intents[0]
    assertThat(intent).isNotNull()
    checkNotNull(intent)
    val oldAuthenticationToken: AuthenticationToken =
        checkNotNull(
            intent.getParcelableExtra(AuthenticationTokenManager.EXTRA_OLD_AUTHENTICATION_TOKEN))
    val newAuthenticationToken: AuthenticationToken =
        checkNotNull(
            intent.getParcelableExtra(AuthenticationTokenManager.EXTRA_NEW_AUTHENTICATION_TOKEN))
    assertThat(authenticationToken.token).isEqualTo(oldAuthenticationToken.token)
    assertThat(anotherAuthenticationToken.token).isEqualTo(newAuthenticationToken.token)
  }
}
