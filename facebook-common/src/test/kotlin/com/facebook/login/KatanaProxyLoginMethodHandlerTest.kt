/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.facebook.AuthenticationToken
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getAutoLogAppEventsEnabled
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.login.AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest
import java.lang.NullPointerException
import java.security.PublicKey
import java.util.Date
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
class KatanaProxyLoginMethodHandlerTest : LoginHandlerTestCase() {
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
  fun testProxyAuthHandlesSuccess() {
    testProxyAuthHandlesSuccess(encodedAuthTokenString)
  }

  @Test
  fun testProxyAuthHandlesSuccessWithEmptyAuthenticationToken() {
    val result = testProxyAuthHandlesSuccess("")
    val authenticationToken = result.authenticationToken
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun testProxyAuthHandlesSuccessWithNoAuthenticationToken() {
    val result = testProxyAuthHandlesSuccess(null)
    val authenticationToken = result.authenticationToken
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun testProxyAuthHandlesSuccessWithIdTokenAndNonce() {
    val expectedIdTokenString = getEncodedAuthTokenStringForTest()
    val bundle = Bundle()
    bundle.putLong("expires_in", EXPIRES_IN_DELTA)
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY, expectedIdTokenString)
    bundle.putString("signed_request", SIGNED_REQUEST_STR)
    val intent = Intent()
    intent.putExtras(bundle)
    val handler = KatanaProxyLoginMethodHandler(mockLoginClient)
    val request = createRequestWithNonce()
    whenever(mockLoginClient.pendingRequest).thenReturn(request)
    try {
      handler.tryAuthorize(request)
    } catch (e: NullPointerException) {
      // continue
    }
    handler.onActivityResult(0, Activity.RESULT_OK, intent)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(LoginClient.Result.Code.SUCCESS).isEqualTo(result.code)

    // make sure id_token get created with correct nonce
    val authenticationToken = result.authenticationToken
    checkNotNull(authenticationToken)
    assertThat(expectedIdTokenString).isEqualTo(authenticationToken.token)

    // make sure access_token get created
    val token = result.token
    checkNotNull(token)
    assertThat(ACCESS_TOKEN).isEqualTo(token.token)
    assertDateDiffersWithinDelta(Date(), token.expires, EXPIRES_IN_DELTA * 1000, 1000)
    assertThat(PERMISSIONS).isEqualTo(token.permissions)
  }

  @Test
  fun testProxyAuthHandlesCancel() {
    val bundle = Bundle()
    bundle.putString("error", ERROR_MESSAGE)
    val intent = Intent()
    intent.putExtras(bundle)
    val handler = KatanaProxyLoginMethodHandler(mockLoginClient)
    val request = createRequest()
    try {
      handler.tryAuthorize(request)
    } catch (e: NullPointerException) {
      // continue
    }
    handler.onActivityResult(0, Activity.RESULT_CANCELED, intent)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(LoginClient.Result.Code.CANCEL).isEqualTo(result.code)
    assertThat(result.token).isNull()
    checkNotNull(result.errorMessage)
    assertThat(result.errorMessage).contains(ERROR_MESSAGE)
  }

  @Test
  fun testProxyAuthHandlesCancelErrorMessage() {
    val bundle = Bundle()
    bundle.putString("error", "access_denied")
    val intent = Intent()
    intent.putExtras(bundle)
    val handler = KatanaProxyLoginMethodHandler(mockLoginClient)
    val request = createRequest()
    try {
      handler.tryAuthorize(request)
    } catch (e: NullPointerException) {
      // continue
    }
    handler.onActivityResult(0, Activity.RESULT_CANCELED, intent)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(LoginClient.Result.Code.CANCEL).isEqualTo(result.code)
    assertThat(result.token).isNull()
  }

  @Test
  fun testProxyAuthHandlesDisabled() {
    val bundle = Bundle()
    bundle.putString("error", "service_disabled")
    val intent = Intent()
    intent.putExtras(bundle)
    val handler = KatanaProxyLoginMethodHandler(mockLoginClient)
    val request = createRequest()
    try {
      handler.tryAuthorize(request)
    } catch (e: NullPointerException) {
      // continue
    }
    handler.onActivityResult(0, Activity.RESULT_OK, intent)
    verify(mockLoginClient, never()).completeAndValidate(any())
    verify(mockLoginClient, times(1)).tryNextHandler()
  }

  private fun testProxyAuthHandlesSuccess(authenticationString: String?): LoginClient.Result {
    val bundle = Bundle()
    bundle.putLong("expires_in", EXPIRES_IN_DELTA)
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString("authentication_token", authenticationString)
    bundle.putString("signed_request", SIGNED_REQUEST_STR)
    val intent = Intent()
    intent.putExtras(bundle)
    val handler = KatanaProxyLoginMethodHandler(mockLoginClient)
    val request = createRequest()
    whenever(mockLoginClient.pendingRequest).thenReturn(request)
    try {
      handler.tryAuthorize(request)
    } catch (e: NullPointerException) {
      // continue
    }
    handler.onActivityResult(0, Activity.RESULT_OK, intent)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(LoginClient.Result.Code.SUCCESS).isEqualTo(result.code)
    val token = result.token
    checkNotNull(token)
    assertThat(ACCESS_TOKEN).isEqualTo(token.token)
    assertDateDiffersWithinDelta(Date(), token.expires, EXPIRES_IN_DELTA * 1000, 1000)
    assertThat(PERMISSIONS).isEqualTo(token.permissions)
    return result
  }

  companion object {
    private const val SIGNED_REQUEST_STR =
        "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJjb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0"
  }
}
