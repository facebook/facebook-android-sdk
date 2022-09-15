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

import android.content.Intent
import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.AuthenticationToken
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getAutoLogAppEventsEnabled
import com.facebook.internal.NativeProtocol
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.login.AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest
import java.security.PublicKey
import java.util.ArrayList
import java.util.Date
import java.util.HashSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(LoginClient::class, FacebookSdk::class, OidcSecurityUtil::class)
class GetTokenLoginMethodHandlerTest : LoginHandlerTestCase() {
  val ID_TOKEN_STRING = getEncodedAuthTokenStringForTest()
  val NONCE = AuthenticationTokenTestUtil.NONCE

  override fun setup() {
    super.setup()
    mockStatic(FacebookSdk::class.java)
    whenever(getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)
    whenever(getAutoLogAppEventsEnabled()).thenReturn(false)

    // mock and bypass signature verification
    mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(any()))
        .thenReturn(PowerMockito.mock(PublicKey::class.java))
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)
  }

  @Test
  fun testGetTokenHandlesSuccessWithAllPermissions() {
    val bundle = Bundle()
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, ArrayList(PERMISSIONS))
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, Date().time / 1000 + EXPIRES_IN_DELTA)
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN)
    bundle.putString(NativeProtocol.EXTRA_USER_ID, USER_ID)
    bundle.putString(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN, ID_TOKEN_STRING)
    val handler = GetTokenLoginMethodHandler(mockLoginClient)
    val request = createRequestWithNonce()
    handler.getTokenCompleted(request, bundle)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(LoginClient.Result.Code.SUCCESS).isEqualTo(result.code)
    val token = result.token
    checkNotNull(token)
    assertThat(ACCESS_TOKEN).isEqualTo(token.token)
    assertDateDiffersWithinDelta(Date(), token.expires, EXPIRES_IN_DELTA * 1000, 1000)
    assertThat(PERMISSIONS).isEqualTo(token.permissions)
  }

  @Test
  fun testGetTokenHandlesSuccessWithOnlySomePermissions() {
    val bundle = Bundle()
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, ArrayList(listOf("go outside")))
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, Date().time / 1000 + EXPIRES_IN_DELTA)
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN)
    val handler = GetTokenLoginMethodHandler(mockLoginClient)
    val request = createRequest()
    assertThat(PERMISSIONS.size.toLong()).isEqualTo(request.permissions.size.toLong())
    handler.getTokenCompleted(request, bundle)
    verify(mockLoginClient, never()).completeAndValidate(any())
    verify(mockLoginClient, times(1)).tryNextHandler()
  }

  @Test
  fun testGetTokenHandlesNoResult() {
    val handler = GetTokenLoginMethodHandler(mockLoginClient)
    val request = createRequest()
    assertThat(PERMISSIONS.size.toLong()).isEqualTo(request.permissions.size.toLong())
    handler.getTokenCompleted(request, null)
    verify(mockLoginClient, never()).completeAndValidate(any())
    verify(mockLoginClient, times(1)).tryNextHandler()
  }

  @Test
  fun testFromNativeLogin() {
    val permissions = ArrayList<String>()
    permissions.add("stream_publish")
    permissions.add("go_outside_and_play")
    val token = "AnImaginaryTokenValue"
    val userId = "1000"
    val nowSeconds = Date().time / 1000
    val intent = Intent()
    intent.putExtra(NativeProtocol.EXTRA_ACCESS_TOKEN, token)
    intent.putExtra(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN, ID_TOKEN_STRING)
    intent.putExtra(NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, nowSeconds + 60L)
    intent.putExtra(NativeProtocol.EXTRA_PERMISSIONS, permissions)
    intent.putExtra(NativeProtocol.EXTRA_USER_ID, userId)
    val extras = checkNotNull(intent.extras)
    val accessToken: AccessToken? =
        LoginMethodHandler.createAccessTokenFromNativeLogin(
            extras, AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, "1234")
    checkNotNull(accessToken)
    assertThat(permissions.toSet()).isEqualTo(accessToken.permissions)
    assertThat(token).isEqualTo(accessToken.token)
    assertThat(AccessTokenSource.FACEBOOK_APPLICATION_NATIVE).isEqualTo(accessToken.source)
    assertThat(accessToken.isExpired).isFalse
    val authenticationToken: AuthenticationToken? =
        LoginMethodHandler.createAuthenticationTokenFromNativeLogin(extras, NONCE)
    checkNotNull(authenticationToken)
    assertThat(ID_TOKEN_STRING).isEqualTo(authenticationToken.token)
  }

  /**
   * This can happens when there is compatibility issue, thus no authentication string returned
   * Login still works, but just no id_token is created or returned
   */
  @Test
  fun testAllPermissionWithNoAuthenticationStringStillSuccess() {
    val bundle = Bundle()
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, ArrayList(PERMISSIONS))
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, Date().time / 1000 + EXPIRES_IN_DELTA)
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN)
    bundle.putString(NativeProtocol.EXTRA_USER_ID, USER_ID)
    val handler = GetTokenLoginMethodHandler(mockLoginClient)
    val request = createRequestWithNonce()
    handler.getTokenCompleted(request, bundle)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(LoginClient.Result.Code.SUCCESS).isEqualTo(result.code)
    val token = result.token
    checkNotNull(token)
    assertThat(ACCESS_TOKEN).isEqualTo(token.token)
    assertDateDiffersWithinDelta(Date(), token.expires, EXPIRES_IN_DELTA * 1000, 1000)
    assertThat(PERMISSIONS).isEqualTo(token.permissions)

    // when no id_token string is returned from fb4a, we expect no authentication Token is created
    // but Get Token should still works even id_token is null
    val authenticationToken = result.authenticationToken
    assertThat(authenticationToken).isNull()
  }

  /** Case when id_token returned, but it is invalid */
  @Test
  fun testAllPermissionWithAuthenticationValidationError() {
    // force token signature validation to fail
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(false)
    val bundle = Bundle()
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, ArrayList(PERMISSIONS))
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, Date().time / 1000 + EXPIRES_IN_DELTA)
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN)
    bundle.putString(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN, ID_TOKEN_STRING)
    bundle.putString(NativeProtocol.EXTRA_USER_ID, USER_ID)
    val handler = GetTokenLoginMethodHandler(mockLoginClient)
    val request = createRequestWithNonce()
    handler.getTokenCompleted(request, bundle)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(LoginClient.Result.Code.ERROR).isEqualTo(result.code)
  }

  /**
   * Make sure we fallback to try next handler when permission contains openid but the result
   * returned does not contains id_token
   */
  @Test
  fun testGetTokenToTryNextHandlerWithOpenIdButNoIdToken() {
    val openIdPermission = HashSet(listOf("openid"))
    val bundle = Bundle()
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, ArrayList(openIdPermission))
    bundle.putLong(
        NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, Date().time / 1000 + EXPIRES_IN_DELTA)
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, ACCESS_TOKEN)
    val handler = GetTokenLoginMethodHandler(mockLoginClient)
    val request = createRequestWithNonce()
    handler.getTokenCompleted(request, bundle)
    verify(mockLoginClient, never()).completeAndValidate(any())
    verify(mockLoginClient, times(1)).tryNextHandler()
  }
}
