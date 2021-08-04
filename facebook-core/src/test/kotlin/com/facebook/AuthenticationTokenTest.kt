package com.facebook

import org.junit.Assert
import org.junit.Test

class AuthenticationTokenTest : FacebookPowerMockTestCase() {

  @Test(expected = IllegalArgumentException::class)
  fun `test empty token throws`() {
    AuthenticationToken("")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test invalid token format`() {
    // Correct format should be [abc.def.ghi]
    AuthenticationToken("abc.def")
  }

  @Test
  fun `test AuthenticationToken constructor`() {
    val headerString = "eyJhbGciOiJTSEEyNTYiLCJ0eXAiOiJ0b2tlbl90eXBlIiwia2lkIjoiYWJjIn0="
    val claimsString =
        "eyJqdGkiOiIxMjM0NTY3ODkiLCJzdWIiOiIxMjM0IiwibmFtZSI6IlRlc3QgVXNlciIsImlzcyI6Imh0dHBzOi8vZmFjZWJvb2suY29tL2RpYWxvZy9vYXV0aCIsImF1ZCI6IjQzMjEiLCJub25jZSI6InNvbWVfbm9uY2UiLCJleHAiOjE1MTYyNTkwMjIsImVtYWlsIjoiZW1haWxAZW1haWwuY29tIiwicGljdHVyZSI6Imh0dHBzOi8vd3d3LmZhY2Vib29rLmNvbS9zb21lX3BpY3R1cmUiLCJpYXQiOjE1MTYyMzkwMjJ9"
    val signatureString = "signature"
    val tokenString = "$headerString.$claimsString.$signatureString"
    val authenticationToken = AuthenticationToken(tokenString)
    Assert.assertEquals(tokenString, authenticationToken.token)
    Assert.assertEquals(signatureString, authenticationToken.signature)
  }
}
