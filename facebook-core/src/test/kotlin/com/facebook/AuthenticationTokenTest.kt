/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.util.common.AuthenticationTokenTestUtil
import java.security.PublicKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, OidcSecurityUtil::class, LocalBroadcastManager::class)
class AuthenticationTokenTest : FacebookPowerMockTestCase() {
  val publicKeyString =
      "-----BEGIN PUBLIC KEY-----\n" +
          "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv\n" +
          "vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc\n" +
          "aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy\n" +
          "tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0\n" +
          "e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb\n" +
          "V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9\n" +
          "MwIDAQAB\n" +
          "-----END PUBLIC KEY-----"
  val headerString = AuthenticationTokenTestUtil.VALID_HEADER_STRING
  val claimsString = AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST.toEnCodedString()
  val signatureString = "signature"
  val tokenString = "$headerString.$claimsString.$signatureString"

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn(publicKeyString)
    val pubKey = PowerMockito.mock(PublicKey::class.java)
    whenever(OidcSecurityUtil.getPublicKeyFromString(publicKeyString)).thenReturn(pubKey)

    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    val mockLocalBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(any())).thenReturn(mockLocalBroadcastManager)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test empty token throws`() {
    AuthenticationToken("", "nonce")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test invalid token format`() {
    // Correct format should be [abc.def.ghi]
    AuthenticationToken("abc.def", "nonce")
  }

  @Test
  fun `test AuthenticationToken constructor succeed`() {
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)
    val authenticationToken = AuthenticationToken(tokenString, AuthenticationTokenTestUtil.NONCE)
    assertThat(tokenString).isEqualTo(authenticationToken.token)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test AuthenticationToken constructor fail when signature verification fail`() {
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(false)
    AuthenticationToken(tokenString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test AuthenticationToken constructor fail when signature verification with getRawKeyFromEndPoint return null`() {
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn(null)
    AuthenticationToken(tokenString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test AuthenticationToken constructor fail when create public key fail`() {
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("invalid_key")
    AuthenticationToken(tokenString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test
  fun `test AuthenticationToken parceling`() {
    // bypass signature check, since this is not testing for signature
    PowerMockito.`when`(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    val idToken1 = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    val idToken2 = FacebookTestUtility.parcelAndUnparcel(idToken1)
    assertThat(idToken2).isNotNull
    assertThat(idToken1).isEqualTo(idToken2)
  }

  @Test
  fun `test setting and getting the current AuthenticationToken to or from cache`() {
    // bypass signature check, since this is not testing for signature
    PowerMockito.`when`(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    val expectedToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    AuthenticationToken.setCurrentAuthenticationToken(expectedToken)
    val actualToken = AuthenticationToken.getCurrentAuthenticationToken()
    assertThat(expectedToken).isEqualTo(actualToken)
  }

  @Test
  fun `test json roundtrip`() {
    // bypass signature check, since this is not testing for signature
    PowerMockito.`when`(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    val jsonObject = authenticationToken.toJSONObject()
    val deserializeToken = AuthenticationToken(jsonObject)
    assertThat(authenticationToken).isEqualTo(deserializeToken)
  }
}
