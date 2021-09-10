/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook

import android.os.Parcel
import android.os.Parcelable
import com.facebook.internal.Validate
import com.facebook.internal.security.OidcSecurityUtil
import java.io.IOException
import java.security.spec.InvalidKeySpecException

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
    Validate.notNullOrEmpty(token, "token")
    this.token = checkNotNull(token)

    val expectedNonce = parcel.readString()
    Validate.notNull(expectedNonce, "expectedNonce")
    this.expectedNonce = checkNotNull(expectedNonce)

    this.header =
        checkNotNull(parcel.readParcelable(AuthenticationTokenHeader::class.java.classLoader))
    this.claims =
        checkNotNull(parcel.readParcelable(AuthenticationTokenClaims::class.java.classLoader))

    val signature = parcel.readString()
    Validate.notNullOrEmpty(signature, "signature")
    this.signature = checkNotNull(signature)
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

  override fun describeContents(): Int {
    return 0
  }

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
