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

    @Volatile private var instance: ProfileManager? = null
    @JvmStatic
    fun getInstance(): ProfileManager {
      if (instance == null) {
        synchronized(this) {
          if (instance == null) {
            val applicationContext = FacebookSdk.getApplicationContext()
            val localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
            instance = ProfileManager(localBroadcastManager, ProfileCache())
          }
        }
      }
      return checkNotNull(instance)
    }
  }
}
