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
    this.alg = Validate.notNullOrEmpty(alg, "alg")

    val typ = parcel.readString()
    this.typ = Validate.notNullOrEmpty(typ, "typ")

    val kid = parcel.readString()
    this.kid = Validate.notNullOrEmpty(kid, "kid")
  }

  /**
   * This constructor is only for caching only. NOTE: Using the following constructor is strongly
   * discouraged, it will bypass any validation
   */
  @Throws(JSONException::class)
  internal constructor(jsonObject: JSONObject) {
    this.alg = jsonObject.getString("alg")
    this.typ = jsonObject.getString("typ")
    this.kid = jsonObject.getString("kid")
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

  override fun describeContents(): Int = 0

  override fun toString(): String {
    val headerJsonObject = toJSONObject()
    return headerJsonObject.toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is AuthenticationTokenHeader) {
      return false
    }
    return alg == other.alg && typ == other.typ && kid == other.kid
  }

  override fun hashCode(): Int {
    var result = 17
    result = result * 31 + alg.hashCode()
    result = result * 31 + typ.hashCode()
    result = result * 31 + kid.hashCode()
    return result
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

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<AuthenticationTokenHeader> =
        object : Parcelable.Creator<AuthenticationTokenHeader> {
          override fun createFromParcel(source: Parcel): AuthenticationTokenHeader {
            return AuthenticationTokenHeader(source)
          }

          override fun newArray(size: Int): Array<AuthenticationTokenHeader?> {
            return arrayOfNulls(size)
          }
        }
  }
}
