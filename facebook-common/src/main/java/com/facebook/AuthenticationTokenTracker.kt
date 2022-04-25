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
import android.content.IntentFilter
import android.os.Parcelable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.internal.Utility
import com.facebook.internal.Validate

/**
 * This class can be extended to receive notifications of authentication token changes. The {@link
 * #stopTracking()} method should be called in the onDestroy() method of the receiving Activity or
 * Fragment.
 */
abstract class AuthenticationTokenTracker {
  private val receiver: BroadcastReceiver
  private val broadcastManager: LocalBroadcastManager

  /** The constructor init */
  init {
    Validate.sdkInitialized()
    receiver = CurrentAuthenticationTokenBroadcastReceiver()
    broadcastManager = LocalBroadcastManager.getInstance(FacebookSdk.getApplicationContext())
    startTracking()
  }

  /**
   * Gets whether the tracker is tracking the current authentication token.
   *
   * @return true if the tracker is tracking the current authentication token, false if not
   */
  var isTracking = false
    private set

  /**
   * The method that will be called with the authentication token changes.
   *
   * @param oldAuthenticationToken The authentication token before the change.
   * @param currentAuthenticationToken The new authentication token.
   */
  protected abstract fun onCurrentAuthenticationTokenChanged(
      oldAuthenticationToken: AuthenticationToken?,
      currentAuthenticationToken: AuthenticationToken?
  )

  /** Starts tracking the current authentication token */
  fun startTracking() {
    if (isTracking) {
      return
    }

    addBroadcastReceiver()
    isTracking = true
  }

  /** Stops tracking the current authentication token. */
  fun stopTracking() {
    if (!isTracking) {
      return
    }
    broadcastManager.unregisterReceiver(receiver)
    isTracking = false
  }

  private inner class CurrentAuthenticationTokenBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (AuthenticationTokenManager.ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED == intent.action) {
        Utility.logd(TAG, "AuthenticationTokenChanged")
        val oldAuthenticationToken =
            intent.getParcelableExtra<Parcelable>(
                AuthenticationTokenManager.EXTRA_OLD_AUTHENTICATION_TOKEN) as AuthenticationToken?
        val newAuthenticationToken =
            intent.getParcelableExtra<Parcelable>(
                AuthenticationTokenManager.EXTRA_NEW_AUTHENTICATION_TOKEN) as AuthenticationToken?
        onCurrentAuthenticationTokenChanged(oldAuthenticationToken, newAuthenticationToken)
      }
    }
  }

  private fun addBroadcastReceiver() {
    val filter = IntentFilter()
    filter.addAction(AuthenticationTokenManager.ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED)
    broadcastManager.registerReceiver(receiver, filter)
  }

  companion object {
    private val TAG = AuthenticationTokenTracker::class.java.simpleName
  }
}
