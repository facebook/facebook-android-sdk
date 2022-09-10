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

package com.facebook.login

import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessToken
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.login.AuthenticationTokenTestUtil.getAuthenticationTokenForTest
import java.security.PublicKey
import java.util.HashSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, OidcSecurityUtil::class)
class LoginResultTest : FacebookPowerMockTestCase() {
  private val EMAIL_SET = setOf("email")
  private val LIKES_EMAIL_SET = setOf("user_likes", "email")
  private val PROFILE_EMAIL_SET = setOf("user_profile", "email")

  @Before
  fun setUp() {
    super.setup()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())

    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(anyString())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(anyString()))
        .thenReturn(mock(PublicKey::class.java))
    whenever(OidcSecurityUtil.verify(any(), anyString(), anyString())).thenReturn(true)
  }

  @Test
  fun testInitialLogin() {
    val request = createRequest(EMAIL_SET, false)
    val accessToken = createAccessToken(PROFILE_EMAIL_SET, HashSet(), HashSet())
    val authenticationToken = getAuthenticationTokenForTest()
    val result = LoginManager.computeLoginResult(request, accessToken, authenticationToken)
    assertThat(accessToken).isEqualTo(result.accessToken)
    assertThat(authenticationToken).isEqualTo(result.authenticationToken)
    assertThat(PROFILE_EMAIL_SET).isEqualTo(result.recentlyGrantedPermissions)
    assertThat(0).isEqualTo(result.recentlyDeniedPermissions.size)
  }

  @Test
  fun testReAuth() {
    val request = createRequest(EMAIL_SET, true)
    val accessToken = createAccessToken(PROFILE_EMAIL_SET, HashSet(), HashSet())
    val authenticationToken = getAuthenticationTokenForTest()
    val result = LoginManager.computeLoginResult(request, accessToken, authenticationToken)
    assertThat(accessToken).isEqualTo(result.accessToken)
    assertThat(authenticationToken).isEqualTo(result.authenticationToken)
    assertThat(EMAIL_SET).isEqualTo(result.recentlyGrantedPermissions)
    assertThat(0).isEqualTo(result.recentlyDeniedPermissions.size)
  }

  @Test
  fun testDeniedPermissions() {
    val request = createRequest(LIKES_EMAIL_SET, true)
    val accessToken = createAccessToken(EMAIL_SET, HashSet(), HashSet())
    val authenticationToken = getAuthenticationTokenForTest()
    val result = LoginManager.computeLoginResult(request, accessToken, authenticationToken)
    assertThat(accessToken).isEqualTo(result.accessToken)
    assertThat(authenticationToken).isEqualTo(result.authenticationToken)
    assertThat(EMAIL_SET).isEqualTo(result.recentlyGrantedPermissions)
    assertThat(setOf("user_likes")).isEqualTo(result.recentlyDeniedPermissions)
  }

  private fun createAccessToken(
      permissions: Set<String>,
      declinedPermissions: Set<String>,
      expiredPermissions: Set<String>
  ): AccessToken {
    return AccessToken(
        "token",
        "123",
        "234",
        permissions,
        declinedPermissions,
        expiredPermissions,
        null,
        null,
        null,
        null)
  }

  private fun createRequest(permissions: Set<String>, isRerequest: Boolean): LoginClient.Request {
    val request =
        LoginClient.Request(
            LoginBehavior.NATIVE_WITH_FALLBACK,
            permissions,
            DefaultAudience.EVERYONE,
            "rerequest",
            "123",
            "authid")
    request.isRerequest = isRerequest
    return request
  }
}
