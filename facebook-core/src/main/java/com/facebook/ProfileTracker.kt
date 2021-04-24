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
