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
import androidx.annotation.VisibleForTesting
import com.facebook.internal.Validate
import org.json.JSONException
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

  constructor(encodedHeaderString: String) {
    // verify header
    require(isValidHeader(encodedHeaderString)) { "Invalid Header" }

    val decodedBytes = Base64.decode(encodedHeaderString, Base64.DEFAULT)
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

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  constructor(alg: String, typ: String, kid: String) {
    this.alg = alg
    this.typ = typ
    this.kid = kid
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(alg)
    dest.writeString(typ)
    dest.writeString(kid)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun toString(): String {
    val headerJsonObject = toJSONObject()
    return headerJsonObject.toString()
  }

  /**
   * check if the input header string is a valid id token header
   *
   * @param headerString the header string
   */
  private fun isValidHeader(headerString: String): Boolean {
    Validate.notEmpty(headerString, "encodedHeaderString")

    val decodedBytes = Base64.decode(headerString, Base64.DEFAULT)
    val claimsString = String(decodedBytes)

    return try {
      val jsonObj = JSONObject(claimsString)

      val alg = jsonObj.optString("alg")
      val validAlg = alg.isNotEmpty() && alg == "RS256"

      val hasKid = jsonObj.optString("kid").isNotEmpty()
      val validTyp = jsonObj.optString("typ").isNotEmpty()

      validAlg && hasKid && validTyp
    } catch (_ex: JSONException) {
      // return false if there any problem parsing the JSON string
      false
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("alg", this.alg)
    jsonObject.put("typ", this.typ)
    jsonObject.put("kid", this.kid)

    return jsonObject
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun toEnCodedString(): String {
    val claimsJsonString = toString()
    return Base64.encodeToString(claimsJsonString.toByteArray(), Base64.DEFAULT)
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
