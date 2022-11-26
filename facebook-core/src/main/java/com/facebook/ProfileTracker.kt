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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.internal.Validate.sdkInitialized

/**
 * This class can be extended to receive notifications of profile changes. The [ ][.stopTracking]
 * method should be called in the onDestroy() method of the receiving Activity or Fragment.
 */
abstract class ProfileTracker {
  private val receiver: BroadcastReceiver
  private val broadcastManager: LocalBroadcastManager

  /**
   * Gets whether the tracker is tracking the current access token.
   *
   * @return true if the tracker is the tracking the current access token, false if not
   */
  var isTracking = false
    private set

  /**
   * The method that will be called when the profile changes.
   *
   * @param oldProfile The profile before the change.
   * @param currentProfile The new profile.
   */
  protected abstract fun onCurrentProfileChanged(oldProfile: Profile?, currentProfile: Profile?)

  /** Starts tracking the current profile. */
  fun startTracking() {
    if (isTracking) {
      return
    }
    addBroadcastReceiver()
    isTracking = true
  }

  /** Stops tracking the current profile. */
  fun stopTracking() {
    if (!isTracking) {
      return
    }
    broadcastManager.unregisterReceiver(receiver)
    isTracking = false
  }

  private inner class ProfileBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (ProfileManager.ACTION_CURRENT_PROFILE_CHANGED == intent.action) {
        val oldProfile: Profile? = intent.getParcelableExtra(ProfileManager.EXTRA_OLD_PROFILE)
        val newProfile: Profile? = intent.getParcelableExtra(ProfileManager.EXTRA_NEW_PROFILE)
        onCurrentProfileChanged(oldProfile, newProfile)
      }
    }
  }

  private fun addBroadcastReceiver() {
    val filter = IntentFilter()
    filter.addAction(ProfileManager.ACTION_CURRENT_PROFILE_CHANGED)
    broadcastManager.registerReceiver(receiver, filter)
  }

  /** Constructor. */
  init {
    sdkInitialized()
    receiver = ProfileBroadcastReceiver()
    broadcastManager = LocalBroadcastManager.getInstance(FacebookSdk.getApplicationContext())
    startTracking()
  }
}
