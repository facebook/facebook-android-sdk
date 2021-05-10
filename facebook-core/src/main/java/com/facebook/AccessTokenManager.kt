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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.AccessToken.Companion.isCurrentAccessTokenActive
import com.facebook.internal.Utility.areObjectsEqual
import com.facebook.internal.Utility.clearFacebookCookies
import com.facebook.internal.Utility.isNullOrEmpty
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class AccessTokenManager
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
constructor(
    private val localBroadcastManager: LocalBroadcastManager,
    private val accessTokenCache: AccessTokenCache
) {
  private var currentAccessTokenField: AccessToken? = null
  var currentAccessToken: AccessToken?
    get() = currentAccessTokenField
    set(value) = setCurrentAccessToken(value, true)
  private val tokenRefreshInProgress = AtomicBoolean(false)
  private var lastAttemptedTokenExtendDate = Date(0)

  fun loadCurrentAccessToken(): Boolean {
    val accessToken = accessTokenCache.load()
    if (accessToken != null) {
      setCurrentAccessToken(accessToken, false)
      return true
    }
    return false
  }

  fun currentAccessTokenChanged() {
    sendCurrentAccessTokenChangedBroadcastIntent(currentAccessToken, currentAccessToken)
  }

  private fun setCurrentAccessToken(currentAccessToken: AccessToken?, saveToCache: Boolean) {
    val oldAccessToken = this.currentAccessTokenField
    this.currentAccessTokenField = currentAccessToken
    tokenRefreshInProgress.set(false)
    lastAttemptedTokenExtendDate = Date(0)
    if (saveToCache) {
      if (currentAccessToken != null) {
        accessTokenCache.save(currentAccessToken)
      } else {
        accessTokenCache.clear()
        clearFacebookCookies(FacebookSdk.getApplicationContext())
      }
    }
    if (!areObjectsEqual(oldAccessToken, currentAccessToken)) {
      sendCurrentAccessTokenChangedBroadcastIntent(oldAccessToken, currentAccessToken)
      setTokenExpirationBroadcastAlarm()
    }
  }

  private fun sendCurrentAccessTokenChangedBroadcastIntent(
      oldAccessToken: AccessToken?,
      currentAccessToken: AccessToken?
  ) {
    val intent =
        Intent(
            FacebookSdk.getApplicationContext(),
            CurrentAccessTokenExpirationBroadcastReceiver::class.java)
    intent.action = ACTION_CURRENT_ACCESS_TOKEN_CHANGED
    intent.putExtra(EXTRA_OLD_ACCESS_TOKEN, oldAccessToken)
    intent.putExtra(EXTRA_NEW_ACCESS_TOKEN, currentAccessToken)
    localBroadcastManager.sendBroadcast(intent)
  }

  private fun setTokenExpirationBroadcastAlarm() {
    val context = FacebookSdk.getApplicationContext()
    val accessToken = AccessToken.getCurrentAccessToken()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
    if (!isCurrentAccessTokenActive() || accessToken?.expires == null || alarmManager == null) {
      return
    }
    val intent = Intent(context, CurrentAccessTokenExpirationBroadcastReceiver::class.java)
    intent.action = ACTION_CURRENT_ACCESS_TOKEN_CHANGED
    val alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
    try {
      alarmManager[AlarmManager.RTC, accessToken.expires.time] = alarmIntent
    } catch (e: Exception) {
      /* no op */
    }
  }

  fun extendAccessTokenIfNeeded() {
    if (!shouldExtendAccessToken()) {
      return
    }
    refreshCurrentAccessToken(null)
  }

  private fun shouldExtendAccessToken(): Boolean {
    val currentAccessToken = this.currentAccessToken ?: return false
    val now = Date().time
    return currentAccessToken.source.canExtendToken() &&
        now - lastAttemptedTokenExtendDate.time > TOKEN_EXTEND_RETRY_SECONDS * 1000 &&
        now - currentAccessToken.lastRefresh.time > TOKEN_EXTEND_THRESHOLD_SECONDS * 1000
  }

  private class RefreshResult {
    var accessToken: String? = null
    var expiresAt = 0
    var dataAccessExpirationTime: Long? = null
    var graphDomain: String? = null
  }

  fun refreshCurrentAccessToken(callback: AccessToken.AccessTokenRefreshCallback?) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      refreshCurrentAccessTokenImpl(callback)
    } else {
      val mainHandler = Handler(Looper.getMainLooper())
      mainHandler.post { refreshCurrentAccessTokenImpl(callback) }
    }
  }

  private fun refreshCurrentAccessTokenImpl(callback: AccessToken.AccessTokenRefreshCallback?) {
    val accessToken = currentAccessToken
    if (accessToken == null) {
      callback?.OnTokenRefreshFailed(FacebookException("No current access token to refresh"))
      return
    }
    if (!tokenRefreshInProgress.compareAndSet(false, true)) {
      callback?.OnTokenRefreshFailed(FacebookException("Refresh already in progress"))
      return
    }
    lastAttemptedTokenExtendDate = Date()
    val permissions: MutableSet<String?> = HashSet()
    val declinedPermissions: MutableSet<String?> = HashSet()
    val expiredPermissions: MutableSet<String?> = HashSet()
    val permissionsCallSucceeded = AtomicBoolean(false)
    val refreshResult = RefreshResult()
    val batch =
        GraphRequestBatch(
            createGrantedPermissionsRequest(
                accessToken,
                GraphRequest.Callback { response ->
                  val result = response.jsonObject ?: return@Callback
                  val permissionsArray = result.optJSONArray("data") ?: return@Callback
                  permissionsCallSucceeded.set(true)
                  for (i in 0 until permissionsArray.length()) {
                    val permissionEntry = permissionsArray.optJSONObject(i) ?: continue
                    val permission = permissionEntry.optString("permission")
                    var status = permissionEntry.optString("status")
                    if (!isNullOrEmpty(permission) && !isNullOrEmpty(status)) {
                      status = status.toLowerCase(Locale.US)
                      when (status) {
                        "granted" -> permissions.add(permission)
                        "declined" -> declinedPermissions.add(permission)
                        "expired" -> expiredPermissions.add(permission)
                        else -> Log.w(TAG, "Unexpected status: $status")
                      }
                    }
                  }
                }),
            createExtendAccessTokenRequest(
                accessToken,
                GraphRequest.Callback { response ->
                  val data = response.jsonObject ?: return@Callback
                  refreshResult.accessToken = data.optString("access_token")
                  refreshResult.expiresAt = data.optInt("expires_at")
                  refreshResult.dataAccessExpirationTime =
                      data.optLong("data_access_expiration_time")
                  refreshResult.graphDomain = data.optString("graph_domain", null)
                }))
    batch.addCallback(
        GraphRequestBatch.Callback {
          var newAccessToken: AccessToken? = null
          val returnAccessToken = refreshResult.accessToken
          val returnExpiresAt = refreshResult.expiresAt
          val returnDataAccessExpirationTime = refreshResult.dataAccessExpirationTime
          val returnGraphDomain = refreshResult.graphDomain

          try {
            if (getInstance().currentAccessToken == null ||
                getInstance().currentAccessToken?.userId !== accessToken.userId) {
              callback?.OnTokenRefreshFailed(
                  FacebookException("No current access token to refresh"))
              return@Callback
            }
            if (!permissionsCallSucceeded.get() &&
                returnAccessToken == null &&
                returnExpiresAt == 0) {
              callback?.OnTokenRefreshFailed(FacebookException("Failed to refresh access token"))
              return@Callback
            }
            newAccessToken =
                AccessToken(
                    returnAccessToken ?: accessToken.token,
                    accessToken.applicationId,
                    accessToken.userId,
                    if (permissionsCallSucceeded.get()) permissions else accessToken.permissions,
                    if (permissionsCallSucceeded.get()) declinedPermissions
                    else accessToken.declinedPermissions,
                    if (permissionsCallSucceeded.get()) expiredPermissions
                    else accessToken.expiredPermissions,
                    accessToken.source,
                    if (refreshResult.expiresAt != 0) Date(refreshResult.expiresAt * 1000L)
                    else accessToken.expires,
                    Date(),
                    if (returnDataAccessExpirationTime != null) {
                      Date(returnDataAccessExpirationTime * 1000L)
                    } else accessToken.dataAccessExpirationTime,
                    returnGraphDomain)
            getInstance().currentAccessToken = newAccessToken
          } finally {
            tokenRefreshInProgress.set(false)
            if (callback != null && newAccessToken != null) {
              callback.OnTokenRefreshed(newAccessToken)
            }
          }
        })
    batch.executeAsync()
  }

  companion object {
    const val TAG = "AccessTokenManager"
    const val ACTION_CURRENT_ACCESS_TOKEN_CHANGED =
        "com.facebook.sdk.ACTION_CURRENT_ACCESS_TOKEN_CHANGED"
    const val EXTRA_OLD_ACCESS_TOKEN = "com.facebook.sdk.EXTRA_OLD_ACCESS_TOKEN"
    const val EXTRA_NEW_ACCESS_TOKEN = "com.facebook.sdk.EXTRA_NEW_ACCESS_TOKEN"
    const val SHARED_PREFERENCES_NAME = "com.facebook.AccessTokenManager.SharedPreferences"

    // Token extension constants
    private const val TOKEN_EXTEND_THRESHOLD_SECONDS = 24 * 60 * 60 // 1 day
    private const val TOKEN_EXTEND_RETRY_SECONDS = 60 * 60 // 1 hour
    private const val TOKEN_EXTEND_GRAPH_PATH = "oauth/access_token"
    private const val ME_PERMISSIONS_GRAPH_PATH = "me/permissions"

    private var instanceField: AccessTokenManager? = null

    @JvmStatic
    fun getInstance(): AccessTokenManager {
      val instance = instanceField
      if (instance == null) {
        synchronized(this) {
          val instance = instanceField
          return if (instance == null) {
            val applicationContext = FacebookSdk.getApplicationContext()
            val localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
            val accessTokenCache = AccessTokenCache()
            val newInstance = AccessTokenManager(localBroadcastManager, accessTokenCache)
            instanceField = newInstance
            newInstance
          } else {
            instance
          }
        }
      } else {
        return instance
      }
    }

    private fun createGrantedPermissionsRequest(
        accessToken: AccessToken,
        callback: GraphRequest.Callback
    ): GraphRequest {
      val parameters = Bundle()
      return GraphRequest(
          accessToken, ME_PERMISSIONS_GRAPH_PATH, parameters, HttpMethod.GET, callback)
    }

    private fun createExtendAccessTokenRequest(
        accessToken: AccessToken,
        callback: GraphRequest.Callback
    ): GraphRequest {
      val parameters = Bundle()
      parameters.putString("grant_type", "fb_extend_sso_token")
      parameters.putString("client_id", accessToken.applicationId)
      return GraphRequest(
          accessToken, TOKEN_EXTEND_GRAPH_PATH, parameters, HttpMethod.GET, callback)
    }
  }
}
