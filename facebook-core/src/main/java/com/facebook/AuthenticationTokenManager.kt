/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.internal.Utility.areObjectsEqual
import com.facebook.internal.Utility.clearFacebookCookies

class AuthenticationTokenManager
internal constructor(
    private val localBroadcastManager: LocalBroadcastManager,
    private val authenticationTokenCache: AuthenticationTokenCache
) {
  private var currentAuthenticationTokenField: AuthenticationToken? = null
  var currentAuthenticationToken: AuthenticationToken?
    get() = currentAuthenticationTokenField
    set(value) = setCurrentAuthenticationToken(value, true)

  /**
   * Load authentication token from AuthenticationTokenCache and set to currentAuthentication Token
   *
   * @return if load authentication token success
   */
  fun loadCurrentAuthenticationToken(): Boolean {
    val authenticationToken = authenticationTokenCache.load()
    if (authenticationToken != null) {
      setCurrentAuthenticationToken(authenticationToken, false)
      return true
    }
    return false
  }

  /**
   * Build intent from currentAuthenticationToken and broadcast the intent to
   * CurrentAuthenticationTokenExpirationBroadcastReceiver.
   */
  fun currentAuthenticationTokenChanged() {
    sendCurrentAuthenticationTokenChangedBroadcastIntent(
        currentAuthenticationToken, currentAuthenticationToken)
  }

  private fun setCurrentAuthenticationToken(
      currentAuthenticationToken: AuthenticationToken?,
      saveToCache: Boolean
  ) {
    val oldAuthenticationToken = this.currentAuthenticationToken
    this.currentAuthenticationTokenField = currentAuthenticationToken
    if (saveToCache) {
      if (currentAuthenticationToken != null) {
        authenticationTokenCache.save(currentAuthenticationToken)
      } else {
        authenticationTokenCache.clear()
        clearFacebookCookies(FacebookSdk.getApplicationContext())
      }
    }
    if (!areObjectsEqual(oldAuthenticationToken, currentAuthenticationToken)) {
      sendCurrentAuthenticationTokenChangedBroadcastIntent(
          oldAuthenticationToken, currentAuthenticationToken)
    }
  }

  private fun sendCurrentAuthenticationTokenChangedBroadcastIntent(
      oldAuthenticationToken: AuthenticationToken?,
      currentAuthenticationToken: AuthenticationToken?
  ) {
    val intent =
        Intent(
            FacebookSdk.getApplicationContext(),
            CurrentAuthenticationTokenChangedBroadcastReceiver::class.java)
    intent.action = ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED
    intent.putExtra(EXTRA_OLD_AUTHENTICATION_TOKEN, oldAuthenticationToken)
    intent.putExtra(EXTRA_NEW_AUTHENTICATION_TOKEN, currentAuthenticationToken)
    localBroadcastManager.sendBroadcast(intent)
  }

  companion object {
    const val TAG = "AuthenticationTokenManager"
    const val ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED =
        "com.facebook.sdk.ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED"
    const val EXTRA_OLD_AUTHENTICATION_TOKEN = "com.facebook.sdk.EXTRA_OLD_AUTHENTICATION_TOKEN"
    const val EXTRA_NEW_AUTHENTICATION_TOKEN = "com.facebook.sdk.EXTRA_NEW_AUTHENTICATION_TOKEN"
    const val SHARED_PREFERENCES_NAME = "com.facebook.AuthenticationTokenManager.SharedPreferences"

    private var instanceField: AuthenticationTokenManager? = null

    @JvmStatic
    fun getInstance(): AuthenticationTokenManager {
      val instance = instanceField
      if (instance == null) {
        synchronized(this) {
          val instance = instanceField
          return if (instance == null) {
            val applicationContext = FacebookSdk.getApplicationContext()
            val localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
            val authenticationTokenCache = AuthenticationTokenCache()
            val newInstance =
                AuthenticationTokenManager(localBroadcastManager, authenticationTokenCache)
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
  }

  /** Wrapper class for the AuthenticationToken intent */
  class CurrentAuthenticationTokenChangedBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      // Do not do anything since this is just wrapper class for the intent broadcast
    }
  }
}
