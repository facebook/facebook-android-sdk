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

    val decodedBytes = Base64.decode(encodedClaims, Base64.URL_SAFE)
    val claimsString = String(decodedBytes)
    val jsonObj = JSONObject(claimsString)

    // verify claims
    require(isValidClaims(jsonObj, expectedNonce)) { "Invalid claims" }

    this.jti = jsonObj.getString(JSON_KEY_JIT)
    this.iss = jsonObj.getString(JSON_KEY_ISS)
    this.aud = jsonObj.getString(JSON_KEY_AUD)
    this.nonce = jsonObj.getString(JSON_KEY_NONCE)
    this.exp = jsonObj.getLong(JSON_KEY_EXP)
    this.iat = jsonObj.getLong(JSON_KEY_IAT)
    this.sub = jsonObj.getString(JSON_KEY_SUB)
    this.name = jsonObj.getNullableString(JSON_KEY_NAME)
    this.givenName = jsonObj.getNullableString(JSON_KEY_GIVEN_NAME)
    this.middleName = jsonObj.getNullableString(JSON_KEY_MIDDLE_NAME)
    this.familyName = jsonObj.getNullableString(JSON_KEY_FAMILY_NAME)
    this.email = jsonObj.getNullableString(JSON_KEY_EMAIL)
    this.picture = jsonObj.getNullableString(JSON_KEY_PICTURE)

    val userFriendsList = jsonObj.optJSONArray(JSON_KEY_USER_FRIENDS)
    this.userFriends =
        if (userFriendsList == null) null
        else Collections.unmodifiableSet(jsonArrayToSet(userFriendsList))
    this.userBirthday = jsonObj.getNullableString(JSON_KEY_USER_BIRTHDAY)
    val userAgeRangeJson = jsonObj.optJSONObject(JSON_KEY_USER_AGE_RANGE)
    this.userAgeRange =
        if (userAgeRangeJson == null) null
        else {
          Collections.unmodifiableMap(
              convertJSONObjectToHashMap(userAgeRangeJson) as Map<String, Int>?)
        }
    val userHometownJson = jsonObj.optJSONObject(JSON_KEY_USER_HOMETOWN)
    this.userHometown =
        if (userHometownJson == null) null
        else Collections.unmodifiableMap(convertJSONObjectToStringMap(userHometownJson))
    val userLocationJson = jsonObj.optJSONObject(JSON_KEY_USER_LOCATION)
    this.userLocation =
        if (userLocationJson == null) null
        else Collections.unmodifiableMap(convertJSONObjectToStringMap(userLocationJson))
    this.userGender = jsonObj.getNullableString(JSON_KEY_USER_GENDER)
    this.userLink = jsonObj.getNullableString(JSON_KEY_USER_LINK)
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
    Validate.notEmpty(jti, JSON_KEY_JIT)
    Validate.notEmpty(iss, JSON_KEY_ISS)
    Validate.notEmpty(aud, JSON_KEY_AUD)
    Validate.notEmpty(nonce, JSON_KEY_NONCE)
    Validate.notEmpty(sub, JSON_KEY_SUB)

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
    Validate.notNullOrEmpty(jti, JSON_KEY_JIT)
    this.jti = checkNotNull(jti)

    val iss = parcel.readString()
    Validate.notNullOrEmpty(iss, JSON_KEY_ISS)
    this.iss = checkNotNull(iss)

    val aud = parcel.readString()
    Validate.notNullOrEmpty(aud, JSON_KEY_AUD)
    this.aud = checkNotNull(aud)

    val nonce = parcel.readString()
    Validate.notNullOrEmpty(nonce, JSON_KEY_NONCE)
    this.nonce = checkNotNull(nonce)

    this.exp = parcel.readLong()
    this.iat = parcel.readLong()
    val sub = parcel.readString()
    Validate.notNullOrEmpty(sub, JSON_KEY_SUB)
    this.sub = checkNotNull(sub)
    this.name = parcel.readString()
    this.givenName = parcel.readString()
    this.middleName = parcel.readString()
    this.familyName = parcel.readString()
    this.email = parcel.readString()
    this.picture = parcel.readString()

    val userFriendsList = parcel.createStringArrayList()
    this.userFriends =
        if (userFriendsList != null) Collections.unmodifiableSet(HashSet(userFriendsList)) else null

    this.userBirthday = parcel.readString()

    val userAgeRangeMap = parcel.readHashMap(Int.javaClass.classLoader) as? (HashMap<String, Int>)
    this.userAgeRange =
        if (userAgeRangeMap != null) Collections.unmodifiableMap(userAgeRangeMap) else null

    val userHometownMap =
        parcel.readHashMap(String.javaClass.classLoader) as? (HashMap<String, String>)
    this.userHometown =
        if (userHometownMap != null) Collections.unmodifiableMap(userHometownMap) else null

    val userLocationMap =
        parcel.readHashMap(String.javaClass.classLoader) as? (HashMap<String, String>)
    this.userLocation =
        if (userLocationMap != null) Collections.unmodifiableMap(userLocationMap) else null

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

    val jti = claimsJson.optString(JSON_KEY_JIT)
    if (jti.isEmpty()) {
      return false
    }

    try {
      val iss = claimsJson.optString(JSON_KEY_ISS)
      if (iss.isEmpty() ||
          ((URL(iss).host != "facebook.com") && (URL(iss).host != "www.facebook.com"))) {
        return false
      }
    } catch (ex: MalformedURLException) {
      // since its MalformedURLException, iss is invalid
      return false
    }

    val aud = claimsJson.optString(JSON_KEY_AUD)
    if (aud.isEmpty() || aud != FacebookSdk.getApplicationId()) { // aud matched
      return false
    }

    val expDate = Date(claimsJson.optLong(JSON_KEY_EXP) * 1000)
    if (Date().after(expDate)) { // is expired
      return false
    }

    val iatInSeconds = claimsJson.optLong(JSON_KEY_IAT)
    val iatExpireDate = Date(iatInSeconds * 1000 + MAX_TIME_SINCE_TOKEN_ISSUED)
    if (Date().after(iatExpireDate)) { // issued too far in the past
      return false
    }

    val sub = claimsJson.optString(JSON_KEY_SUB)
    if (sub.isEmpty()) { // user ID is not valid
      return false
    }

    val nonce = claimsJson.optString(JSON_KEY_NONCE)
    if (nonce.isEmpty() || nonce != expectedNonce) { // incorrect nonce
      return false
    }

    return true
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun toEnCodedString(): String {
    val claimsJsonString = toString()
    return Base64.encodeToString(claimsJsonString.toByteArray(), Base64.URL_SAFE)
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put(JSON_KEY_JIT, this.jti)
    jsonObject.put(JSON_KEY_ISS, this.iss)
    jsonObject.put(JSON_KEY_AUD, this.aud)
    jsonObject.put(JSON_KEY_NONCE, this.nonce)
    jsonObject.put(JSON_KEY_EXP, this.exp)
    jsonObject.put(JSON_KEY_IAT, this.iat)
    if (this.sub != null) {
      jsonObject.put(JSON_KEY_SUB, this.sub)
    }
    if (this.name != null) {
      jsonObject.put(JSON_KEY_NAME, this.name)
    }
    if (this.givenName != null) {
      jsonObject.put(JSON_KEY_GIVEN_NAME, this.givenName)
    }
    if (this.middleName != null) {
      jsonObject.put(JSON_KEY_MIDDLE_NAME, this.middleName)
    }
    if (this.familyName != null) {
      jsonObject.put(JSON_KEY_FAMILY_NAME, this.familyName)
    }
    if (this.email != null) {
      jsonObject.put(JSON_KEY_EMAIL, this.email)
    }
    if (this.picture != null) {
      jsonObject.put(JSON_KEY_PICTURE, this.picture)
    }
    if (this.userFriends != null) {
      jsonObject.put(JSON_KEY_USER_FRIENDS, JSONArray(this.userFriends))
    }
    if (this.userBirthday != null) {
      jsonObject.put(JSON_KEY_USER_BIRTHDAY, this.userBirthday)
    }
    if (this.userAgeRange != null) {
      jsonObject.put(JSON_KEY_USER_AGE_RANGE, JSONObject(this.userAgeRange))
    }
    if (this.userHometown != null) {
      jsonObject.put(JSON_KEY_USER_HOMETOWN, JSONObject(this.userHometown))
    }
    if (this.userLocation != null) {
      jsonObject.put(JSON_KEY_USER_LOCATION, JSONObject(this.userLocation))
    }
    if (this.userGender != null) {
      jsonObject.put(JSON_KEY_USER_GENDER, this.userGender)
    }
    if (this.userLink != null) {
      jsonObject.put(JSON_KEY_USER_LINK, this.userLink)
    }

    return jsonObject
  }

  companion object {
    const val MAX_TIME_SINCE_TOKEN_ISSUED = DateUtils.MINUTE_IN_MILLIS * 10 // 10 minutes
    const val JSON_KEY_JIT = "jti"
    const val JSON_KEY_ISS = "iss"
    const val JSON_KEY_AUD = "aud"
    const val JSON_KEY_NONCE = "nonce"
    const val JSON_KEY_EXP = "exp"
    const val JSON_KEY_IAT = "iat"
    const val JSON_KEY_SUB = "sub"
    const val JSON_KEY_NAME = "name"
    const val JSON_KEY_GIVEN_NAME = "given_name"
    const val JSON_KEY_MIDDLE_NAME = "middle_name"
    const val JSON_KEY_FAMILY_NAME = "family_name"
    const val JSON_KEY_EMAIL = "email"
    const val JSON_KEY_PICTURE = "picture"
    const val JSON_KEY_USER_FRIENDS = "user_friends"
    const val JSON_KEY_USER_BIRTHDAY = "user_birthday"
    const val JSON_KEY_USER_AGE_RANGE = "user_age_range"
    const val JSON_KEY_USER_HOMETOWN = "user_hometown"
    const val JSON_KEY_USER_GENDER = "user_gender"
    const val JSON_KEY_USER_LINK = "user_link"
    const val JSON_KEY_USER_LOCATION = "user_location"

    internal fun JSONObject.getNullableString(name: String): String? {
      if (has(name)) {
        return getString(name)
      }
      return null
    }

    internal fun createFromJSONObject(jsonObject: JSONObject): AuthenticationTokenClaims {
      val jti = jsonObject.getString(JSON_KEY_JIT)
      val iss = jsonObject.getString(JSON_KEY_ISS)
      val aud = jsonObject.getString(JSON_KEY_AUD)
      val nonce = jsonObject.getString(JSON_KEY_NONCE)
      val exp = jsonObject.getLong(JSON_KEY_EXP)
      val iat = jsonObject.getLong(JSON_KEY_IAT)
      val sub = jsonObject.getString(JSON_KEY_SUB)
      val name = jsonObject.getNullableString(JSON_KEY_NAME)
      val givenName = jsonObject.getNullableString(JSON_KEY_GIVEN_NAME)
      val middleName = jsonObject.getNullableString(JSON_KEY_MIDDLE_NAME)
      val familyName = jsonObject.getNullableString(JSON_KEY_FAMILY_NAME)
      val email = jsonObject.getNullableString(JSON_KEY_EMAIL)
      val picture = jsonObject.getNullableString(JSON_KEY_PICTURE)
      val userFriends = jsonObject.optJSONArray(JSON_KEY_USER_FRIENDS)
      val userBirthday = jsonObject.getNullableString(JSON_KEY_USER_BIRTHDAY)
      val userAgeRange = jsonObject.optJSONObject(JSON_KEY_USER_AGE_RANGE)
      val userHometown = jsonObject.optJSONObject(JSON_KEY_USER_HOMETOWN)
      val userLocation = jsonObject.optJSONObject(JSON_KEY_USER_LOCATION)
      val userGender = jsonObject.getNullableString(JSON_KEY_USER_GENDER)
      val userLink = jsonObject.getNullableString(JSON_KEY_USER_LINK)
      return AuthenticationTokenClaims(
          jti,
          iss,
          aud,
          nonce,
          exp,
          iat,
          sub,
          name,
          givenName,
          middleName,
          familyName,
          email,
          picture,
          if (userFriends == null) null else jsonArrayToStringList(userFriends),
          userBirthday,
          if (userAgeRange == null) null
          else convertJSONObjectToHashMap(userAgeRange) as Map<String, Int>?,
          if (userHometown == null) null else convertJSONObjectToStringMap(userHometown),
          if (userLocation == null) null else convertJSONObjectToStringMap(userLocation),
          userGender,
          userLink)
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
