package com.facebook.login

import android.text.format.DateUtils
import com.facebook.AuthenticationToken
import com.facebook.AuthenticationTokenClaims
import java.util.Date

class AuthenticationTokenTestUtil {
  companion object {
    const val JTI = "jti"
    const val APP_ID = "123456789" // aud
    const val ISS = "https://facebook.com/dialog/oauth"
    const val SUB = "1234"
    const val NONCE = "nonce"
    val EXPIRATION_DATE =
        Date(Date().time + DateUtils.HOUR_IN_MILLIS) // valid only within 60 minutes

    @JvmField
    val AUTH_TOKEN_CLAIMS_FOR_TEST =
        AuthenticationTokenClaims(
            JTI,
            ISS,
            APP_ID,
            NONCE,
            EXPIRATION_DATE, // exp
            Date(), // iat
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

    @JvmField
    val authenticationTokenClaimsWithRequiredFieldsOnly =
        AuthenticationTokenClaims(JTI, ISS, APP_ID, NONCE, EXPIRATION_DATE, Date(), SUB)

    @JvmStatic
    fun getAuthenticationTokenForTest(): AuthenticationToken {
      return AuthenticationToken(getEncodedAuthTokenStringForTest(), NONCE)
    }

    @JvmStatic
    fun getEncodedAuthTokenStringForTest(): String {
      val sb = StringBuilder()
      sb.append("eyJhbGciOiJTSEEyNTYiLCJ0eXAiOiJ0b2tlbl90eXBlIiwia2lkIjoiYWJjIn0=").append(".")
      sb.append(AUTH_TOKEN_CLAIMS_FOR_TEST.toEnCodedString()).append(".")
      sb.append("Signature")
      return sb.toString()
    }
  }
}
