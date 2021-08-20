package com.facebook.internal.security

import com.facebook.FacebookPowerMockTestCase
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.spec.InvalidKeySpecException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(OidcSecurityUtil::class)
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
  fun `before`() {
    val urlMock = PowerMockito.mock(URL::class.java)
    val huc = PowerMockito.mock(HttpURLConnection::class.java)
    PowerMockito.whenNew(URL::class.java)
        .withArguments(OidcSecurityUtil.OPENID_KEYS_URL_STRING)
        .thenReturn(urlMock)
    PowerMockito.`when`(urlMock.openConnection()).thenReturn(huc)

    val jsonString = "{\"kid\":\"abc\"}"
    val inputStream = ByteArrayInputStream(jsonString.toByteArray())
    PowerMockito.`when`(huc.inputStream).thenReturn(inputStream)
  }

  @Test
  fun `test getRawKeyFromEndPoint succeed`() {
    val result = OidcSecurityUtil.getRawKeyFromEndPoint("kid")
    Assert.assertEquals("abc", result)
  }

  @Test
  fun `test getRawKeyFromEndPoint kid not found`() {
    val result = OidcSecurityUtil.getRawKeyFromEndPoint("incorrect_kid")
    Assert.assertEquals("", result)
  }

  @Test
  fun `test creating public key and verify succeed`() {
    val pubKey = OidcSecurityUtil.getPublicKeyFromString(pubKeyString)
    val isValid = OidcSecurityUtil.verify(pubKey, "$encodedHeader.$encodedClaims", signature)
    Assert.assertTrue(isValid)
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
    Assert.assertFalse(isValid)
  }
}
