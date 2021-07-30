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
import java.util.Collections
import java.util.Date
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import org.json.JSONObject

/**
 * This class represents an immutable claims for using AuthenticationToken. It includes all metadata
 * or key values of the claims
 *
 * WARNING: This feature is currently in development and not intended for external usage.
 */
class AuthenticationTokenClaims : Parcelable {
  /** Get a unique identifier for the token. */
  val jti: String

  /** Get issuer Identifier for the Issuer of the response. */
  val iss: String

  /** Get audience(s) that this ID Token is intended for. */
  val aud: String

  /**
   * String value used to associate a Client session with an ID Token, and to mitigate replay
   * attacks.
   */
  val nonce: String

  /** Expiration time on or after which the ID Token MUST NOT be accepted for processing. */
  val exp: Date

  /** Time at which the JWT was issued. */
  val iat: Date

  /** Subject - Identifier for the End-User at the Issuer. */
  val sub: String

  /** End-User's full name in displayable form including all name parts. */
  val name: String?

  /** End-User's given name in displayable form */
  val givenName: String?

  /** End-User's middle name in displayable form */
  val middleName: String?

  /** End-User's family name in displayable form */
  val familyName: String?

  /**
   * End-User's preferred e-mail address.
   *
   * IMPORTANT: This field will only be populated if your user has granted your application the
   * 'email' permission.
   */
  val email: String?

  /** URL of the End-User's profile picture. */
  val picture: String?

  /**
   * End-User's friends.
   *
   * IMPORTANT: This field will only be populated if your user has granted your application the
   * 'user_friends' permission.
   */
  val userFriends: Set<String>?

  /** End-User's birthday */
  val userBirthday: String?

  /** End-User's age range */
  val userAgeRange: Map<String, Int>?

  /** End-User's hometown */
  val userHometown: Map<String, String>?

  /** End-User's location */
  val userLocation: Map<String, String>?

  /** End-User's gender */
  val userGender: String?

  /** End-User's link */
  val userLink: String?

  constructor(encodedClaims: String) {
    Validate.notEmpty(encodedClaims, "token")

    val decodedBytes = Base64.decode(encodedClaims, Base64.DEFAULT)
    val claimsString = String(decodedBytes)
    val jsonObj = JSONObject(claimsString)

    // TODO: Additional validation needed
    // Reference: https://developers.facebook.com/docs/facebook-login/limited-login/token/validating

    this.jti = jsonObj.getString("jti")
    this.iss = jsonObj.getString("iss")
    this.aud = jsonObj.getString("aud")
    this.nonce = jsonObj.getString("nonce")
    this.exp = Date(jsonObj.getLong("exp"))
    this.iat = Date(jsonObj.getLong("iat"))
    this.sub = jsonObj.getString("sub")
    this.name = jsonObj.optString("name")
    this.givenName = jsonObj.optString("givenName")
    this.middleName = jsonObj.optString("middleName")
    this.familyName = jsonObj.optString("familyName")
    this.email = jsonObj.optString("email")
    this.picture = jsonObj.optString("picture")

    // TODO: Validate and test the format of userFriends
    this.userFriends = jsonObj.optJSONObject("userFriends") as Set<String>?
    this.userBirthday = jsonObj.optString("userBirthday")
    this.userAgeRange = jsonObj.optJSONObject("userAgeRange") as Map<String, Int>?
    this.userHometown = jsonObj.optJSONObject("userHometown") as Map<String, String>?
    this.userLocation = jsonObj.optJSONObject("userLocation") as Map<String, String>?
    this.userGender = jsonObj.optString("userGender")
    this.userLink = jsonObj.optString("userLink")
  }

  internal constructor(parcel: Parcel) {
    val jti = parcel.readString()
    Validate.notNullOrEmpty(jti, "jti")
    this.jti = checkNotNull(jti)

    val iss = parcel.readString()
    Validate.notNullOrEmpty(iss, "iss")
    this.iss = checkNotNull(iss)

    val aud = parcel.readString()
    Validate.notNullOrEmpty(aud, "aud")
    this.aud = checkNotNull(aud)

    val nonce = parcel.readString()
    Validate.notNullOrEmpty(nonce, "nonce")
    this.nonce = checkNotNull(nonce)

    this.exp = Date(parcel.readLong())
    this.iat = Date(parcel.readLong())
    val sub = parcel.readString()
    Validate.notNullOrEmpty(sub, "sub")
    this.sub = checkNotNull(sub)
    this.name = parcel.readString()
    this.givenName = parcel.readString()
    this.middleName = parcel.readString()
    this.familyName = parcel.readString()
    this.email = parcel.readString()
    this.picture = parcel.readString()

    val userFriendsList = ArrayList<String>()
    parcel.readStringList(userFriendsList)
    this.userFriends = Collections.unmodifiableSet(HashSet(userFriendsList))
    this.userBirthday = parcel.readString()

    val userAgeRangeMap = parcel.readHashMap(Int.javaClass.classLoader) as? (HashMap<String, Int>)
    this.userAgeRange = Collections.unmodifiableMap(userAgeRangeMap)

    val userHometownMap =
        parcel.readHashMap(String.javaClass.classLoader) as? (HashMap<String, String>)
    this.userHometown = Collections.unmodifiableMap(userHometownMap)

    val userLocationMap =
        parcel.readHashMap(String.javaClass.classLoader) as? (HashMap<String, String>)
    this.userLocation = Collections.unmodifiableMap(userLocationMap)

    this.userGender = parcel.readString()
    this.userLink = parcel.readString()
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(jti)
    dest.writeString(iss)
    dest.writeString(aud)
    dest.writeString(nonce)
    dest.writeLong(exp.time)
    dest.writeLong(iat.time)
    dest.writeString(sub)
    dest.writeString(name)
    dest.writeString(givenName)
    dest.writeString(middleName)
    dest.writeString(familyName)
    dest.writeString(email)
    dest.writeString(picture)
    if (userFriends == null) {
      dest.writeStringList(null)
    } else {
      dest.writeStringList(ArrayList(userFriends))
    }
    dest.writeString(userBirthday)
    dest.writeMap(userAgeRange)
    dest.writeMap(userHometown)
    dest.writeMap(userLocation)
    dest.writeString(userGender)
    dest.writeString(userLink)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<AuthenticationTokenClaims> {
    override fun createFromParcel(parcel: Parcel): AuthenticationTokenClaims {
      return AuthenticationTokenClaims(parcel)
    }

    override fun newArray(size: Int): Array<AuthenticationTokenClaims?> {
      return arrayOfNulls(size)
    }
  }
}
