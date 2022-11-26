/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.Parcel
import android.os.Parcelable
import com.facebook.internal.Validate
import com.facebook.internal.security.OidcSecurityUtil
import java.io.IOException
import java.security.spec.InvalidKeySpecException
import org.json.JSONException
import org.json.JSONObject

/**
 * This class represents an immutable Authentication Token (or so called "id token") for using
 * Facebook Login. It includes the three component that constructs the token
 * HEADER.PAYLOAD.SIGNATURE
 *
 * WARNING: This feature is currently in development and not intended for external usage.
 */
class AuthenticationToken : Parcelable {

  /**
   * Gets the raw string of the AuthenticationToken (or id_token)
   *
   * @return the raw string representing the Authentication Token in the following format
   * Header.Payload.Signature
   */
  val token: String

  /** Expected nonce - used in Login Configuration */
  val expectedNonce: String

  /** Header component of the AuthenticationToken */
  val header: AuthenticationTokenHeader

  /** Payload component of the AuthenticationToken */
  val claims: AuthenticationTokenClaims

  /** Signature component of the id_token TODO: create a new class for the signature part */
  val signature: String

  @JvmOverloads
  constructor(token: String, expectedNonce: String) {
    Validate.notEmpty(token, "token")
    Validate.notEmpty(expectedNonce, "expectedNonce")

    val tokenArray = token.split(".")
    require(tokenArray.size == 3) { "Invalid IdToken string" }
    val rawHeader = tokenArray[0]
    val rawClaims = tokenArray[1]
    val rawSignature = tokenArray[2]

    this.token = token
    this.expectedNonce = expectedNonce
    this.header = AuthenticationTokenHeader(rawHeader)
    this.claims = AuthenticationTokenClaims(rawClaims, expectedNonce)

    // validate signature
    require(isValidSignature(rawHeader, rawClaims, rawSignature, header.kid)) {
      "Invalid Signature"
    }
    this.signature = rawSignature
  }

  internal constructor(parcel: Parcel) {
    val token = parcel.readString()
    this.token = Validate.notNullOrEmpty(token, "token")

    val expectedNonce = parcel.readString()
    this.expectedNonce = Validate.notNullOrEmpty(expectedNonce, "expectedNonce")

    this.header =
        checkNotNull(parcel.readParcelable(AuthenticationTokenHeader::class.java.classLoader))
    this.claims =
        checkNotNull(parcel.readParcelable(AuthenticationTokenClaims::class.java.classLoader))

    val signature = parcel.readString()
    this.signature = Validate.notNullOrEmpty(signature, "signature")
  }

  /**
   * internal constructor for caching only NOTE: This constructor will skip the benefit of token
   * validation
   */
  @Throws(JSONException::class)
  internal constructor(jsonObject: JSONObject) {
    this.token = jsonObject.getString(TOKEN_STRING_KEY)
    this.expectedNonce = jsonObject.getString(EXPECTED_NONCE_KEY)
    this.signature = jsonObject.getString(SIGNATURE_KEY)

    val headerJSONObject = jsonObject.getJSONObject((HEADER_KEY))
    val claimsJSONObject = jsonObject.getJSONObject(CLAIMS_KEY)
    this.header = AuthenticationTokenHeader(headerJSONObject)
    this.claims = AuthenticationTokenClaims.createFromJSONObject(claimsJSONObject)
  }

  /** convert current AuthenticationToken object to JSON */
  @Throws(JSONException::class)
  internal fun toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put(TOKEN_STRING_KEY, token)
    jsonObject.put(EXPECTED_NONCE_KEY, expectedNonce)
    jsonObject.put(HEADER_KEY, header.toJSONObject())
    jsonObject.put(CLAIMS_KEY, claims.toJSONObject())
    jsonObject.put(SIGNATURE_KEY, signature)
    return jsonObject
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is AuthenticationToken) {
      return false
    }
    return token == other.token &&
        expectedNonce == other.expectedNonce &&
        header == other.header &&
        claims == other.claims &&
        signature == other.signature
  }

  override fun hashCode(): Int {
    var result = 17
    result = result * 31 + token.hashCode()
    result = result * 31 + expectedNonce.hashCode()
    result = result * 31 + header.hashCode()
    result = result * 31 + claims.hashCode()
    result = result * 31 + signature.hashCode()
    return result
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(token)
    dest.writeString(expectedNonce)
    dest.writeParcelable(header, flags)
    dest.writeParcelable(claims, flags)
    dest.writeString(signature)
  }

  override fun describeContents(): Int = 0

  private fun isValidSignature(
      headerString: String,
      claimsString: String,
      sigString: String,
      kid: String
  ): Boolean {
    try {
      val pubKeyString: String = OidcSecurityUtil.getRawKeyFromEndPoint(kid) ?: return false
      val pubKey = OidcSecurityUtil.getPublicKeyFromString(pubKeyString)
      return OidcSecurityUtil.verify(pubKey, "$headerString.$claimsString", sigString)
    } catch (ex: InvalidKeySpecException) {
      // invalid key
      return false
    } catch (_ex: IOException) {
      // exception happens on
      return false
    }
  }

  companion object {
    const val AUTHENTICATION_TOKEN_KEY = "id_token"

    // JSON serialization Key
    private const val TOKEN_STRING_KEY = "token_string"
    private const val EXPECTED_NONCE_KEY = "expected_nonce"
    private const val HEADER_KEY = "header"
    private const val CLAIMS_KEY = "claims"
    private const val SIGNATURE_KEY = "signature"

    /**
     * Getter for the authentication token that is current for the application.
     *
     * @return The authentication token that is current for the application.
     */
    @JvmStatic
    fun getCurrentAuthenticationToken(): AuthenticationToken? {
      return AuthenticationTokenManager.getInstance().currentAuthenticationToken
    }

    /**
     * Setter for the authentication token that is current for the application.
     *
     * @param authenticationToken The authentication token to set.
     */
    @JvmStatic
    fun setCurrentAuthenticationToken(authenticationToken: AuthenticationToken?) {
      AuthenticationTokenManager.getInstance().currentAuthenticationToken = authenticationToken
    }

    @JvmField
    val CREATOR: Parcelable.Creator<AuthenticationToken> =
        object : Parcelable.Creator<AuthenticationToken> {
          override fun createFromParcel(source: Parcel): AuthenticationToken {
            return AuthenticationToken(source)
          }

          override fun newArray(size: Int): Array<AuthenticationToken?> {
            return arrayOfNulls(size)
          }
        }
  }
}
