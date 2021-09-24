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
