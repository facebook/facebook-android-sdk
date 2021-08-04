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

  /** Header component of the AuthenticationToken */
  val header: AuthenticationTokenHeader

  /** Payload component of the AuthenticationToken */
  val claims: AuthenticationTokenClaims

  /** Signature component of the id_token TODO: create a new class for the signature part */
  val signature: String

  constructor(token: String) {
    Validate.notEmpty(token, "token")

    // TODO: id_token validation

    val tokenArray = token.split(".")
    require(tokenArray.size == 3) { "Invalid IdToken string" }

    this.token = token
    this.header = AuthenticationTokenHeader(tokenArray[0])
    this.claims = AuthenticationTokenClaims(tokenArray[1])
    this.signature = tokenArray[2]
  }

  internal constructor(parcel: Parcel) {
    val token = parcel.readString()
    Validate.notNullOrEmpty(token, "token")
    this.token = checkNotNull(token)

    val tokenArray = token.split(".")
    this.header = AuthenticationTokenHeader(tokenArray[0])
    this.claims = AuthenticationTokenClaims(tokenArray[1])
    this.signature = tokenArray[2]
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is AuthenticationToken) {
      return false
    }
    return token == other.token
  }

  override fun hashCode(): Int {
    var result = 17
    result = result * 31 + token.hashCode()
    return result
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(token)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object {
    const val AUTHENTICATION_TOKEN_KEY = "authentication_token"

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
