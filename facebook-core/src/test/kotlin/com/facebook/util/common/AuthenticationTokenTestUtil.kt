package com.facebook.util.common

import android.text.format.DateUtils
import com.facebook.AuthenticationToken
import com.facebook.AuthenticationTokenClaims
import com.facebook.AuthenticationTokenHeader

object AuthenticationTokenTestUtil {

  const val JTI = "jti"
  const val APP_ID = "123456789" // aud
  const val ISS = "https://facebook.com/dialog/oauth"
  const val SUB = "1234"
  const val NONCE = "nonce"
  val IAT = System.currentTimeMillis() / 1000
  val EXPIRATION_DATE =
      (System.currentTimeMillis() + DateUtils.HOUR_IN_MILLIS) / 1000 // valid only within 60 minutes

  @JvmField
  val AUTH_TOKEN_CLAIMS_FOR_TEST =
      AuthenticationTokenClaims(
          JTI,
          ISS,
          APP_ID,
          NONCE,
          EXPIRATION_DATE, // exp
          IAT, // iat
          SUB,
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

  @JvmField val AUTH_TOKEN_HEADER_FOR_TEST = AuthenticationTokenHeader("RS256", "token_type", "abc")

  val VALID_HEADER_STRING = AUTH_TOKEN_HEADER_FOR_TEST.toEnCodedString()

  @JvmField
  val authenticationTokenClaimsWithRequiredFieldsOnly =
      AuthenticationTokenClaims(JTI, ISS, APP_ID, NONCE, EXPIRATION_DATE, IAT, SUB)

  @JvmStatic
  fun getAuthenticationTokenForTest(): AuthenticationToken {
    return AuthenticationToken(getEncodedAuthTokenStringForTest(), NONCE)
  }

  @JvmStatic
  fun getEncodedAuthTokenStringForTest(): String {
    val sb = StringBuilder()
    sb.append(VALID_HEADER_STRING).append(".")
    sb.append(AUTH_TOKEN_CLAIMS_FOR_TEST.toEnCodedString()).append(".")
    sb.append("Signature")
    return sb.toString()
  }
}
