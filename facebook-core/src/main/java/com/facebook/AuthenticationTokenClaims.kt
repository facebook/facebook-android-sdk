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
import android.text.format.DateUtils
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.facebook.internal.Utility.convertJSONObjectToHashMap
import com.facebook.internal.Utility.convertJSONObjectToStringMap
import com.facebook.internal.Utility.jsonArrayToSet
import com.facebook.internal.Utility.jsonArrayToStringList
import com.facebook.internal.Validate
import java.net.MalformedURLException
import java.net.URL
import java.util.Collections
import java.util.Date
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import org.json.JSONArray
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

  /**
   * Expiration time (in seconds) on or after which the ID Token MUST NOT be accepted for
   * processing.
   */
  val exp: Long

  /** Time (in seconds) at which the JWT was issued. */
  val iat: Long

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

  @JvmOverloads
  constructor(encodedClaims: String, expectedNonce: String) {
    Validate.notEmpty(encodedClaims, "encodedClaims")

    val decodedBytes = Base64.decode(encodedClaims, Base64.DEFAULT)
    val claimsString = String(decodedBytes)
    val jsonObj = JSONObject(claimsString)

    // verify claims
    require(isValidClaims(jsonObj, expectedNonce)) { "Invalid claims" }

    this.jti = jsonObj.getString("jti")
    this.iss = jsonObj.getString("iss")
    this.aud = jsonObj.getString("aud")
    this.nonce = jsonObj.getString("nonce")
    this.exp = jsonObj.getLong("exp")
    this.iat = jsonObj.getLong("iat")
    this.sub = jsonObj.getString("sub")
    this.name = jsonObj.getNullableString("name")
    this.givenName = jsonObj.getNullableString("givenName")
    this.middleName = jsonObj.getNullableString("middleName")
    this.familyName = jsonObj.getNullableString("familyName")
    this.email = jsonObj.getNullableString("email")
    this.picture = jsonObj.getNullableString("picture")

    val userFriendsList = jsonObj.optJSONArray("userFriends")
    this.userFriends =
        if (userFriendsList == null) null
        else Collections.unmodifiableSet(jsonArrayToSet(userFriendsList))
    this.userBirthday = jsonObj.getNullableString("userBirthday")
    val userAgeRangeJson = jsonObj.optJSONObject("userAgeRange")
    this.userAgeRange =
        if (userAgeRangeJson == null) null
        else {
          Collections.unmodifiableMap(
              convertJSONObjectToHashMap(userAgeRangeJson) as Map<String, Int>?)
        }
    val userHometownJson = jsonObj.optJSONObject("userHometown")
    this.userHometown =
        if (userHometownJson == null) null
        else Collections.unmodifiableMap(convertJSONObjectToStringMap(userHometownJson))
    val userLocationJson = jsonObj.optJSONObject("userLocation")
    this.userLocation =
        if (userLocationJson == null) null
        else Collections.unmodifiableMap(convertJSONObjectToStringMap(userLocationJson))
    this.userGender = jsonObj.getNullableString("userGender")
    this.userLink = jsonObj.getNullableString("userLink")
  }

  /**
   * Creates a the claims component of
   *
   * @param jti A unique identifier for the token
   * @param iss Issuer Identifier for the Issuer of the response
   * @param aud Audience(s) that this ID Token is intended for
   * @param nonce String value used to associate a Client session with an ID Token
   * @param exp Expiration time on or after which the ID Token MUST NOT be accepted for processing
   * @param iat Time at which the JWT was issued
   * @param sub Subject - Identifier for the End-User at the Issuer
   * @param name End-User's full name in displayable form including all name parts
   * @param givenName End-User's given name in displayable form
   * @param middleName End-User's middle name in displayable form
   * @param familyName End-User's family name in displayable form
   * @param email End-User's preferred e-mail address
   * @param picture URL of the End-User's profile picture
   * @param userFriends End-User's friends
   * @param userBirthday End-User's birthday
   * @param userAgeRange End-User's age range
   * @param userHometown End-User's hometown
   * @param userLocation End-User's location
   * @param userGender End-User's gender
   * @param userLink End-User's link
   */
  @JvmOverloads
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  constructor(
      jti: String,
      iss: String,
      aud: String,
      nonce: String,
      exp: Long,
      iat: Long,
      sub: String,
      name: String? = null,
      givenName: String? = null,
      middleName: String? = null,
      familyName: String? = null,
      email: String? = null,
      picture: String? = null,
      userFriends: Collection<String>? = null,
      userBirthday: String? = null,
      userAgeRange: Map<String, Int>? = null,
      userHometown: Map<String, String>? = null,
      userLocation: Map<String, String>? = null,
      userGender: String? = null,
      userLink: String? = null
  ) {
    Validate.notEmpty(jti, "jti")
    Validate.notEmpty(iss, "iss")
    Validate.notEmpty(aud, "aud")
    Validate.notEmpty(nonce, "nonce")
    Validate.notEmpty(sub, "sub")

    /**
     * TODO: We do not want developer to consume this constructor directly, need to move this class
     * to internal folder
     */
    this.jti = jti
    this.iss = iss
    this.aud = aud
    this.nonce = nonce
    this.exp = exp
    this.iat = iat
    this.sub = sub
    this.name = name
    this.givenName = givenName
    this.middleName = middleName
    this.familyName = familyName
    this.email = email
    this.picture = picture
    this.userFriends =
        if (userFriends != null) Collections.unmodifiableSet(HashSet(userFriends)) else null
    this.userBirthday = userBirthday
    this.userAgeRange =
        if (userAgeRange != null) Collections.unmodifiableMap(HashMap(userAgeRange)) else null
    this.userHometown =
        if (userHometown != null) Collections.unmodifiableMap(HashMap(userHometown)) else null
    this.userLocation =
        if (userLocation != null) Collections.unmodifiableMap(HashMap(userLocation)) else null
    this.userGender = userGender
    this.userLink = userLink
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

    this.exp = parcel.readLong()
    this.iat = parcel.readLong()
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
    dest.writeLong(exp)
    dest.writeLong(iat)
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

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is AuthenticationTokenClaims) {
      return false
    }
    return jti == other.jti &&
        iss == other.iss &&
        aud == other.aud &&
        nonce == other.nonce &&
        exp == other.exp &&
        iat == other.iat &&
        sub == other.sub &&
        name == other.name &&
        givenName == other.givenName &&
        middleName == other.middleName &&
        familyName == other.familyName &&
        email == other.email &&
        picture == other.picture &&
        userFriends == other.userFriends &&
        userBirthday == other.userBirthday &&
        userAgeRange == other.userAgeRange &&
        userHometown == other.userHometown &&
        userLocation == other.userLocation &&
        userGender == other.userGender &&
        userLink == other.userLink
  }

  override fun hashCode(): Int {
    var result = 17
    result = result * 31 + jti.hashCode()
    result = result * 31 + iss.hashCode()
    result = result * 31 + aud.hashCode()
    result = result * 31 + nonce.hashCode()
    result = result * 31 + exp.hashCode()
    result = result * 31 + iat.hashCode()
    result = result * 31 + sub.hashCode()
    result = result * 31 + (name?.hashCode() ?: 0)
    result = result * 31 + (givenName?.hashCode() ?: 0)
    result = result * 31 + (middleName?.hashCode() ?: 0)
    result = result * 31 + (familyName?.hashCode() ?: 0)
    result = result * 31 + (email?.hashCode() ?: 0)
    result = result * 31 + (picture?.hashCode() ?: 0)
    result = result * 31 + (userFriends?.hashCode() ?: 0)
    result = result * 31 + (userBirthday?.hashCode() ?: 0)
    result = result * 31 + (userAgeRange?.hashCode() ?: 0)
    result = result * 31 + (userHometown?.hashCode() ?: 0)
    result = result * 31 + (userLocation?.hashCode() ?: 0)
    result = result * 31 + (userGender?.hashCode() ?: 0)
    result = result * 31 + (userLink?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    val claimsJsonObject = toJSONObject()
    return claimsJsonObject.toString()
  }

  override fun describeContents(): Int {
    return 0
  }

  private fun isValidClaims(claimsJson: JSONObject, expectedNonce: String): Boolean {
    if (claimsJson == null) {
      return false
    }

    val jti = claimsJson.optString("jti")
    if (jti.isEmpty()) {
      return false
    }

    try {
      val iss = claimsJson.optString("iss")
      if (iss.isEmpty() || URL(iss).host != "facebook.com") {
        return false
      }
    } catch (ex: MalformedURLException) {
      // since its MalformedURLException, iss is invalid
      return false
    }

    val aud = claimsJson.optString("aud")
    if (aud.isEmpty() || aud != FacebookSdk.getApplicationId()) { // aud matched
      return false
    }

    val expDate = Date(claimsJson.optLong("exp") * 1000)
    if (Date().after(expDate)) { // is expired
      return false
    }

    val iatInSeconds = claimsJson.optLong("iat")
    val iatExpireDate = Date(iatInSeconds * 1000 + MAX_TIME_SINCE_TOKEN_ISSUED)
    if (Date().after(iatExpireDate)) { // issued too far in the past
      return false
    }

    val sub = claimsJson.optString("sub")
    if (sub.isEmpty()) { // user ID is not valid
      return false
    }

    val nonce = claimsJson.optString("nonce")
    if (nonce.isEmpty() || nonce != expectedNonce) { // incorrect nonce
      return false
    }

    return true
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun toEnCodedString(): String {
    val claimsJsonString = toString()
    return Base64.encodeToString(claimsJsonString.toByteArray(), Base64.DEFAULT)
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("jti", this.jti)
    jsonObject.put("iss", this.iss)
    jsonObject.put("aud", this.aud)
    jsonObject.put("nonce", this.nonce)
    jsonObject.put("exp", this.exp)
    jsonObject.put("iat", this.iat)
    if (this.sub != null) {
      jsonObject.put("sub", this.sub)
    }
    if (this.name != null) {
      jsonObject.put("name", this.name)
    }
    if (this.givenName != null) {
      jsonObject.put("givenName", this.givenName)
    }
    if (this.middleName != null) {
      jsonObject.put("middleName", this.middleName)
    }
    if (this.familyName != null) {
      jsonObject.put("familyName", this.familyName)
    }
    if (this.email != null) {
      jsonObject.put("email", this.email)
    }
    if (this.picture != null) {
      jsonObject.put("picture", this.picture)
    }
    if (this.userFriends != null && this.userFriends.isNotEmpty()) {
      jsonObject.put("userFriends", JSONArray(this.userFriends))
    }
    if (this.userBirthday != null) {
      jsonObject.put("userBirthday", this.userBirthday)
    }
    if (this.userAgeRange != null && this.userAgeRange.isNotEmpty()) {
      jsonObject.put("userAgeRange", JSONObject(this.userAgeRange))
    }
    if (this.userHometown != null && this.userHometown.isNotEmpty()) {
      jsonObject.put("userHometown", JSONObject(this.userHometown))
    }
    if (this.userLocation != null && this.userLocation.isNotEmpty()) {
      jsonObject.put("userLocation", JSONObject(this.userLocation))
    }
    if (this.userGender != null) {
      jsonObject.put("userGender", this.userGender)
    }
    if (this.userLink != null) {
      jsonObject.put("userLink", this.userLink)
    }

    return jsonObject
  }

  private fun JSONObject.getNullableString(name: String): String? {
    if (has(name)) {
      return getString(name)
    }
    return null
  }

  companion object {
    const val MAX_TIME_SINCE_TOKEN_ISSUED = DateUtils.MINUTE_IN_MILLIS * 10 // 10 minutes

    internal fun createFromJSONObject(jsonObject: JSONObject): AuthenticationTokenClaims {
      val jti = jsonObject.getString("jti")
      val iss = jsonObject.getString("iss")
      val aud = jsonObject.getString("aud")
      val nonce = jsonObject.getString("nonce")
      val exp = jsonObject.getLong("exp")
      val iat = jsonObject.getLong("iat")
      val sub = jsonObject.getString("sub")
      val name = jsonObject.optString("name")
      val givenName = jsonObject.optString("givenName")
      val middleName = jsonObject.optString("middleName")
      val familyName = jsonObject.optString("familyName")
      val email = jsonObject.optString("email")
      val picture = jsonObject.optString("picture")
      val userFriends = jsonObject.optJSONArray("userFriends")
      val userBirthday = jsonObject.optString("userBirthday")
      val userAgeRange = jsonObject.optJSONObject("userAgeRange")
      val userHometown = jsonObject.optJSONObject("userHometown")
      val userLocation = jsonObject.optJSONObject("userLocation")
      val userGender = jsonObject.optString("userGender")
      val userLink = jsonObject.optString("userLink")
      return AuthenticationTokenClaims(
          jti,
          iss,
          aud,
          nonce,
          exp,
          iat,
          sub,
          if (name.isNullOrEmpty()) null else name,
          if (givenName.isNullOrEmpty()) null else givenName,
          if (middleName.isNullOrEmpty()) null else middleName,
          if (familyName.isNullOrEmpty()) null else familyName,
          if (email.isNullOrEmpty()) null else email,
          if (picture.isNullOrEmpty()) null else picture,
          if (userFriends == null) null else jsonArrayToStringList(userFriends),
          if (userBirthday.isNullOrEmpty()) null else userBirthday,
          if (userAgeRange == null) null
          else convertJSONObjectToHashMap(userAgeRange) as Map<String, Int>?,
          if (userHometown == null) null else convertJSONObjectToStringMap(userHometown),
          if (userLocation == null) null else convertJSONObjectToStringMap(userLocation),
          if (userGender.isNullOrEmpty()) null else userGender,
          if (userLink.isNullOrEmpty()) null else userLink)
    }

    @JvmField
    val CREATOR: Parcelable.Creator<AuthenticationTokenClaims> =
        object : Parcelable.Creator<AuthenticationTokenClaims> {
          override fun createFromParcel(source: Parcel): AuthenticationTokenClaims {
            return AuthenticationTokenClaims(source)
          }

          override fun newArray(size: Int): Array<AuthenticationTokenClaims?> {
            return arrayOfNulls(size)
          }
        }
  }
}
