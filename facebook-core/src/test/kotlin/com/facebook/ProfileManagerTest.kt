/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
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
    val profileCache = mock<ProfileCache>()
    val localBroadcastManager = PowerMockito.mock(LocalBroadcastManager::class.java)
    val profileManager = ProfileManager(localBroadcastManager, profileCache)
    assertThat(profileManager.loadCurrentProfile()).isFalse
    verify(profileCache, times(1)).load()
  }

  @Test
  fun testLoadCurrentProfileWithCache() {
    val profileCache = mock<ProfileCache>()
    val profile = createDefaultProfile()
    whenever(profileCache.load()).thenReturn(profile)
    val localBroadcastManager = mock<LocalBroadcastManager>()
    val profileManager = ProfileManager(localBroadcastManager, profileCache)
    assertThat(profileManager.loadCurrentProfile()).isTrue
    verify(profileCache, times(1)).load()

    profileManager.currentProfile = createDefaultProfile()
    verify(localBroadcastManager, times(1)).sendBroadcast(any())

    // Verify that if we unset the profile there is a broadcast
    profileManager.currentProfile = null
    verify(localBroadcastManager, times(2)).sendBroadcast(any())
  }

  @Test
  fun `test setting a new profile will write to the cache`() {
    val profileCache = mock<ProfileCache>()
    val profile = createDefaultProfile()
    val localBroadcastManager = mock<LocalBroadcastManager>()
    val profileManager = ProfileManager(localBroadcastManager, profileCache)

    profileManager.currentProfile = profile

    verify(profileCache, times(1)).save(profile)
    verify(localBroadcastManager, times(1)).sendBroadcast(any())
    assertThat(profileManager.currentProfile).isEqualTo(profile)
  }
}
