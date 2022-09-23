/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.security

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.spec.InvalidKeySpecException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(OidcSecurityUtil::class, FacebookSdk::class)
class OidcSecurityUtilTest : FacebookPowerMockTestCase() {
  private val encodedHeader = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
  private val encodedClaims =
      "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0"
  private val signature =
      "POstGetfAytaZS82wHcjoTyoqhMyxXiWdR7Nn7A29DNSl0EiXLdwJ6xC6AfgZWF1bOsS_TuYI3OG85AmiExREkrS6tDfTQ2B3WXlrr-wp5AokiRbz3_oB4OxG-W9KcEEbDRcZc0nH3L7LzYptiy1PtAylQGxHTWZXtGz4ht0bAecBgmpdgXMguEIcoqPJ1n3pIWk_dUZegpqx0Lka21H6XxUTxiy8OcaarA8zdnPUnV6AmNP3ecFawIFYdvJB_cm-GvpCSbr8G8y_Mllj8f4x9nBH8pQux89_6gUY618iYv7tuPWBFfEbLxtF2pZS6YC1aSfLQxeNe8djT9YjpvRZA"
  private val pubKeyString =
      "-----BEGIN PUBLIC KEY-----\n" +
          "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv\n" +
          "vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc\n" +
          "aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy\n" +
          "tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0\n" +
          "e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb\n" +
          "V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9\n" +
          "MwIDAQAB\n" +
          "-----END PUBLIC KEY-----"

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getFacebookDomain()).thenReturn("facebook.com")
    whenever(FacebookSdk.getExecutor()).thenReturn(FacebookSerialExecutor())

    val urlMock: URL = mock()
    val huc: HttpURLConnection = mock()
    PowerMockito.whenNew(URL::class.java)
        .withArguments("https", "www.facebook.com", OidcSecurityUtil.OPENID_KEYS_PATH)
        .thenReturn(urlMock)
    whenever(urlMock.openConnection()).thenReturn(huc)

    val jsonString = "{\"kid\":\"abc\"}"
    val inputStream = ByteArrayInputStream(jsonString.toByteArray())
    whenever(huc.inputStream).thenReturn(inputStream)
  }

  @Test
  fun `test getRawKeyFromEndPoint succeed`() {
    val result = OidcSecurityUtil.getRawKeyFromEndPoint("kid")
    assertThat("abc").isEqualTo(result)
  }

  @Test
  fun `test getRawKeyFromEndPoint kid not found`() {
    val result = OidcSecurityUtil.getRawKeyFromEndPoint("incorrect_kid")
    assertThat("").isEqualTo(result)
  }

  @Test
  fun `test creating public key and verify succeed`() {
    val pubKey = OidcSecurityUtil.getPublicKeyFromString(pubKeyString)
    val isValid = OidcSecurityUtil.verify(pubKey, "$encodedHeader.$encodedClaims", signature)
    assertThat(isValid).isTrue
  }

  @Test(expected = InvalidKeySpecException::class)
  fun `test creating incorrect public key throws`() {
    OidcSecurityUtil.getPublicKeyFromString("not_correct_publicKey")
  }

  @Test(expected = InvalidKeySpecException::class)
  fun `test creating empty public key throws`() {
    OidcSecurityUtil.getPublicKeyFromString("")
  }

  @Test
  fun `test verify fail with incorrect signature`() {
    val pubKey = OidcSecurityUtil.getPublicKeyFromString(pubKeyString)
    val isValid = OidcSecurityUtil.verify(pubKey, "$encodedHeader.$encodedClaims", "abc")
    assertThat(isValid).isFalse
  }

  @Test
  fun `test getRawKeyFromEndPoint return null when IOException happens`() {
    val urlMock: URL = mock()
    val huc: HttpURLConnection = mock()
    PowerMockito.whenNew(URL::class.java)
        .withArguments("https", "www.facebook.com", OidcSecurityUtil.OPENID_KEYS_PATH)
        .thenReturn(urlMock)
    whenever(urlMock.openConnection()).thenReturn(huc)
    whenever(huc.inputStream).thenThrow(IOException::class.java)

    val result = OidcSecurityUtil.getRawKeyFromEndPoint("kid")
    assertThat(result).isNull()
  }
}
