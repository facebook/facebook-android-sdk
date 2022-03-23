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

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.util.common.ProfileTestHelper.assertDefaultObjectGetters
import com.facebook.util.common.ProfileTestHelper.assertMostlyNullsObjectGetters
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import com.facebook.util.common.ProfileTestHelper.createMostlyNullsProfile
import com.facebook.util.common.mockLocalBroadcastManager
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import org.mockito.Matchers
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
    Assert.assertFalse(testProfileTracker.isTracking)
    sendBroadcast(localBroadcastManager, null, createDefaultProfile())
    Assert.assertFalse(testProfileTracker.isCallbackCalled)
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
