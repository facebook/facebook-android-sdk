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

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.facebook.internal.ImageRequest.Companion.getProfilePictureUri
import com.facebook.internal.Utility
import com.facebook.internal.Utility.getGraphMeRequestWithCacheAsync
import com.facebook.internal.Validate.notNullOrEmpty
import org.json.JSONException
import org.json.JSONObject

/** This class represents a basic Facebook profile. */
class Profile : Parcelable {
  /**
   * Getter for the id of the profile.
   *
   * @return id of the profile.
   */
  val id: String?

  /**
   * Getter for the first name of the profile.
   *
   * @return the first name of the profile.
   */
  val firstName: String?

  /**
   * Getter for the middle name of the profile.
   *
   * @return the middle name of the profile.
   */
  val middleName: String?

  /**
   * Getter for the last name of the profile.
   *
   * @return the last name of the profile.
   */
  val lastName: String?

  /**
   * Getter for the name of the profile.
   *
   * @return the name of the profile.
   */
  val name: String?

  /**
   * Getter for the link of the profile.
   *
   * @return the link of the profile.
   */
  val linkUri: Uri?

  /**
   * Getter for the picture URI of the profile
   *
   * @return the picture URI of the profile.
   */
  val pictureUri: Uri?

  /**
   * Constructor.
   *
   * @param id The id of the profile.
   * @param firstName The first name of the profile. Can be null.
   * @param middleName The middle name of the profile. Can be null.
   * @param lastName The last name of the profile. Can be null.
   * @param name The name of the profile. Can be null.
   * @param linkUri The link for this profile. Can be null.
   */
  @JvmOverloads
  constructor(
      id: String?,
      firstName: String?,
      middleName: String?,
      lastName: String?,
      name: String?,
      linkUri: Uri?,
      pictureUri: Uri? = null,
  ) {
    notNullOrEmpty(id, "id")
    this.id = id
    this.firstName = firstName
    this.middleName = middleName
    this.lastName = lastName
    this.name = name
    this.linkUri = linkUri
    this.pictureUri = pictureUri
  }

  /**
   * Getter for the Uri of the profile picture.
   *
   * @param width The desired width for the profile picture.
   * @param height The desired height for the profile picture.
   * @return The Uri of the profile picture.
   */
  fun getProfilePictureUri(width: Int, height: Int): Uri {
    // Only Instagram users will have non-null profile picture URIs
    if (pictureUri != null) {
      return pictureUri
    }
    val accessToken =
        if (AccessToken.isCurrentAccessTokenActive()) AccessToken.getCurrentAccessToken()?.token
        else ""
    return getProfilePictureUri(id, width, height, accessToken)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is Profile) {
      return false
    }

