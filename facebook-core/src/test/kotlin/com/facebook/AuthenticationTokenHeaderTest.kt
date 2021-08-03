package com.facebook

import java.util.*
import org.json.JSONException
import org.junit.Assert
import org.junit.Test

class AuthenticationTokenHeaderTest : FacebookPowerMockTestCase() {
  companion object {
    const val ENCODED_HEADER_VALID =
        "eyJhbGciOiJTSEEyNTYiLCJ0eXAiOiJ0b2tlbl90eXBlIiwia2lkIjoiYWJjIn0="
    const val ENCODED_HEADER_MISSING_ALG = "eyJ0eXAiOiJ0b2tlbl90eXBlIiwia2lkIjoiYWJjIn0="
    const val ENCODED_HEADER_MISSING_TYP = "eyJhbGciOiJTSEEyNTYiLCJraWQiOiJhYmMifQ=="
    const val ENCODED_HEADER_MISSING_KID = "eyJhbGciOiJTSEEyNTYiLCJ0eXAiOiJ0b2tlbl90eXBlIn0="
    const val ENCODED_HEADER_INVALID_JSON = "notvalid"
  }

  @Test(expected = JSONException::class)
  fun `test missing alg throws`() {
    AuthenticationTokenHeader(ENCODED_HEADER_MISSING_ALG)
  }

  @Test(expected = JSONException::class)
  fun `test missing typ throws`() {
    AuthenticationTokenHeader(ENCODED_HEADER_MISSING_TYP)
  }

  @Test(expected = JSONException::class)
  fun `test missing kid throws`() {
    AuthenticationTokenHeader(ENCODED_HEADER_MISSING_KID)
  }

  @Test(expected = JSONException::class)
  fun `test throw - invalid encoded header string which is invalid json format`() {
    AuthenticationTokenHeader(ENCODED_HEADER_INVALID_JSON)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test empty encode claims string throws`() {
    AuthenticationTokenHeader("")
  }

  @Test
  fun `test constructor with required encoded header`() {
    val authenticationHeader = AuthenticationTokenHeader(ENCODED_HEADER_VALID)
    Assert.assertEquals(authenticationHeader.alg, "SHA256")
    Assert.assertEquals(authenticationHeader.typ, "token_type")
    Assert.assertEquals(authenticationHeader.kid, "abc")
  }
}
