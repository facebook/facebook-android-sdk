package com.facebook

import android.util.Base64
import com.facebook.util.common.AuthenticationTokenTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class AuthenticationTokenHeaderTest : FacebookPowerMockTestCase() {
  private lateinit var headerMap: HashMap<String, String>

  @Before
  fun before() {
    headerMap = hashMapOf("alg" to "RS256", "typ" to "token_type", "kid" to "abc")
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

  @Ignore // TODO: Re-enable when flakiness is fixed T98889789
  @Test
  fun `test constructor with required encoded header`() {
    val authenticationHeader =
        AuthenticationTokenHeader(AuthenticationTokenTestUtil.VALID_HEADER_STRING)
    assertThat(authenticationHeader.alg).isEqualTo("RS256")
    assertThat(authenticationHeader.typ).isEqualTo("token_type")
    assertThat(authenticationHeader.kid).isEqualTo("abc")
  }

  @Test
  fun `test parceling`() {
    val idTokenHeader1 = AuthenticationTokenHeader("alg", "typ", "kid")
    val idTokenHeader2 = FacebookTestUtility.parcelAndUnparcel(idTokenHeader1)
    assertThat(idTokenHeader2).isNotNull
    assertThat(idTokenHeader1.alg).isEqualTo(idTokenHeader2?.alg)
    assertThat(idTokenHeader1.typ).isEqualTo(idTokenHeader2?.typ)
    assertThat(idTokenHeader1.kid).isEqualTo(idTokenHeader2?.kid)
  }

  @Test
  fun `test json roundtrip`() {
    val jsonObject = AuthenticationTokenTestUtil.AUTH_TOKEN_HEADER_FOR_TEST.toJSONObject()
    val deserializeHeader = AuthenticationTokenHeader(jsonObject)
    assertThat(AuthenticationTokenTestUtil.AUTH_TOKEN_HEADER_FOR_TEST).isEqualTo(deserializeHeader)
  }
}
