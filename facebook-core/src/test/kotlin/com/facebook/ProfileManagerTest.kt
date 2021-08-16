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

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ProfileCache::class, FacebookSdk::class)
class ProfileManagerTest : FacebookPowerMockTestCase() {

  private val mockAppID = "123456789"
  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)

    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testLoadCurrentProfileEmptyCache() {
    val profileCache = PowerMockito.mock(ProfileCache::class.java)
    val localBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    val profileManager = ProfileManager(localBroadcastManager, profileCache)
    Assert.assertFalse(profileManager.loadCurrentProfile())
    verify(profileCache, times(1)).load()
  }

  fun testLoadCurrentProfileWithCache() {
    val profileCache = PowerMockito.mock(ProfileCache::class.java)
    val profile = createDefaultProfile()
    whenever(profileCache.load()).thenReturn(profile)
    val localBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    val profileManager = ProfileManager(localBroadcastManager, profileCache)
    Assert.assertTrue(profileManager.loadCurrentProfile())
    verify(profileCache, times(1)).load()

    profileManager.currentProfile = createDefaultProfile()
    verify(localBroadcastManager, times(1)).sendBroadcast(any())

    // Verify that if we unset the profile there is a broadcast
    profileManager.currentProfile = null
    verify(localBroadcastManager, times(2)).sendBroadcast(any())
  }
}
