/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.util.common.ProfileTestHelper.assertDefaultObjectGetters
import com.facebook.util.common.ProfileTestHelper.assertMostlyNullsObjectGetters
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import com.facebook.util.common.ProfileTestHelper.createMostlyNullsProfile
import com.facebook.util.common.mockLocalBroadcastManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import org.mockito.Matchers
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@PrepareForTest(LocalBroadcastManager::class, FacebookSdk::class)
class ProfileTrackerTest : FacebookPowerMockTestCase() {
  @Test
  fun testStartStopTrackingAndBroadcast() {
    val localBroadcastManager =
        mockLocalBroadcastManager(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(LocalBroadcastManager::class.java)
    whenever(LocalBroadcastManager.getInstance(Matchers.isA(Context::class.java)))
        .thenReturn(localBroadcastManager)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    val testProfileTracker = TestProfileTracker()
    // Starts tracking
    assertThat(testProfileTracker.isTracking).isTrue
    testProfileTracker.stopTracking()
    assertThat(testProfileTracker.isTracking).isFalse
    sendBroadcast(localBroadcastManager, null, createDefaultProfile())
    assertThat(testProfileTracker.isCallbackCalled).isFalse
    testProfileTracker.startTracking()
    assertThat(testProfileTracker.isTracking).isTrue
    val profile = createDefaultProfile()
    sendBroadcast(localBroadcastManager, null, profile)
    Assert.assertNull(testProfileTracker.oldProfile)
    Assert.assertEquals(profile, testProfileTracker.currentProfile)
    assertThat(testProfileTracker.isCallbackCalled).isTrue
    val profile1 = createMostlyNullsProfile()
    val profile2 = createDefaultProfile()
    sendBroadcast(localBroadcastManager, profile1, profile2)
    assertMostlyNullsObjectGetters(testProfileTracker.oldProfile)
    assertDefaultObjectGetters(testProfileTracker.currentProfile)
    Assert.assertEquals(profile1, testProfileTracker.oldProfile)
    Assert.assertEquals(profile2, testProfileTracker.currentProfile)
    testProfileTracker.stopTracking()
  }

  class TestProfileTracker : ProfileTracker() {
    var oldProfile: Profile? = null
    var currentProfile: Profile? = null
    var isCallbackCalled = false
    override fun onCurrentProfileChanged(oldProfile: Profile?, currentProfile: Profile?) {
      this.oldProfile = oldProfile
      this.currentProfile = currentProfile
      isCallbackCalled = true
    }
  }

  companion object {
    private fun sendBroadcast(
        localBroadcastManager: LocalBroadcastManager,
        oldProfile: Profile?,
        currentProfile: Profile
    ) {
      val intent = Intent(ProfileManager.ACTION_CURRENT_PROFILE_CHANGED)
      intent.putExtra(ProfileManager.EXTRA_OLD_PROFILE, oldProfile)
      intent.putExtra(ProfileManager.EXTRA_NEW_PROFILE, currentProfile)
      localBroadcastManager.sendBroadcast(intent)
    }
  }
}
