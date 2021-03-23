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

package com.facebook;

import static com.facebook.util.common.TestHelpersKt.mockLocalBroadcastManager;
import static org.junit.Assert.*;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.annotation.LooperMode;

@LooperMode(LEGACY)
@PrepareForTest({LocalBroadcastManager.class, FacebookSdk.class})
public class ProfileTrackerTest extends FacebookPowerMockTestCase {

  @Test
  public void testStartStopTrackingAndBroadcast() {
    LocalBroadcastManager localBroadcastManager = mockLocalBroadcastManager();
    PowerMockito.mockStatic(LocalBroadcastManager.class);
    PowerMockito.when(LocalBroadcastManager.getInstance(Matchers.isA(Context.class)))
        .thenReturn(localBroadcastManager);

    PowerMockito.mockStatic(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.isInitialized()).thenReturn(true);
    PowerMockito.when(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext());
    TestProfileTracker testProfileTracker = new TestProfileTracker();
    // Starts tracking
    assertTrue(testProfileTracker.isTracking());

    testProfileTracker.stopTracking();
    assertFalse(testProfileTracker.isTracking());
    sendBroadcast(localBroadcastManager, null, ProfileTest.createDefaultProfile());
    assertFalse(testProfileTracker.isCallbackCalled);
    testProfileTracker.startTracking();
    assertTrue(testProfileTracker.isTracking());
    Profile profile = ProfileTest.createDefaultProfile();
    sendBroadcast(localBroadcastManager, null, profile);
    assertNull(testProfileTracker.oldProfile);
    assertEquals(profile, testProfileTracker.currentProfile);
    assertTrue(testProfileTracker.isCallbackCalled);

    Profile profile1 = ProfileTest.createMostlyNullsProfile();
    Profile profile2 = ProfileTest.createDefaultProfile();
    sendBroadcast(localBroadcastManager, profile1, profile2);
    ProfileTest.assertMostlyNullsObjectGetters(testProfileTracker.oldProfile);
    ProfileTest.assertDefaultObjectGetters(testProfileTracker.currentProfile);
    assertEquals(profile1, testProfileTracker.oldProfile);
    assertEquals(profile2, testProfileTracker.currentProfile);

    testProfileTracker.stopTracking();
  }

  private static void sendBroadcast(
      LocalBroadcastManager localBroadcastManager, Profile oldProfile, Profile currentProfile) {
    Intent intent = new Intent(ProfileManager.ACTION_CURRENT_PROFILE_CHANGED);

    intent.putExtra(ProfileManager.EXTRA_OLD_PROFILE, oldProfile);
    intent.putExtra(ProfileManager.EXTRA_NEW_PROFILE, currentProfile);

    localBroadcastManager.sendBroadcast(intent);
  }

  static class TestProfileTracker extends ProfileTracker {
    Profile oldProfile;
    Profile currentProfile;
    boolean isCallbackCalled = false;

    @Override
    protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
      this.oldProfile = oldProfile;
      this.currentProfile = currentProfile;
      isCallbackCalled = true;
    }
  }
}
