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
    val header = "header"
    val payload = "payload"
    val signature = "signature"
    val tokenString = "$header.$payload.$signature"
    val authenticationToken = AuthenticationToken(tokenString)
    Assert.assertEquals(tokenString, authenticationToken.token)
    Assert.assertEquals(header, authenticationToken.header)
    Assert.assertEquals(payload, authenticationToken.payload)
    Assert.assertEquals(signature, authenticationToken.signature)
  }
}
