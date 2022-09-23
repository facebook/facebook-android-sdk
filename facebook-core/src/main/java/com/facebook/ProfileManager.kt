/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.internal.Utility.areObjectsEqual

class ProfileManager
internal constructor(
    private val localBroadcastManager: LocalBroadcastManager,
    private val profileCache: ProfileCache
) {
  private var currentProfileField: Profile? = null
  var currentProfile: Profile?
    get() = currentProfileField
    set(value) = setCurrentProfile(value, true)

  /**
   * load profile from cache and set it to current profile
   *
   * @return if successfully load profile
   */
  fun loadCurrentProfile(): Boolean {
    val profile = profileCache.load()
    if (profile != null) {
      setCurrentProfile(profile, false)
      return true
    }
    return false
  }

  private fun setCurrentProfile(currentProfile: Profile?, writeToCache: Boolean) {
    val oldProfile = this.currentProfileField
    this.currentProfileField = currentProfile
    if (writeToCache) {
      if (currentProfile != null) {
        profileCache.save(currentProfile)
      } else {
        profileCache.clear()
      }
    }
    if (!areObjectsEqual(oldProfile, currentProfile)) {
      sendCurrentProfileChangedBroadcast(oldProfile, currentProfile)
    }
  }

  private fun sendCurrentProfileChangedBroadcast(oldProfile: Profile?, currentProfile: Profile?) {
    val intent = Intent(ACTION_CURRENT_PROFILE_CHANGED)
    intent.putExtra(EXTRA_OLD_PROFILE, oldProfile)
    intent.putExtra(EXTRA_NEW_PROFILE, currentProfile)
    localBroadcastManager.sendBroadcast(intent)
  }

  companion object {
    const val ACTION_CURRENT_PROFILE_CHANGED = "com.facebook.sdk.ACTION_CURRENT_PROFILE_CHANGED"
    const val EXTRA_OLD_PROFILE = "com.facebook.sdk.EXTRA_OLD_PROFILE"
    const val EXTRA_NEW_PROFILE = "com.facebook.sdk.EXTRA_NEW_PROFILE"

    @Volatile private lateinit var instance: ProfileManager
    @JvmStatic
    @Synchronized
    fun getInstance(): ProfileManager {
      if (!this::instance.isInitialized) {
        val applicationContext = FacebookSdk.getApplicationContext()
        val localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        instance = ProfileManager(localBroadcastManager, ProfileCache())
      }
      return instance
    }
  }
}
