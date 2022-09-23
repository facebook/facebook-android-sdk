/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.login.AuthenticationTokenTestUtil
import com.facebook.util.common.mockLocalBroadcastManager
import java.security.PublicKey
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.RuntimeEnvironment

@PrepareForTest(FacebookSdk::class, LocalBroadcastManager::class, OidcSecurityUtil::class)
class AuthenticationTokenTrackerTest : FacebookPowerMockTestCase() {

  private lateinit var authenticationTokenTracker: TestAuthenticationTokenTracker
  private lateinit var localBroadcastManager: LocalBroadcastManager

  private val mockExecutor: Executor = FacebookSerialExecutor()

  @Before
  @Throws(Exception::class)
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(any()))
        .thenReturn(PowerMockito.mock(PublicKey::class.java))
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    Whitebox.setInternalState(FacebookSdk::class.java, "executor", mockExecutor)
    localBroadcastManager = mockLocalBroadcastManager(RuntimeEnvironment.application)
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(FacebookSdk.getApplicationContext()))
        .thenReturn(localBroadcastManager)
    authenticationTokenTracker = TestAuthenticationTokenTracker()
  }

  @After
  fun after() {
    if (authenticationTokenTracker != null && authenticationTokenTracker.isTracking) {
      authenticationTokenTracker.stopTracking()
    }
  }

  @Test(expected = FacebookSdkNotInitializedException::class)
  fun testRequiresSdkToBeInitialized() {
    whenever(FacebookSdk.isInitialized()).thenReturn(false)
    authenticationTokenTracker = TestAuthenticationTokenTracker()
  }

  @Test
  fun testDefaultsToTracking() {
    authenticationTokenTracker = TestAuthenticationTokenTracker()
    assertThat(authenticationTokenTracker.isTracking).isTrue
  }

  @Test
  fun testCanTurnTrackingOff() {
    authenticationTokenTracker = TestAuthenticationTokenTracker()
    authenticationTokenTracker.stopTracking()
    assertThat(authenticationTokenTracker.isTracking).isFalse
  }

  @Test
  fun testCanTurnTrackingOn() {
    authenticationTokenTracker = TestAuthenticationTokenTracker()
    authenticationTokenTracker.stopTracking()
    authenticationTokenTracker.startTracking()
    assertThat(authenticationTokenTracker.isTracking).isTrue
  }

  @Test
  @Throws(java.lang.Exception::class)
  fun testCallbackCalledOnBroadcastReceived() {
    authenticationTokenTracker = TestAuthenticationTokenTracker()
    val oldAuthenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    val currentAuthenticationToken =
        AuthenticationTokenTestUtil.getAuthenticationTokenEmptyOptionalClaimsForTest()
    sendBroadcast(oldAuthenticationToken, currentAuthenticationToken)
    assertThat(authenticationTokenTracker.currentAuthenticationToken).isNotNull
    assertThat(currentAuthenticationToken.token)
        .isEqualTo(authenticationTokenTracker.currentAuthenticationToken?.token)
    assertThat(authenticationTokenTracker.oldAuthenticationToken).isNotNull
    assertThat(oldAuthenticationToken.token)
        .isEqualTo(authenticationTokenTracker.oldAuthenticationToken?.token)
  }

  private fun sendBroadcast(
      oldAuthenticationToken: AuthenticationToken,
      currentAuthenticationToken: AuthenticationToken
  ) {
    val intent = Intent(AuthenticationTokenManager.ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED)
    intent.putExtra(
        AuthenticationTokenManager.EXTRA_OLD_AUTHENTICATION_TOKEN, oldAuthenticationToken)
    intent.putExtra(
        AuthenticationTokenManager.EXTRA_NEW_AUTHENTICATION_TOKEN, currentAuthenticationToken)
    localBroadcastManager.sendBroadcast(intent)
  }

  private class TestAuthenticationTokenTracker : AuthenticationTokenTracker() {
    var oldAuthenticationToken: AuthenticationToken? = null
    var currentAuthenticationToken: AuthenticationToken? = null

    override fun onCurrentAuthenticationTokenChanged(
        oldAuthenticationToken: AuthenticationToken?,
        currentAuthenticationToken: AuthenticationToken?
    ) {
      this.oldAuthenticationToken = oldAuthenticationToken
      this.currentAuthenticationToken = currentAuthenticationToken
    }
  }
}
