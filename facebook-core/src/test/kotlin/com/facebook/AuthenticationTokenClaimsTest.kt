package com.facebook

import android.util.Base64
import com.facebook.util.common.AuthenticationTokenTestUtil
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class AuthenticationTokenClaimsTest : FacebookPowerMockTestCase() {

  private var claimsMap = hashMapOf<String, Any>()

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)
    claimsMap["jti"] = "jti"
    claimsMap["sub"] = "1234"
    claimsMap["iss"] = "https://facebook.com/dialog/oauth"
    claimsMap["aud"] = AuthenticationTokenTestUtil.APP_ID
    claimsMap["nonce"] = "some nonce"
    claimsMap["exp"] = 1_516_259_022
    claimsMap["iat"] = 1_516_239_022
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing jti throws`() {
    claimsMap.remove("jti")
    val missingJti = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingJti.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing iss throws`() {
    claimsMap.remove("iss")
    val missingIss = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingIss.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing aud throws`() {
    claimsMap.remove("aud")
    val missingAud = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingAud.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing nonce throws`() {
    claimsMap.remove("nonce")
    val missingNonce = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingNonce.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing sub throws`() {
    claimsMap.remove("sub")
    val missingSub = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingSub.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = JSONException::class)
  fun `test throw - invalid json format`() {
    val invalidJson = "123"
    val encodedClaimsString = Base64.encodeToString(invalidJson.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test empty encode claims string throws`() {
    AuthenticationTokenClaims("")
  }

  @Test
  fun `test constructor with required encoded claims`() {
    val encodedClaims =
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly
            .toEnCodedString()
    val authenticationToken = AuthenticationTokenClaims(encodedClaims)
    Assert.assertEquals(
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.sub,
        authenticationToken.sub)
    Assert.assertEquals(
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.jti,
        authenticationToken.jti)
    Assert.assertEquals(
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.iss,
        authenticationToken.iss)
    Assert.assertEquals(
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.aud,
        authenticationToken.aud)
    Assert.assertEquals(
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.nonce,
        authenticationToken.nonce)
    Assert.assertEquals(
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.exp,
        authenticationToken.exp)
    Assert.assertEquals(
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.iat,
        authenticationToken.iat)
  }

  @Test
  fun `test roundtrip JSONObject`() {
    // test full claims
    val jsonObject = AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST.toJSONObject()
    val deserializeClaims = AuthenticationTokenClaims.createFromJSONObject(jsonObject)
    assertThat(AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST == deserializeClaims).isTrue

    // test only required claims fields with others are null
    val jsonObjectRequired =
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.toJSONObject()
    val deserializeClaimsRequired =
        AuthenticationTokenClaims.createFromJSONObject(jsonObjectRequired)
    assertThat(
            AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly ==
                deserializeClaimsRequired)
        .isTrue
  }

  @Test
  fun `test roundtrip decode and encode`() {
    // test full claims
    val encodedString = AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST.toEnCodedString()
    val newAuthenticationTokenClaims = AuthenticationTokenClaims(encodedString)
    assertThat(
            AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST == newAuthenticationTokenClaims)
        .isTrue

    // test only required claims fields with others are null
    val encodedStringWithRequiredFields =
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly
            .toEnCodedString()
    val newClaimsWithRequiredFields = AuthenticationTokenClaims(encodedStringWithRequiredFields)
    assertThat(
            AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly ==
                newClaimsWithRequiredFields)
        .isTrue
  }
}
