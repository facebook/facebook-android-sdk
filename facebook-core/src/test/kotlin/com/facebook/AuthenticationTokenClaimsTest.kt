package com.facebook

import android.util.Base64
import com.facebook.util.common.AuthenticationTokenTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
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
    val encodedClaimsString = Base64.encodeToString(missingJti.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing iss throws`() {
    claimsMap.remove("iss")
    val missingIss = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingIss.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing aud throws`() {
    claimsMap.remove("aud")
    val missingAud = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingAud.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing nonce throws`() {
    claimsMap.remove("nonce")
    val missingNonce = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingNonce.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test nonce does not match throws`() {
    claimsMap["nonce"] = "not_nonce"
    val missingNonce = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingNonce.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test empty nonce throws`() {
    claimsMap["nonce"] = ""
    val missingNonce = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingNonce.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test missing sub throws`() {
    claimsMap.remove("sub")
    val missingSub = JSONObject(claimsMap as Map<*, *>).toString()
    val encodedClaimsString = Base64.encodeToString(missingSub.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = JSONException::class)
  fun `test throw - invalid json format`() {
    val invalidJson = "123"
    val encodedClaimsString = Base64.encodeToString(invalidJson.toByteArray(), Base64.URL_SAFE)
    AuthenticationTokenClaims(encodedClaimsString, AuthenticationTokenTestUtil.NONCE)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test empty encode claims string throws`() {
    AuthenticationTokenClaims("", AuthenticationTokenTestUtil.NONCE)
  }

  @Test
  fun `test constructor with required encoded claims`() {
    val encodedClaims =
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly
            .toEnCodedString()
    val authenticationToken =
        AuthenticationTokenClaims(encodedClaims, AuthenticationTokenTestUtil.NONCE)
    assertThat(AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.sub)
        .isEqualTo(authenticationToken.sub)
    assertThat(AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.jti)
        .isEqualTo(authenticationToken.jti)
    assertThat(AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.iss)
        .isEqualTo(authenticationToken.iss)
    assertThat(AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.aud)
        .isEqualTo(authenticationToken.aud)
    assertThat(AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.nonce)
        .isEqualTo(authenticationToken.nonce)
    assertThat(AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.exp)
        .isEqualTo(authenticationToken.exp)
    assertThat(AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly.iat)
        .isEqualTo(authenticationToken.iat)
  }

  @Test
  fun `test roundtrip JSONObject`() {
    // test full claims
    val jsonObject = AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST.toJSONObject()
    val deserializeClaims = AuthenticationTokenClaims.createFromJSONObject(jsonObject)
    assertThat(AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST == deserializeClaims).isTrue

    // test claims with empty optional fields
    val jsonObjectEmptyOptionalFields =
        AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_WITH_EMPTY_OPTIONAL_FIELDS.toJSONObject()
    val deserializeClaimsEmptyOptionalFields =
        AuthenticationTokenClaims.createFromJSONObject(jsonObjectEmptyOptionalFields)
    assertThat(
            AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_WITH_EMPTY_OPTIONAL_FIELDS ==
                deserializeClaimsEmptyOptionalFields)
        .isTrue

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
    val newAuthenticationTokenClaims =
        AuthenticationTokenClaims(encodedString, AuthenticationTokenTestUtil.NONCE)
    assertThat(
            AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST == newAuthenticationTokenClaims)
        .isTrue

    // test only required claims fields with others are null
    val encodedStringWithRequiredFields =
        AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly
            .toEnCodedString()
    val newClaimsWithRequiredFields =
        AuthenticationTokenClaims(
            encodedStringWithRequiredFields, AuthenticationTokenTestUtil.NONCE)
    assertThat(
            AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly ==
                newClaimsWithRequiredFields)
        .isTrue
  }

  @Test
  fun `test parceling with all fields`() {
    val claims1 = AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_FOR_TEST
    val claims2 = FacebookTestUtility.parcelAndUnparcel(claims1)
    assertThat(claims2).isNotNull
    assertThat(claims1).isEqualTo(claims2)
  }

  @Test
  fun `test parceling with empty optional fields`() {
    val claims1 = AuthenticationTokenTestUtil.AUTH_TOKEN_CLAIMS_WITH_EMPTY_OPTIONAL_FIELDS
    val claims2 = FacebookTestUtility.parcelAndUnparcel(claims1)
    assertThat(claims2).isNotNull
    assertThat(claims1).isEqualTo(claims2)
  }

  @Test
  fun `test parceling only required fields while other are null`() {
    val claims1 = AuthenticationTokenTestUtil.authenticationTokenClaimsWithRequiredFieldsOnly
    val claims2 = FacebookTestUtility.parcelAndUnparcel(claims1)
    assertThat(claims2).isNotNull
    assertThat(claims1).isEqualTo(claims2)
  }
}
