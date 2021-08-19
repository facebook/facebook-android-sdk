package com.facebook

import android.util.Base64
import com.facebook.util.common.AuthenticationTokenTestUtil
import java.util.*
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthenticationTokenHeaderTest : FacebookPowerMockTestCase() {
  private var headerMap = hashMapOf<String, Any>()

  @Before
  fun before() {
    headerMap["alg"] = "RS256"
    headerMap["typ"] = "token_type"
    headerMap["kid"] = "abc"
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing alg throws`() {
    headerMap.remove("alg")
    val missingAlg = JSONObject(headerMap as Map<*, *>).toString()
    val encodedHeaderString = Base64.encodeToString(missingAlg.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenHeader(encodedHeaderString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing typ throws`() {
    headerMap.remove("typ")
    val missingAlg = JSONObject(headerMap as Map<*, *>).toString()
    val encodedHeaderString = Base64.encodeToString(missingAlg.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenHeader(encodedHeaderString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing kid throws`() {
    headerMap.remove("kid")
    val missingAlg = JSONObject(headerMap as Map<*, *>).toString()
    val encodedHeaderString = Base64.encodeToString(missingAlg.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenHeader(encodedHeaderString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test throw - invalid encoded header string which is invalid json format`() {
    AuthenticationTokenHeader("invalid")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test empty encode claims string throws`() {
    AuthenticationTokenHeader("")
  }

  @Test
  fun `test constructor with required encoded header`() {
    val authenticationHeader =
        AuthenticationTokenHeader(AuthenticationTokenTestUtil.VALID_HEADER_STRING)
    Assert.assertEquals(authenticationHeader.alg, "RS256")
    Assert.assertEquals(authenticationHeader.typ, "token_type")
    Assert.assertEquals(authenticationHeader.kid, "abc")
  }
}
