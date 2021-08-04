package com.facebook

import android.util.Base64
import java.util.Calendar
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthenticationTokenClaimsTest : FacebookPowerMockTestCase() {
  private val authenticationTokenClaims =
      AuthenticationTokenClaims(
          "jti",
          "iss",
          "aud",
          "nonce",
          Date(1630191912), // 8/28/2021
          Calendar.getInstance().time,
          "sub",
          "name",
          "givenName",
          "middleName",
          "familyName",
          "email",
          "picture",
          listOf("friend1", "friend2"),
          "userBirthday",
          hashMapOf("min" to 20),
          hashMapOf("id" to "112724962075996", "name" to "Martinez, California"),
          hashMapOf("id" to "110843418940484", "name" to "Seattle, Washington"),
          "male",
          "facebook.com")

  private val authenticationTokenClaimsWithRequiredFieldsOnly =
      AuthenticationTokenClaims(
          "jti",
          "iss",
          "aud",
          "nonce",
          Date(1630191912), // 8/28/2021
          Calendar.getInstance().time,
          "sub")

  private var claimsMap = hashMapOf<String, Any>()

  @Before
  fun before() {
    claimsMap["jti"] = "jti"
    claimsMap["sub"] = "1234"
    claimsMap["iss"] = "https://facebook.com/dialog/oauth"
    claimsMap["aud"] = "4321"
    claimsMap["nonce"] = "some nonce"
    claimsMap["exp"] = 1516259022
    claimsMap["iat"] = 1516239022
  }

  @Test(expected = JSONException::class)
  fun `test missing jti throws`() {
    claimsMap.remove("jti")
    val missingJti = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingJti.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = JSONException::class)
  fun `test missing iss throws`() {
    claimsMap.remove("iss")
    val missingIss = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingIss.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = JSONException::class)
  fun `test missing aud throws`() {
    claimsMap.remove("aud")
    val missingAud = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingAud.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = JSONException::class)
  fun `test empty nonce throws`() {
    claimsMap.remove("nonce")
    val missingNonce = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingNonce.toByteArray(), Base64.DEFAULT)
    AuthenticationTokenClaims(encodedClaimsString)
  }

  @Test(expected = JSONException::class)
  fun `test empty sub throws`() {
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
    val encodedClaims = authenticationTokenClaimsWithRequiredFieldsOnly.toEnCodedString()
    val authenticationToken = AuthenticationTokenClaims(encodedClaims)
    Assert.assertEquals(
        authenticationTokenClaimsWithRequiredFieldsOnly.sub, authenticationToken.sub)
    Assert.assertEquals(
        authenticationTokenClaimsWithRequiredFieldsOnly.jti, authenticationToken.jti)
    Assert.assertEquals(
        authenticationTokenClaimsWithRequiredFieldsOnly.iss, authenticationToken.iss)
    Assert.assertEquals(
        authenticationTokenClaimsWithRequiredFieldsOnly.aud, authenticationToken.aud)
    Assert.assertEquals(
        authenticationTokenClaimsWithRequiredFieldsOnly.nonce, authenticationToken.nonce)
    Assert.assertEquals(
        authenticationTokenClaimsWithRequiredFieldsOnly.exp, authenticationToken.exp)
    Assert.assertEquals(
        authenticationTokenClaimsWithRequiredFieldsOnly.iat, authenticationToken.iat)
  }

  @Test
  fun `test roundtrip JSONObject`() {
    // test full claims
    val jsonObject = authenticationTokenClaims.toJSONObject()
    val deserializeClaims = AuthenticationTokenClaims.createFromJSONObject(jsonObject)
    assertThat(authenticationTokenClaims == deserializeClaims).isTrue

    // test only required claims fields with others are null
    val jsonObjectRequired = authenticationTokenClaimsWithRequiredFieldsOnly.toJSONObject()
    val deserializeClaimsRequired =
        AuthenticationTokenClaims.createFromJSONObject(jsonObjectRequired)
    assertThat(authenticationTokenClaimsWithRequiredFieldsOnly == deserializeClaimsRequired).isTrue
  }

  @Test
  fun `test roundtrip decode and encode`() {
    // test full claims
    val encodedString = authenticationTokenClaims.toEnCodedString()
    val newAuthenticationTokenClaims = AuthenticationTokenClaims(encodedString)
    assertThat(authenticationTokenClaims == newAuthenticationTokenClaims).isTrue

    // test only required claims fields with others are null
    val encodedStringWithRequiredFields =
        authenticationTokenClaimsWithRequiredFieldsOnly.toEnCodedString()
    val newClaimsWithRequiredFields = AuthenticationTokenClaims(encodedStringWithRequiredFields)
    assertThat(authenticationTokenClaimsWithRequiredFieldsOnly == newClaimsWithRequiredFields)
        .isTrue
  }
}