    return (id == null && other.id == null || id == other.id) &&
        (firstName == null && other.firstName == null || firstName == other.firstName) &&
        (middleName == null && other.middleName == null || middleName == other.middleName) &&
        (lastName == null && other.lastName == null || lastName == other.lastName) &&
        (name == null && other.name == null || name == other.name) &&
        (linkUri == null && other.linkUri == null || linkUri == other.linkUri) &&
        (pictureUri == null && other.pictureUri == null || pictureUri == other.pictureUri)
  }

  override fun hashCode(): Int {
    var result = 17
    result = result * 31 + id.hashCode()
    if (firstName != null) {
      result = result * 31 + firstName.hashCode()
    }
    if (middleName != null) {
      result = result * 31 + middleName.hashCode()
    }
    if (lastName != null) {
      result = result * 31 + lastName.hashCode()
    }
    if (name != null) {
      result = result * 31 + name.hashCode()
    }
    if (linkUri != null) {
      result = result * 31 + linkUri.hashCode()
    }
    if (pictureUri != null) {
      result = result * 31 + pictureUri.hashCode()
    }
    return result
  }

  fun toJSONObject(): JSONObject? {
    val jsonObject = JSONObject()
    try {
      jsonObject.put(ID_KEY, id)
      jsonObject.put(FIRST_NAME_KEY, firstName)
      jsonObject.put(MIDDLE_NAME_KEY, middleName)
      jsonObject.put(LAST_NAME_KEY, lastName)
      jsonObject.put(NAME_KEY, name)
      if (linkUri != null) {
        jsonObject.put(LINK_URI_KEY, linkUri.toString())
      }
      if (pictureUri != null) {
        jsonObject.put(PICTURE_URI_KEY, pictureUri.toString())
      }
    } catch (_: JSONException) {
      return null
    }
    return jsonObject
  }

  internal constructor(jsonObject: JSONObject) {
    id = jsonObject.optString(ID_KEY, null)
    firstName = jsonObject.optString(FIRST_NAME_KEY, null)
    middleName = jsonObject.optString(MIDDLE_NAME_KEY, null)
    lastName = jsonObject.optString(LAST_NAME_KEY, null)
    name = jsonObject.optString(NAME_KEY, null)
    val linkUriString = jsonObject.optString(LINK_URI_KEY, null)
    linkUri = if (linkUriString == null) null else Uri.parse(linkUriString)
    val pictureUriString = jsonObject.optString(PICTURE_URI_KEY, null)
    pictureUri = if (pictureUriString == null) null else Uri.parse(pictureUriString)
  }

  private constructor(source: Parcel) {
    id = source.readString()
    firstName = source.readString()
    middleName = source.readString()
    lastName = source.readString()
    name = source.readString()
    val linkUriString = source.readString()
    linkUri = if (linkUriString == null) null else Uri.parse(linkUriString)
    val pictureUriString = source.readString()
    pictureUri = if (pictureUriString == null) null else Uri.parse(pictureUriString)
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(id)
    dest.writeString(firstName)
    dest.writeString(middleName)
    dest.writeString(lastName)
    dest.writeString(name)
    dest.writeString(linkUri?.toString())
    dest.writeString(pictureUri?.toString())
  }

  companion object {
    private val TAG = Profile::class.java.simpleName
    private const val ID_KEY = "id"
    private const val FIRST_NAME_KEY = "first_name"
    private const val MIDDLE_NAME_KEY = "middle_name"
    private const val LAST_NAME_KEY = "last_name"
    private const val NAME_KEY = "name"
    private const val LINK_URI_KEY = "link_uri"
    // Used exclusively for Instagram profiles
    private const val PICTURE_URI_KEY = "picture_uri"

    /**
     * Getter for the profile that is currently logged in to the application.
     *
     * @return The profile that is currently logged in to the application.
     */
    @JvmStatic
    fun getCurrentProfile(): Profile? {
      return ProfileManager.getInstance().currentProfile
    }
    /**
     * Setter for the profile that is currently logged in to the application. If the access token is
     * invalidated, the current profile will not be updated. It's only updated when there is an
     * explicit logout, login or when permissions change via the [ ].
     *
     * @param profile The profile that is currently logged in to the application.
     */
    @JvmStatic
    fun setCurrentProfile(profile: Profile?) {
      ProfileManager.getInstance().currentProfile = profile
    }

    /**
     * Fetches and sets the current profile from the current access token.
     *
     * This should only be called from the UI thread.
     */
    @JvmStatic
    fun fetchProfileForCurrentAccessToken() {
      val accessToken = AccessToken.getCurrentAccessToken() ?: return
      if (!AccessToken.isCurrentAccessTokenActive()) {
        setCurrentProfile(null)
        return
      }
      getGraphMeRequestWithCacheAsync(
          accessToken.token,
          object : Utility.GraphMeRequestWithCacheCallback {
            override fun onSuccess(userInfo: JSONObject?) {
              val id = userInfo?.optString("id")
              if (id == null) {
                Log.w(TAG, "No user ID returned on Me request")
                return
              }
              val link = userInfo.optString("link")
              val picture = userInfo.optString("profile_picture", null)
              val profile =
                  Profile(
                      id,
                      userInfo.optString("first_name"),
                      userInfo.optString("middle_name"),
                      userInfo.optString("last_name"),
                      userInfo.optString("name"),
                      if (link != null) Uri.parse(link) else null,
                      if (picture != null) Uri.parse(picture) else null)
              setCurrentProfile(profile)
            }

            override fun onFailure(error: FacebookException?) {
              Log.e(TAG, "Got unexpected exception: $error")
              return
            }
          })
    }

    @JvmField
    val CREATOR: Parcelable.Creator<Profile> =
        object : Parcelable.Creator<Profile> {
          override fun createFromParcel(source: Parcel): Profile {
            return Profile(source)
          }

          override fun newArray(size: Int): Array<Profile?> {
            return arrayOfNulls(size)
          }
        }
  }
}
