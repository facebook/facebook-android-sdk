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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.facebook.internal.Utility
import com.facebook.internal.Utility.awaitGetGraphMeRequestWithCache
import com.facebook.internal.Utility.getBundleLongAsDate
import com.facebook.internal.Utility.getGraphMeRequestWithCacheAsync
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.jsonArrayToStringList
import com.facebook.internal.Validate
import java.lang.Exception
import java.util.Collections
import java.util.Date
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * This class represents an immutable access token for using Facebook APIs. It also includes
 * associated metadata such as expiration date and permissions.
 *
 * For more information on access tokens, see
 * [Access Tokens](https://developers.facebook.com/docs/facebook-login/access-tokens/).
 */
class AccessToken : Parcelable {
  /**
   * Gets the date at which the access token expires.
   *
   * @return the expiration date of the token
   */
  val expires: Date

  /**
   * Gets the list of permissions associated with this access token. Note that the most up-to-date
   * list of permissions is maintained by Facebook, so this list may be outdated if permissions have
   * been added or removed since the time the AccessToken object was created. For more information
   * on permissions, see https://developers.facebook.com/docs/reference/login/#permissions.
   *
   * @return a read-only list of strings representing the permissions granted via this access token
   */
  val permissions: Set<String?>

  /**
   * Gets the list of permissions declined by the user with this access token. It represents the
   * entire set of permissions that have been requested and declined. Note that the most up-to-date
   * list of permissions is maintained by Facebook, so this list may be outdated if permissions have
   * been granted or declined since the last time an AccessToken object was created.
   *
   * @return a read-only list of strings representing the permissions declined by the user
   */
  val declinedPermissions: Set<String?>

  /**
   * Gets the list of permissions that were expired with this access token.
   *
   * @return a read-only list of strings representing the expired permissions
   */
  val expiredPermissions: Set<String?>

  /**
   * Gets the string representing the access token.
   *
   * @return the string representing the access token
   */
  val token: String

  /**
   * Gets the [AccessTokenSource] indicating how this access token was obtained.
   *
   * @return the enum indicating how the access token was obtained
   */
  val source: AccessTokenSource

  /**
   * Gets the date at which the token was last refreshed. Since tokens expire, the Facebook SDK will
   * attempt to renew them periodically.
   *
   * @return the date at which this token was last refreshed
   */
  val lastRefresh: Date

  /**
   * Gets the ID of the Facebook Application associated with this access token.
   *
   * @return the application ID
   */
  val applicationId: String

  /**
   * Returns the user id for this access token.
   *
   * @return The user id for this access token.
   */
  val userId: String

  /**
   * Gets the date at which user data access expires.
   *
   * @return the expiration date of the token
   */
  val dataAccessExpirationTime: Date

  /**
   * Returns the graph domain for this access token.
   *
   * @return The graph domain for this access token.
   */
  val graphDomain: String?

  /**
   * Creates a new AccessToken using the supplied information from a previously-obtained access
   * token (for instance, from an already-cached access token obtained prior to integration with the
   * Facebook SDK). Note that the caller is asserting that all parameters provided are correct with
   * respect to the access token string; no validation is done to verify they are correct.
   *
   * @param accessToken the access token string obtained from Facebook
   * @param applicationId the ID of the Facebook Application associated with this access token
   * @param userId the id of the user
   * @param permissions the permissions that were requested when the token was obtained (or when it
   * was last reauthorized); may be null if permission set is unknown
   * @param declinedPermissions the permissions that were declined when the token was obtained; may
   * be null if permission set is unknown
   * @param expiredPermissions the permissions that were expired when the token was obtained; may be
   * null if permission set is unknown
   * @param accessTokenSource an enum indicating how the token was originally obtained (in most
   * cases, this will be either AccessTokenSource.FACEBOOK_APPLICATION or
   * AccessTokenSource.WEB_VIEW); if null, FACEBOOK_APPLICATION is assumed.
   * @param expirationTime the expiration date associated with the token; if null, an infinite
   * expiration time is assumed (but will become correct when the token is refreshed)
   * @param lastRefreshTime the last time the token was refreshed (or when it was first obtained);
   * if null, the current time is used.
   * @param dataAccessExpirationTime The time when user data access expires
   * @param graphDomain The Graph API domain that this token is valid for.
   */
  @JvmOverloads
  constructor(
      accessToken: String,
      applicationId: String,
      userId: String,
      permissions: Collection<String?>?,
      declinedPermissions: Collection<String?>?,
      expiredPermissions: Collection<String?>?,
      accessTokenSource: AccessTokenSource?,
      expirationTime: Date?,
      lastRefreshTime: Date?,
      dataAccessExpirationTime: Date?,
      graphDomain: String? = null
  ) {
    Validate.notEmpty(accessToken, "accessToken")
    Validate.notEmpty(applicationId, "applicationId")
    Validate.notEmpty(userId, "userId")

    expires = expirationTime ?: DEFAULT_EXPIRATION_TIME
    this.permissions =
        Collections.unmodifiableSet(if (permissions != null) HashSet(permissions) else HashSet())
    this.declinedPermissions =
        Collections.unmodifiableSet(
            if (declinedPermissions != null) HashSet(declinedPermissions) else HashSet())
    this.expiredPermissions =
        Collections.unmodifiableSet(
            if (expiredPermissions != null) HashSet(expiredPermissions) else HashSet())
    token = accessToken
    source = accessTokenSource ?: DEFAULT_ACCESS_TOKEN_SOURCE
    lastRefresh = lastRefreshTime ?: DEFAULT_LAST_REFRESH_TIME
    this.applicationId = applicationId
    this.userId = userId
    this.dataAccessExpirationTime =
        if (dataAccessExpirationTime != null && dataAccessExpirationTime.time != 0L) {
          dataAccessExpirationTime
        } else {
          DEFAULT_EXPIRATION_TIME
        }
    this.graphDomain = graphDomain
  }

  interface AccessTokenRefreshCallback {
    fun OnTokenRefreshed(accessToken: AccessToken?)
    fun OnTokenRefreshFailed(exception: FacebookException?)
  }

  /** A callback for creating an access token from a NativeLinkingIntent */
  interface AccessTokenCreationCallback {
    /**
     * The method called on a successful creation of an AccessToken.
     *
     * @param token the access token created from the native link intent.
     */
    fun onSuccess(token: AccessToken?)
    fun onError(error: FacebookException?)
  }

  override fun toString(): String {
    val builder = StringBuilder()
    builder.append("{AccessToken")
    builder.append(" token:").append(tokenToString())
    appendPermissions(builder)
    builder.append("}")
    return builder.toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is AccessToken) {
      return false
    }
    return expires == other.expires &&
        permissions == other.permissions &&
        declinedPermissions == other.declinedPermissions &&
        expiredPermissions == other.expiredPermissions &&
        token == other.token &&
        source === other.source &&
        lastRefresh == other.lastRefresh &&
        applicationId == other.applicationId &&
        userId == other.userId &&
        dataAccessExpirationTime == other.dataAccessExpirationTime &&
        if (graphDomain == null) other.graphDomain == null else graphDomain == other.graphDomain
  }

  override fun hashCode(): Int {
    var result = 17
    result = result * 31 + expires.hashCode()
    result = result * 31 + permissions.hashCode()
    result = result * 31 + declinedPermissions.hashCode()
    result = result * 31 + expiredPermissions.hashCode()
    result = result * 31 + token.hashCode()
    result = result * 31 + source.hashCode()
    result = result * 31 + lastRefresh.hashCode()
    result = result * 31 + applicationId.hashCode()
    result = result * 31 + userId.hashCode()
    result = result * 31 + dataAccessExpirationTime.hashCode()
    result = result * 31 + (graphDomain?.hashCode() ?: 0)
    return result
  }

  /**
   * Shows if the token is expired.
   *
   * @return true if the token is expired.
   */
  val isExpired: Boolean
    get() = Date().after(expires)

  /**
   * Shows if the user data access is expired.
   *
   * @return true if the data access is expired.
   */
  val isDataAccessExpired: Boolean
    get() = Date().after(dataAccessExpirationTime)

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  @Throws(JSONException::class)
  fun toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put(VERSION_KEY, CURRENT_JSON_FORMAT)
    jsonObject.put(TOKEN_KEY, token)
    jsonObject.put(EXPIRES_AT_KEY, expires.time)
    val permissionsArray = JSONArray(permissions)
    jsonObject.put(PERMISSIONS_KEY, permissionsArray)
    val declinedPermissionsArray = JSONArray(declinedPermissions)
    jsonObject.put(DECLINED_PERMISSIONS_KEY, declinedPermissionsArray)
    val expiredPermissionsArray = JSONArray(expiredPermissions)
    jsonObject.put(EXPIRED_PERMISSIONS_KEY, expiredPermissionsArray)
    jsonObject.put(LAST_REFRESH_KEY, lastRefresh.time)
    jsonObject.put(SOURCE_KEY, source.name)
    jsonObject.put(APPLICATION_ID_KEY, applicationId)
    jsonObject.put(USER_ID_KEY, userId)
    jsonObject.put(DATA_ACCESS_EXPIRATION_TIME, dataAccessExpirationTime.time)
    if (graphDomain != null) {
      jsonObject.put(GRAPH_DOMAIN, graphDomain)
    }
    return jsonObject
  }

  private fun tokenToString(): String {
    return if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.INCLUDE_ACCESS_TOKENS)) {
      token
    } else {
      "ACCESS_TOKEN_REMOVED"
    }
  }

  private fun appendPermissions(builder: StringBuilder) {
    builder.append(" permissions:")
    builder.append("[")
    builder.append(TextUtils.join(", ", permissions))
    builder.append("]")
  }

  internal constructor(parcel: Parcel) {
    expires = Date(parcel.readLong())
    val permissionsList = ArrayList<String?>()
    parcel.readStringList(permissionsList)
    permissions = Collections.unmodifiableSet(HashSet(permissionsList))
    permissionsList.clear()
    parcel.readStringList(permissionsList)
    declinedPermissions = Collections.unmodifiableSet(HashSet(permissionsList))
    permissionsList.clear()
    parcel.readStringList(permissionsList)
    expiredPermissions = Collections.unmodifiableSet(HashSet(permissionsList))
    val token = parcel.readString()
    Validate.notNullOrEmpty(token, "token")
    this.token = checkNotNull(token)
    val sourceString = parcel.readString()
    source =
        if (sourceString != null) AccessTokenSource.valueOf(sourceString)
        else DEFAULT_ACCESS_TOKEN_SOURCE
    lastRefresh = Date(parcel.readLong())
    val applicationId = parcel.readString()
    Validate.notNullOrEmpty(applicationId, "applicationId")
    this.applicationId = checkNotNull(applicationId)
    val userId = parcel.readString()
    Validate.notNullOrEmpty(userId, "userId")
    this.userId = checkNotNull(userId)
    dataAccessExpirationTime = Date(parcel.readLong())
    graphDomain = parcel.readString()
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeLong(expires.time)
    dest.writeStringList(ArrayList(permissions))
    dest.writeStringList(ArrayList(declinedPermissions))
    dest.writeStringList(ArrayList(expiredPermissions))
    dest.writeString(token)
    dest.writeString(source.name)
    dest.writeLong(lastRefresh.time)
    dest.writeString(applicationId)
    dest.writeString(userId)
    dest.writeLong(dataAccessExpirationTime.time)
    dest.writeString(graphDomain)
  }

  companion object {
    const val ACCESS_TOKEN_KEY = "access_token"
    const val EXPIRES_IN_KEY = "expires_in"
    const val USER_ID_KEY = "user_id"
    const val DATA_ACCESS_EXPIRATION_TIME = "data_access_expiration_time"
    private val MAX_DATE = Date(Long.MAX_VALUE)
    private val DEFAULT_EXPIRATION_TIME = MAX_DATE
    private val DEFAULT_LAST_REFRESH_TIME = Date()
    private val DEFAULT_ACCESS_TOKEN_SOURCE = AccessTokenSource.FACEBOOK_APPLICATION_WEB

    // Constants related to JSON serialization.
    private const val CURRENT_JSON_FORMAT = 1
    private const val VERSION_KEY = "version"
    private const val EXPIRES_AT_KEY = "expires_at"
    private const val PERMISSIONS_KEY = "permissions"
    private const val DECLINED_PERMISSIONS_KEY = "declined_permissions"
    private const val EXPIRED_PERMISSIONS_KEY = "expired_permissions"
    private const val TOKEN_KEY = "token"
    private const val SOURCE_KEY = "source"
    private const val LAST_REFRESH_KEY = "last_refresh"
    private const val APPLICATION_ID_KEY = "application_id"
    private const val GRAPH_DOMAIN = "graph_domain"
    /**
     * Getter for the access token that is current for the application.
     *
     * @return The access token that is current for the application.
     */
    @JvmStatic
    fun getCurrentAccessToken(): AccessToken? {
      return AccessTokenManager.getInstance().currentAccessToken
    }
    /**
     * Setter for the access token that is current for the application.
     *
     * @param accessToken The access token to set.
     */
    @JvmStatic
    fun setCurrentAccessToken(accessToken: AccessToken?) {
      AccessTokenManager.getInstance().currentAccessToken = accessToken
    }
    /**
     * Returns whether the current [AccessToken] is active or not.
     *
     * @return true if the current AccessToken exists and has not expired; false, otherwise.
     */
    @JvmStatic
    fun isCurrentAccessTokenActive(): Boolean {
      val accessToken = AccessTokenManager.getInstance().currentAccessToken
      return accessToken != null && !accessToken.isExpired
    }

    @JvmStatic
    fun isDataAccessActive(): Boolean {
      val accessToken = AccessTokenManager.getInstance().currentAccessToken
      return accessToken != null && !accessToken.isDataAccessExpired
    }

    /**
     * Sets the current [AccessToken] with an expiration time of now. No action is taken if there is
     * no current AccessToken.
     */
    @JvmStatic
    fun expireCurrentAccessToken() {
      val accessToken = AccessTokenManager.getInstance().currentAccessToken
      if (accessToken != null) {
        setCurrentAccessToken(createExpired(accessToken))
      }
    }

    /**
     * Updates the current access token with up to date permissions, and extends the expiration
     * date, if extension is possible.
     */
    @JvmStatic
    fun refreshCurrentAccessTokenAsync() {
      AccessTokenManager.getInstance().refreshCurrentAccessToken(null)
    }

    /**
     * Updates the current access token with up to date permissions, and extends the expiration
     * date, if extension is possible.
     *
     * @param callback
     */
    @JvmStatic
    fun refreshCurrentAccessTokenAsync(callback: AccessTokenRefreshCallback?) {
      AccessTokenManager.getInstance().refreshCurrentAccessToken(callback)
    }

    /**
     * Creates a new AccessToken using the information contained in an Intent populated by the
     * Facebook application in order to launch a native link. For more information on native
     * linking, please see https://developers.facebook.com/docs/mobile/android/deep_linking/.
     *
     * @param intent the Intent that was used to start an Activity; must not be null
     * @param applicationId the ID of the Facebook Application associated with this access token
     */
    @JvmStatic
    fun createFromNativeLinkingIntent(
        intent: Intent,
        applicationId: String,
        accessTokenCallback: AccessTokenCreationCallback
    ) {
      if (intent.extras == null) {
        accessTokenCallback.onError(FacebookException("No extras found on intent"))
        return
      }
      val extras = Bundle(intent.extras)
      val accessToken = extras.getString(ACCESS_TOKEN_KEY)
      if (accessToken == null || accessToken.isEmpty()) {
        accessTokenCallback.onError(FacebookException("No access token found on intent"))
        return
      }
      val userId = extras.getString(USER_ID_KEY)
      // Old versions of facebook for android don't provide the UserId. Obtain the id if missing
      if (userId == null || userId.isEmpty()) {
        getGraphMeRequestWithCacheAsync(
            accessToken,
            object : Utility.GraphMeRequestWithCacheCallback {
              override fun onSuccess(userInfo: JSONObject?) {
                try {
                  val userId = checkNotNull(userInfo?.getString("id"))
                  extras.putString(USER_ID_KEY, userId)
                  accessTokenCallback.onSuccess(
                      createFromBundle(
                          null,
                          extras,
                          AccessTokenSource.FACEBOOK_APPLICATION_WEB,
                          Date(),
                          applicationId))
                } catch (ex: Exception) {
                  accessTokenCallback.onError(
                      FacebookException("Unable to generate access token due to missing user id"))
                }
              }

              override fun onFailure(error: FacebookException?) {
                accessTokenCallback.onError(error)
              }
            })
      } else {
        accessTokenCallback.onSuccess(
            createFromBundle(
                null, extras, AccessTokenSource.FACEBOOK_APPLICATION_WEB, Date(), applicationId))
      }
    }

    @SuppressLint("FieldGetter")
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @JvmStatic
    fun createFromRefresh(current: AccessToken, bundle: Bundle): AccessToken? {
      // Only tokens obtained via SSO support refresh. Token refresh returns the expiration date
      // in seconds from the epoch rather than seconds from now.
      if (current.source !== AccessTokenSource.FACEBOOK_APPLICATION_WEB &&
          current.source !== AccessTokenSource.FACEBOOK_APPLICATION_NATIVE &&
          current.source !== AccessTokenSource.FACEBOOK_APPLICATION_SERVICE) {
        throw FacebookException("Invalid token source: " + current.source)
      }
      val expires = getBundleLongAsDate(bundle, EXPIRES_IN_KEY, Date(0))
      val token = bundle.getString(ACCESS_TOKEN_KEY) ?: return null
      val graphDomain = bundle.getString(GRAPH_DOMAIN)
      val dataAccessExpirationTime =
          getBundleLongAsDate(bundle, DATA_ACCESS_EXPIRATION_TIME, Date(0))
      return if (isNullOrEmpty(token)) {
        null
      } else {
        AccessToken(
            token,
            current.applicationId,
            current.userId,
            current.permissions,
            current.declinedPermissions,
            current.expiredPermissions,
            current.source,
            expires,
            Date(),
            dataAccessExpirationTime,
            graphDomain)
      }
    }

    internal fun createExpired(current: AccessToken): AccessToken {
      return AccessToken(
          current.token,
          current.applicationId,
          current.userId,
          current.permissions,
          current.declinedPermissions,
          current.expiredPermissions,
          current.source,
          Date(),
          Date(),
          current.dataAccessExpirationTime)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @JvmStatic
    fun createFromLegacyCache(bundle: Bundle): AccessToken? {
      val permissions = getPermissionsFromBundle(bundle, LegacyTokenHelper.PERMISSIONS_KEY)
      val declinedPermissions =
          getPermissionsFromBundle(bundle, LegacyTokenHelper.DECLINED_PERMISSIONS_KEY)
      val expiredPermissions =
          getPermissionsFromBundle(bundle, LegacyTokenHelper.EXPIRED_PERMISSIONS_KEY)
      var applicationId = LegacyTokenHelper.getApplicationId(bundle)
      if (isNullOrEmpty(applicationId)) {
        applicationId = FacebookSdk.getApplicationId()
      }
      val tokenString = LegacyTokenHelper.getToken(bundle)
      val userInfo = awaitGetGraphMeRequestWithCache(tokenString)
      val userId =
          try {
            userInfo?.getString("id")
          } catch (ex: JSONException) {
            // This code is only used by AccessTokenCache. If we for any reason fail to get the
            // user id just return null.
            return null
          }
      return AccessToken(
          tokenString,
          applicationId,
          userId ?: return null,
          permissions,
          declinedPermissions,
          expiredPermissions,
          LegacyTokenHelper.getSource(bundle),
          LegacyTokenHelper.getDate(bundle, LegacyTokenHelper.EXPIRATION_DATE_KEY),
          LegacyTokenHelper.getDate(bundle, LegacyTokenHelper.LAST_REFRESH_DATE_KEY),
          null)
    }

    @JvmStatic
    internal fun getPermissionsFromBundle(bundle: Bundle, key: String?): List<String?> {
      // Copy the list so we can guarantee immutable
      val originalPermissions: List<String?>? = bundle.getStringArrayList(key)
      val permissions: List<String?>
      permissions =
          if (originalPermissions == null) {
            emptyList<String>()
          } else {
            Collections.unmodifiableList(ArrayList(originalPermissions))
          }
      return permissions
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @Throws(JSONException::class)
    @JvmStatic
    fun createFromJSONObject(jsonObject: JSONObject): AccessToken {
      val version = jsonObject.getInt(VERSION_KEY)
      if (version > CURRENT_JSON_FORMAT) {
        throw FacebookException("Unknown AccessToken serialization format.")
      }
      val token = jsonObject.getString(TOKEN_KEY)
      val expiresAt = Date(jsonObject.getLong(EXPIRES_AT_KEY))
      val permissionsArray = jsonObject.getJSONArray(PERMISSIONS_KEY)
      val declinedPermissionsArray = jsonObject.getJSONArray(DECLINED_PERMISSIONS_KEY)
      val expiredPermissionsArray = jsonObject.optJSONArray(EXPIRED_PERMISSIONS_KEY)
      val lastRefresh = Date(jsonObject.getLong(LAST_REFRESH_KEY))
      val source = AccessTokenSource.valueOf(jsonObject.getString(SOURCE_KEY))
      val applicationId = jsonObject.getString(APPLICATION_ID_KEY)
      val userId = jsonObject.getString(USER_ID_KEY)
      val dataAccessExpirationTime = Date(jsonObject.optLong(DATA_ACCESS_EXPIRATION_TIME, 0))
      val graphDomain = jsonObject.optString(GRAPH_DOMAIN, null)
      return AccessToken(
          token,
          applicationId,
          userId,
          jsonArrayToStringList(permissionsArray),
          jsonArrayToStringList(declinedPermissionsArray),
          if (expiredPermissionsArray == null) ArrayList()
          else jsonArrayToStringList(expiredPermissionsArray),
          source,
          expiresAt,
          lastRefresh,
          dataAccessExpirationTime,
          graphDomain)
    }

    private fun createFromBundle(
        requestedPermissions: List<String?>?,
        bundle: Bundle,
        source: AccessTokenSource,
        expirationBase: Date,
        applicationId: String
    ): AccessToken? {
      val token = bundle.getString(ACCESS_TOKEN_KEY) ?: return null
      val expires = getBundleLongAsDate(bundle, EXPIRES_IN_KEY, expirationBase) ?: return null
      val userId = bundle.getString(USER_ID_KEY) ?: return null
      val dataAccessExpirationTime =
          getBundleLongAsDate(bundle, DATA_ACCESS_EXPIRATION_TIME, Date(0))
      return AccessToken(
          token,
          applicationId,
          userId,
          requestedPermissions,
          null,
          null,
          source,
          expires,
          Date(),
          dataAccessExpirationTime)
    }

    @JvmField
    val CREATOR: Parcelable.Creator<AccessToken> =
        object : Parcelable.Creator<AccessToken> {
          override fun createFromParcel(source: Parcel): AccessToken {
            return AccessToken(source)
          }

          override fun newArray(size: Int): Array<AccessToken?> {
            return arrayOfNulls(size)
          }
        }
  }
}
