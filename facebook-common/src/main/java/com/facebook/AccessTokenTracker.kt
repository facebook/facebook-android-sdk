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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.internal.Utility.logd
import com.facebook.internal.Validate

/**
 * This class can be extended to receive notifications of access token changes. The {@link
 * #stopTracking()} method should be called in the onDestroy() method of the receiving Activity or
 * Fragment.
 */
abstract class AccessTokenTracker {
  private val receiver: BroadcastReceiver
  private val broadcastManager: LocalBroadcastManager

  /**
   * Gets whether the tracker is tracking the current access token.
   *
   * @return true if the tracker is tracking the current access token, false if not
   */
  var isTracking = false
    private set

  /**
   * The method that will be called with the access token changes.
   *
   * @param oldAccessToken The access token before the change.
   * @param currentAccessToken The new access token.
   */
  protected abstract fun onCurrentAccessTokenChanged(
      oldAccessToken: AccessToken?,
      currentAccessToken: AccessToken?
  )

  /** The constructor. */
  init {
    Validate.sdkInitialized()
    receiver = CurrentAccessTokenBroadcastReceiver()
    broadcastManager = LocalBroadcastManager.getInstance(FacebookSdk.getApplicationContext())
    startTracking()
  }

  /** Starts tracking the current access token */
  fun startTracking() {
    if (isTracking) {
      return
    }
    addBroadcastReceiver()
    isTracking = true
  }

  /** Stops tracking the current access token. */
  fun stopTracking() {
    if (!isTracking) {
      return
    }
    broadcastManager.unregisterReceiver(receiver)
    isTracking = false
  }

  private inner class CurrentAccessTokenBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED == intent.action) {
        logd(TAG, "AccessTokenChanged")
        val oldAccessToken: AccessToken? =
            intent.getParcelableExtra(AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN)
        val newAccessToken: AccessToken? =
            intent.getParcelableExtra(AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN)
        onCurrentAccessTokenChanged(oldAccessToken, newAccessToken)
      }
    }
  }

  private fun addBroadcastReceiver() {
    val filter = IntentFilter()
    filter.addAction(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED)
    broadcastManager.registerReceiver(receiver, filter)
  }

  companion object {
    private val TAG = AccessTokenTracker::class.java.simpleName
  }
}
