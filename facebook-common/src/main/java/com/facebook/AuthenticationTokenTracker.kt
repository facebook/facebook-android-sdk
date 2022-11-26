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
