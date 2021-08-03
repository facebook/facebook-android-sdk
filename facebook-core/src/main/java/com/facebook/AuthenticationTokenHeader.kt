// Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
//
// You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
// copy, modify, and distribute this software in source code or binary form for use
// in connection with the web services and APIs provided by Facebook.
//
// As with any software that integrates with the Facebook platform, your use of
// this software is subject to the Facebook Developer Principles and Policies
// [http://developers.facebook.com/policy/]. This copyright notice shall be
// included in all copies or substantial portions of the software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.facebook

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.facebook.internal.Validate
import org.json.JSONObject

/**
 * This class represents an immutable header for using AuthenticationToken. It includes all metadata
 * or key values of the header
 *
 * WARNING: This feature is currently in development and not intended for external usage.
 */
class AuthenticationTokenHeader : Parcelable {

  /** Value that represents the algorithm that was used to sign the JWT. */
  val alg: String

  /** The type of the JWT. */
  val typ: String

  /** Key identifier used in identifying the key to be used to verify the signature. */
  val kid: String

  constructor(headerString: String) {
    Validate.notEmpty(headerString, "headerString")

    val decodedBytes = Base64.decode(headerString, Base64.DEFAULT)
    val claimsString = String(decodedBytes)
    val jsonObj = JSONObject(claimsString)

    this.alg = jsonObj.getString("alg")
    this.typ = jsonObj.getString("typ")
    this.kid = jsonObj.getString("kid")
  }

  internal constructor(parcel: Parcel) {
    val alg = parcel.readString()
    Validate.notNullOrEmpty(alg, "alg")
    this.alg = checkNotNull(alg)

    val typ = parcel.readString()
    Validate.notNullOrEmpty(typ, "typ")
    this.typ = checkNotNull(typ)

    val kid = parcel.readString()
    Validate.notNullOrEmpty(kid, "kid")
    this.kid = checkNotNull(kid)
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(alg)
    dest.writeString(typ)
    dest.writeString(kid)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<AuthenticationTokenHeader> {
    override fun createFromParcel(parcel: Parcel): AuthenticationTokenHeader {
      return AuthenticationTokenHeader(parcel)
    }

    override fun newArray(size: Int): Array<AuthenticationTokenHeader?> {
      return arrayOfNulls(size)
    }
  }
}
