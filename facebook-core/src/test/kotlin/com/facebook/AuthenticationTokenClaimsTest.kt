package com.facebook

import org.json.JSONException
import org.junit.Assert
import org.junit.Test

class AuthenticationTokenClaimsTest : FacebookPowerMockTestCase() {
  companion object {
    const val ENCODED_CLAIMS_VALID =
        "eyJqdGkiOiIxMjM0NTY3ODkiLCJzdWIiOiIxMjM0IiwibmFtZSI6IlRlc3QgVXNlciIsImlzcyI6Imh0dHBzOi8vZmFjZWJvb2suY29tL2RpYWxvZy9vYXV0aCIsImF1ZCI6IjQzMjEiLCJub25jZSI6InNvbWVfbm9uY2UiLCJleHAiOjE1MTYyNTkwMjIsImVtYWlsIjoiZW1haWxAZW1haWwuY29tIiwicGljdHVyZSI6Imh0dHBzOi8vd3d3LmZhY2Vib29rLmNvbS9zb21lX3BpY3R1cmUiLCJpYXQiOjE1MTYyMzkwMjJ9"
    const val ENCODED_CLAIMS_NO_JTI =
        "eyJzdWIiOiIxMjM0IiwibmFtZSI6IlRlc3QgVXNlciIsImlzcyI6Imh0dHBzOi8vZmFjZWJvb2suY29tL2RpYWxvZy9vYXV0aCIsImF1ZCI6IjQzMjEiLCJub25jZSI6InNvbWVfbm9uY2UiLCJleHAiOjE1MTYyNTkwMjIsImVtYWlsIjoiZW1haWxAZW1haWwuY29tIiwicGljdHVyZSI6Imh0dHBzOi8vd3d3LmZhY2Vib29rLmNvbS9zb21lX3BpY3R1cmUiLCJpYXQiOjE1MTYyMzkwMjJ9"
    const val ENCODED_CLAIMS_NO_ISS =
        "eyJqdGkiOiIxMjM0NTY3ODkiLCJzdWIiOiIxMjM0IiwibmFtZSI6IlRlc3QgVXNlciIsImF1ZCI6IjQzMjEiLCJub25jZSI6InNvbWVfbm9uY2UiLCJleHAiOjE1MTYyNTkwMjIsImVtYWlsIjoiZW1haWxAZW1haWwuY29tIiwicGljdHVyZSI6Imh0dHBzOi8vd3d3LmZhY2Vib29rLmNvbS9zb21lX3BpY3R1cmUiLCJpYXQiOjE1MTYyMzkwMjJ9"
    const val ENCODED_CLAIMS_NO_AUD =
        "eyJqdGkiOiIxMjM0NTY3ODkiLCJzdWIiOiIxMjM0IiwibmFtZSI6IlRlc3QgVXNlciIsImlzcyI6Imh0dHBzOi8vZmFjZWJvb2suY29tL2RpYWxvZy9vYXV0aCIsIm5vbmNlIjoic29tZV9ub25jZSIsImV4cCI6MTUxNjI1OTAyMiwiZW1haWwiOiJlbWFpbEBlbWFpbC5jb20iLCJwaWN0dXJlIjoiaHR0cHM6Ly93d3cuZmFjZWJvb2suY29tL3NvbWVfcGljdHVyZSIsImlhdCI6MTUxNjIzOTAyMn0="
    const val ENCODED_CLAIMS_NO_NONCE =
        "eyJqdGkiOiIxMjM0NTY3ODkiLCJzdWIiOiIxMjM0IiwibmFtZSI6IlRlc3QgVXNlciIsImlzcyI6Imh0dHBzOi8vZmFjZWJvb2suY29tL2RpYWxvZy9vYXV0aCIsImF1ZCI6IjQzMjEiLCJleHAiOjE1MTYyNTkwMjIsImVtYWlsIjoiZW1haWxAZW1haWwuY29tIiwicGljdHVyZSI6Imh0dHBzOi8vd3d3LmZhY2Vib29rLmNvbS9zb21lX3BpY3R1cmUiLCJpYXQiOjE1MTYyMzkwMjJ9"
    const val ENCODED_CLAIMS_NO_SUB =
        "eyJqdGkiOiIxMjM0NTY3ODkiLCJuYW1lIjoiVGVzdCBVc2VyIiwiaXNzIjoiaHR0cHM6Ly9mYWNlYm9vay5jb20vZGlhbG9nL29hdXRoIiwiYXVkIjoiNDMyMSIsIm5vbmNlIjoic29tZV9ub25jZSIsImV4cCI6MTUxNjI1OTAyMiwiZW1haWwiOiJlbWFpbEBlbWFpbC5jb20iLCJwaWN0dXJlIjoiaHR0cHM6Ly93d3cuZmFjZWJvb2suY29tL3NvbWVfcGljdHVyZSIsImlhdCI6MTUxNjIzOTAyMn0="
    const val ENCODED_CLAIMS_INVALID_JSON = "notvalid"
  }

  @Test(expected = JSONException::class)
  fun `test missing jti throws`() {
    AuthenticationTokenClaims(ENCODED_CLAIMS_NO_JTI)
  }

  @Test(expected = JSONException::class)
  fun `test missing iss throws`() {
    AuthenticationTokenClaims(ENCODED_CLAIMS_NO_ISS)
  }

  @Test(expected = JSONException::class)
  fun `test missing aud throws`() {
    AuthenticationTokenClaims(ENCODED_CLAIMS_NO_AUD)
  }

  @Test(expected = JSONException::class)
  fun `test empty nonce throws`() {
    AuthenticationTokenClaims(ENCODED_CLAIMS_NO_NONCE)
  }

  @Test(expected = JSONException::class)
  fun `test empty sub throws`() {
    AuthenticationTokenClaims(ENCODED_CLAIMS_NO_SUB)
  }

  @Test(expected = JSONException::class)
  fun `test throw - invalid encoded claims string which is invalid json format`() {
    AuthenticationTokenClaims(ENCODED_CLAIMS_INVALID_JSON)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test empty encode claims string throws`() {
    AuthenticationTokenClaims("")
  }

  @Test
  fun `test constructor with required encoded claims`() {
    val authenticationToken = AuthenticationTokenClaims(ENCODED_CLAIMS_VALID)
    Assert.assertEquals(authenticationToken.name, "Test User")
    Assert.assertEquals(authenticationToken.sub, "1234")
    Assert.assertEquals(authenticationToken.jti, "123456789")
    Assert.assertEquals(authenticationToken.iss, "https://facebook.com/dialog/oauth")
    Assert.assertEquals(authenticationToken.aud, "4321")
    Assert.assertEquals(authenticationToken.nonce, "some_nonce")
    Assert.assertEquals(authenticationToken.exp.time, 1516259022)
    Assert.assertEquals(authenticationToken.iat.time, 1516239022)
  }
}
